package com.example.ui.tools.scanner

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun PolygonCropScreen(
    imageBitmap: Bitmap,
    onCropConfirmed: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    // Initial corners: slightly inset from image edges
    val imageSize = Size(imageBitmap.width.toFloat(), imageBitmap.height.toFloat())
    val margin = 0.1f
    
    var viewSize by remember { mutableStateOf(Size.Zero) }
    var corners by remember {
        mutableStateOf(listOf(
            Offset(0f, 0f), Offset(0f, 0f), Offset(0f, 0f), Offset(0f, 0f)
        ))
    }
    
    LaunchedEffect(viewSize) {
        if (viewSize.width > 0 && viewSize.height > 0) {
            corners = listOf(
                Offset(viewSize.width * margin, viewSize.height * margin),
                Offset(viewSize.width * (1 - margin), viewSize.height * margin),
                Offset(viewSize.width * (1 - margin), viewSize.height * (1 - margin)),
                Offset(viewSize.width * margin, viewSize.height * (1 - margin))
            )
        }
    }

    var draggingIndex by remember { mutableStateOf(-1) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = imageBitmap.asImageBitmap(), 
            contentDescription = null,
            modifier = Modifier.fillMaxSize(), 
            contentScale = ContentScale.Fit
        )

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        // Find nearest corner within 80px for better touch target
                        val nearest = corners.mapIndexed { idx, offset -> 
                            val dx = offset.x - pos.x
                            val dy = offset.y - pos.y
                            idx to Math.sqrt((dx*dx + dy*dy).toDouble()).toFloat()
                        }.minByOrNull { it.second }
                        
                        if (nearest != null && nearest.second < 80f) {
                            draggingIndex = nearest.first
                        }
                    },
                    onDrag = { _, delta ->
                        if (draggingIndex >= 0) {
                            val updated = corners.toMutableList()
                            updated[draggingIndex] = (updated[draggingIndex] + delta)
                                .let { Offset(it.x.coerceIn(0f, size.width.toFloat()), it.y.coerceIn(0f, size.height.toFloat())) }
                            corners = updated
                        }
                    },
                    onDragEnd = { draggingIndex = -1 },
                    onDragCancel = { draggingIndex = -1 }
                )
            }
        ) {
            viewSize = size
            // Draw quadrilateral outline
            if (corners.any { it.x != 0f || it.y != 0f }) {
                val path = Path().apply {
                    moveTo(corners[0].x, corners[0].y)
                    corners.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(path, Color(0x884FC3F7), style = Fill)
                drawPath(path, Color(0xFF0288D1), style = Stroke(width = 3f))
                // Draw draggable corner circles
                corners.forEach { pt ->
                    drawCircle(Color.White, radius = 24f, center = pt)
                    drawCircle(Color(0xFF0288D1), radius = 24f, center = pt, style = Stroke(3f))
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onCancel) { Text("Retake") }
            Button(
                onClick = {
                    if (viewSize.width > 0 && viewSize.height > 0) {
                        // Map view coordinates back to image coordinates
                        val scaleX = imageSize.width / viewSize.width
                        val scaleY = imageSize.height / viewSize.height
                        
                        // We are using ContentScale.Fit, which scales while maintaining Aspect ratio
                        val imageAspectRatio = imageSize.width / imageSize.height
                        val viewAspectRatio = viewSize.width / viewSize.height
                        
                        val finalScale: Float
                        val offsetX: Float
                        val offsetY: Float
                        
                        if (imageAspectRatio > viewAspectRatio) {
                            finalScale = viewSize.width / imageSize.width
                            offsetX = 0f
                            offsetY = (viewSize.height - imageSize.height * finalScale) / 2f
                        } else {
                            finalScale = viewSize.height / imageSize.height
                            offsetX = (viewSize.width - imageSize.width * finalScale) / 2f
                            offsetY = 0f
                        }
                        
                        val imageCorners = corners.map { pt ->
                            Offset((pt.x - offsetX) / finalScale, (pt.y - offsetY) / finalScale)
                        }
                        
                        val cropped = perspectiveCrop(imageBitmap, imageCorners)
                        onCropConfirmed(cropped)
                    }
                }
            ) { Text("Confirm Crop") }
        }
    }
}

/** Perspective-correct crop using a simple homography approximation */
fun perspectiveCrop(src: Bitmap, corners: List<Offset>): Bitmap {
    val matrix = Matrix()
    val srcPts = corners.flatMap { listOf(it.x, it.y) }.toFloatArray()
    
    // Find expected width and height using distance formula
    val topWidth = Math.hypot((corners[1].x - corners[0].x).toDouble(), (corners[1].y - corners[0].y).toDouble())
    val bottomWidth = Math.hypot((corners[2].x - corners[3].x).toDouble(), (corners[2].y - corners[3].y).toDouble())
    val dstW = maxOf(topWidth, bottomWidth).toFloat()

    val leftHeight = Math.hypot((corners[3].x - corners[0].x).toDouble(), (corners[3].y - corners[0].y).toDouble())
    val rightHeight = Math.hypot((corners[2].x - corners[1].x).toDouble(), (corners[2].y - corners[1].y).toDouble())
    val dstH = maxOf(leftHeight, rightHeight).toFloat()
    
    val dstPts = floatArrayOf(0f, 0f, dstW, 0f, dstW, dstH, 0f, dstH)
    matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
    
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        .let { Bitmap.createScaledBitmap(it, dstW.toInt(), dstH.toInt(), true) }
}
