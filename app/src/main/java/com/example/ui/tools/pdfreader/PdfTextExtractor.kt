package com.example.ui.tools.pdfreader

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

object PdfTextExtractor {
    fun searchInPage(file: File, pageIndex: Int, query: String): List<Int> {
        return try {
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper().apply {
                    startPage = pageIndex + 1
                    endPage   = pageIndex + 1
                }
                val text = stripper.getText(doc).lowercase()
                val results = mutableListOf<Int>()
                var start = 0
                while (true) {
                    val idx = text.indexOf(query.lowercase(), start)
                    if (idx == -1) break
                    results.add(idx)
                    start = idx + 1
                }
                results
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
