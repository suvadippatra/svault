package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class PdfViewerViewModel : ViewModel() {

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<Int, Bitmap>> = _thumbnails.asStateFlow()

    private val _fullPages = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val fullPages: StateFlow<Map<Int, Bitmap>> = _fullPages.asStateFlow()

    private var renderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private val mutex = Mutex()
    private var isActive = true

    fun loadPdf(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                closeRenderer()
                try {
                    parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(parcelFileDescriptor!!)
                    _pageCount.value = renderer!!.pageCount
                    _thumbnails.value = emptyMap()
                    _fullPages.value = emptyMap()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /** Render a single thumbnail at half resolution for grid view */
    suspend fun renderThumbnail(index: Int) {
        if (!isActive) return
        mutex.withLock {
            val r = renderer ?: return@withLock
            if (index < 0 || index >= r.pageCount) return@withLock
            if (_thumbnails.value.containsKey(index)) return@withLock 
            
            try {
                val page = r.openPage(index)
                // Lower resolution even more for stability (scale 0.3 instead of 0.5)
                val width = (page.width * 0.35f).toInt().coerceAtLeast(1)
                val height = (page.height * 0.35f).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                // Manage memory: Keep max 60 thumbnails (enough for smooth scrolling but not enough to OOM)
                val currentMap = _thumbnails.value.toMutableMap()
                if (currentMap.size > 60) {
                    currentMap.remove(currentMap.keys.first())
                }
                currentMap[index] = bmp
                _thumbnails.value = currentMap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Render a full-resolution page for the single-page pager view */
    suspend fun renderPage(index: Int) {
        if (!isActive) return
        mutex.withLock {
            val r = renderer ?: return@withLock
            if (index < 0 || index >= r.pageCount) return@withLock
            if (_fullPages.value.containsKey(index)) return@withLock 

            try {
                val page = r.openPage(index)
                val scale = 2
                val bmp = Bitmap.createBitmap(
                    page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888
                )
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                // Keep memory in check: only store a few pages
                val currentMap = _fullPages.value.toMutableMap()
                if (currentMap.size > 5) {
                    val firstKey = currentMap.keys.first()
                    currentMap.remove(firstKey)
                }
                currentMap[index] = bmp
                _fullPages.value = currentMap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun closeRenderer() {
        renderer?.close()
        parcelFileDescriptor?.close()
        renderer = null
        parcelFileDescriptor = null
    }

    override fun onCleared() {
        isActive = false
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock { 
                closeRenderer()
            }
        }
    }
}
