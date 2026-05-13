package com.artifex.mupdf.viewer.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext

@Composable
fun IonizationOverlay(
    onCircleComplete: (bounds: androidx.compose.ui.geometry.Rect) -> Unit = {}
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var points = remember { mutableStateListOf<Offset>() }
    val context = LocalContext.current

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        points.clear()
                        points.add(offset)
                        currentPath = Path().apply { moveTo(offset.x, offset.y) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newPoint = change.position
                        points.add(newPoint)
                        currentPath?.lineTo(newPoint.x, newPoint.y)
                        
                        // Force recomposition
                        val p = currentPath
                        currentPath = null
                        currentPath = p
                    },
                    onDragEnd = {
                        if (points.size > 10) {
                            // Trigger vibration
                            triggerVibration(context)
                            
                            // Calculate bounds
                            val minX = points.minOf { it.x }
                            val maxX = points.maxOf { it.x }
                            val minY = points.minOf { it.y }
                            val maxY = points.maxOf { it.y }
                            
                            onCircleComplete(androidx.compose.ui.geometry.Rect(minX, minY, maxX, maxY))
                        }
                        currentPath = null
                        points.clear()
                    }
                )
            }
    ) {
        currentPath?.let { path ->
            drawPath(
                path = path,
                color = Color(0xFF64B5F6).copy(alpha = 0.8f),
                style = Stroke(width = 8f)
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun triggerVibration(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(50)
    }
}
