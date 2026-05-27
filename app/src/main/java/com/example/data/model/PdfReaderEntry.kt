package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_reader_entries")
data class PdfReaderEntry(
    @PrimaryKey val filePath: String,
    val displayName: String,
    val folderId: Int? = null,
    val lastReadPage: Int = 0,
    val lastScrollOffset: Float = 0f,
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val totalPages: Int = 0
)
