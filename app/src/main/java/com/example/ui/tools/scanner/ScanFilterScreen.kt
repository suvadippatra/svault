package com.example.ui.tools.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

enum class ScanFilter(val label: String) { 
    ORIGINAL("Original"), 
    GRAYSCALE("B & W"), 
    ENHANCE("Enhance") 
}

@Composable
fun ScanFilterScreen(
    imageBitmap: Bitmap,
    onFilterConfirmed: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(ScanFilter.ORIGINAL) }
    var currentPreview by remember { mutableStateOf(imageBitmap) }

    LaunchedEffect(selectedFilter) {
        currentPreview = applyFilter(imageBitmap, selectedFilter)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = currentPreview.asImageBitmap(),
                contentDescription = "Preview",
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    items(ScanFilter.entries) { filter ->
                        FilterOption(
                            filter = filter,
                            isSelected = filter == selectedFilter,
                            onClick = { selectedFilter = filter }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Back")
                    }
                    Button(onClick = { onFilterConfirmed(currentPreview) }) {
                        Text("Add to PDF")
                    }
                }
            }
        }
    }
}

@Composable
fun FilterOption(filter: ScanFilter, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.LightGray)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = MaterialTheme.colorScheme.primary
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(filter.label.first().toString(), style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(filter.label, style = MaterialTheme.typography.bodySmall)
    }
}

fun applyFilter(src: Bitmap, filter: ScanFilter): Bitmap = when (filter) {
    ScanFilter.ORIGINAL    -> src
    ScanFilter.GRAYSCALE   -> {
        val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint  = Paint()
        val cm = ColorMatrix().also { it.setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        bmp
    }
    ScanFilter.ENHANCE     -> {
        // Increase contrast: bright -> white, dark -> black
        val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint  = Paint()
        val cm = ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, -60f,
            0f, 1.5f, 0f, 0f, -60f,
            0f, 0f, 1.5f, 0f, -60f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        bmp
    }
}
