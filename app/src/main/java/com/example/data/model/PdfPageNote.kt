package com.example.data.model

import androidx.room.Entity

@Entity(
    tableName = "pdf_page_notes",
    primaryKeys = ["filePath", "pageIndex"]
)
data class PdfPageNote(
    val filePath: String,
    val pageIndex: Int,
    val noteText: String,
    val updatedAt: Long = System.currentTimeMillis()
)
