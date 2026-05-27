package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.data.model.PdfFolder
import com.example.data.model.PdfPageNote
import com.example.data.model.PdfReaderEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfReaderDao {

    @Upsert
    suspend fun upsertEntry(entry: PdfReaderEntry)

    @Query("SELECT * FROM pdf_reader_entries WHERE filePath = :filePath")
    suspend fun getEntry(filePath: String): PdfReaderEntry?

    @Query("SELECT * FROM pdf_reader_entries ORDER BY lastOpenedAt DESC")
    fun getAllEntries(): Flow<List<PdfReaderEntry>>

    @Upsert
    suspend fun upsertNote(note: PdfPageNote)

    @Query("SELECT * FROM pdf_page_notes WHERE filePath = :filePath AND pageIndex = :pageIndex")
    suspend fun getNote(filePath: String, pageIndex: Int): PdfPageNote?

    @Query("SELECT * FROM pdf_page_notes WHERE filePath = :filePath AND pageIndex = :pageIndex")
    fun getNoteFlow(filePath: String, pageIndex: Int): Flow<PdfPageNote?>

    @Query("SELECT * FROM pdf_page_notes WHERE filePath = :filePath")
    fun getAllNotes(filePath: String): Flow<List<PdfPageNote>>

    @Query("SELECT * FROM pdf_reader_folders ORDER BY createdAt ASC")
    fun getAllFolders(): Flow<List<PdfFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: PdfFolder)
}
