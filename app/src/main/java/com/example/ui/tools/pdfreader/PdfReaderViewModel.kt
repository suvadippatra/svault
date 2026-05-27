package com.example.ui.tools.pdfreader

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.dao.PdfReaderDao
import com.example.data.model.PdfFolder
import com.example.data.model.PdfPageNote
import com.example.data.model.PdfReaderEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import androidx.room.Room
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PdfReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "scholar_vault_db"
    ).build()

    private val dao = db.pdfReaderDao()

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _twoPageMode = MutableStateFlow(false)
    val twoPageMode: StateFlow<Boolean> = _twoPageMode.asStateFlow()

    fun toggleTwoPageMode() {
        _twoPageMode.value = !_twoPageMode.value
    }

    private val _savedPage = MutableStateFlow(0)
    val savedPage: StateFlow<Int> = _savedPage.asStateFlow()

    private var renderer: PdfRenderer? = null
    private val mutex = Mutex()
    private var isActive = true

    private val _strokesPerPage = MutableStateFlow<Map<Int, List<List<Offset>>>>(emptyMap())
    val strokesPerPage: StateFlow<Map<Int, List<List<Offset>>>> = _strokesPerPage.asStateFlow()

    fun loadPdf(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    renderer?.close()
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(pfd)
                    _pageCount.value = renderer!!.pageCount
                    val entry = dao.getEntry(file.absolutePath)
                    _savedPage.value = entry?.lastReadPage ?: 0
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun renderPage(index: Int): Bitmap? {
        if (!isActive) return null
        var bmp: Bitmap? = null
        try {
            val r = renderer ?: return null
            if (index >= r.pageCount || index < 0) return null
            synchronized(r) {
                val page = r.openPage(index)
                val scale = 2
                bmp = Bitmap.createBitmap(
                    page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888
                )
                bmp!!.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bmp
    }

    fun onPageChanged(filePath: String, pageIndex: Int) {
        _savedPage.value = pageIndex
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = dao.getEntry(filePath)
                dao.upsertEntry(
                    existing?.copy(
                        lastReadPage = pageIndex,
                        lastOpenedAt = System.currentTimeMillis()
                    ) ?: PdfReaderEntry(
                        filePath = filePath, 
                        displayName = File(filePath).name,
                        lastReadPage = pageIndex,
                        totalPages = _pageCount.value
                    )
                )
            } catch (e: Exception) {
               e.printStackTrace()
            }
        }
    }

    fun saveStroke(pageIndex: Int, stroke: List<Offset>) {
        val current = _strokesPerPage.value.toMutableMap()
        val pageStrokes = current[pageIndex]?.toMutableList() ?: mutableListOf()
        pageStrokes.add(stroke)
        current[pageIndex] = pageStrokes
        _strokesPerPage.value = current
    }

    fun getNoteFlow(filePath: String, pageIndex: Int) = dao.getNoteFlow(filePath, pageIndex)

    fun saveNote(filePath: String, pageIndex: Int, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertNote(
                PdfPageNote(
                    filePath = filePath,
                    pageIndex = pageIndex,
                    noteText = text
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        isActive = false
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    renderer?.close()
                } catch (e: Exception) {}
            }
        }
    }

    suspend fun getRenderer(): PdfRenderer? = renderer

    val allFolders = dao.getAllFolders()
    val allEntries = dao.getAllEntries()

    fun createFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertFolder(PdfFolder(name = name))
        }
    }

    suspend fun exportWithAnnotations(
        sourceFile: File,
        outputFile: File
    ): Result<File> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val r = renderer ?: return@withContext Result.failure(Exception("Renderer not initialized"))
            val document = android.graphics.pdf.PdfDocument()
            val strokes = _strokesPerPage.value
            for (i in 0 until r.pageCount) {
                val page = r.openPage(i)
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val canvas = android.graphics.Canvas(bmp)
                val paint  = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLUE
                    strokeWidth = 6f
                    style = android.graphics.Paint.Style.STROKE
                    isAntiAlias = true
                }
                strokes[i]?.forEach { stroke ->
                    for (j in 1 until stroke.size) {
                        canvas.drawLine(stroke[j-1].x, stroke[j-1].y, stroke[j].x, stroke[j].y, paint)
                    }
                }

                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width, bmp.height, i + 1).create()
                val docPage = document.startPage(pageInfo)
                docPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                document.finishPage(docPage)
                bmp.recycle()
            }
            java.io.FileOutputStream(outputFile).use { document.writeTo(it) }
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
