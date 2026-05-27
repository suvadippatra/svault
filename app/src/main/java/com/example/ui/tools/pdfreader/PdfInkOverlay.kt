package com.example.ui.tools.pdfreader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun PdfInkOverlay(
    strokes: List<List<Offset>>,         // existing committed strokes for this page
    onStrokeComplete: (List<Offset>) -> Unit,
    strokeColor: Color = Color(0xFF1565C0),
    strokeWidth: Float = 4f,
    modifier: Modifier = Modifier
) {
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset -> currentStroke = listOf(offset) },
                onDrag = { _, delta ->
                    currentStroke = currentStroke + (currentStroke.last() + delta)
                },
                onDragEnd = {
                    if (currentStroke.isNotEmpty()) {
                        onStrokeComplete(currentStroke)
                    }
                    currentStroke = emptyList()
                }
            )
        }
    ) {
        // Draw committed strokes
        strokes.forEach { stroke ->
            val path = Path()
            stroke.forEachIndexed { i, pt ->
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = path, 
                color = strokeColor, 
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth * density, 
                    pathEffect = PathEffect.cornerPathEffect(10f)
                )
            )
        }
        // Draw current in-progress stroke
        if (currentStroke.isNotEmpty()) {
            val liveP = Path()
            currentStroke.forEachIndexed { i, pt ->
                if (i == 0) liveP.moveTo(pt.x, pt.y) else liveP.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = liveP, 
                color = strokeColor, 
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth * density, 
                    pathEffect = PathEffect.cornerPathEffect(10f)
                )
            )
        }
    }
}
