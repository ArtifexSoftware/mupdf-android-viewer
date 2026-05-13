package com.artifex.mupdf.viewer.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class Atom(
    val id: String,
    val content: String,
    val initialPosition: Offset
)

@Composable
fun BondingView(
    atoms: List<Atom>,
    onBondCreated: (Atom, Atom) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D47A1))) {
        atoms.forEach { atom ->
            AtomicSphere(atom = atom, otherAtoms = atoms.filter { it != atom }, onBondCreated = onBondCreated)
        }
    }
}

@Composable
fun AtomicSphere(
    atom: Atom,
    otherAtoms: List<Atom>,
    onBondCreated: (Atom, Atom) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offset = remember { Animatable(atom.initialPosition, Offset.VectorConverter) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
            .size(80.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF64B5F6), Color(0xFF1976D2))
                )
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offset.snapTo(offset.value + dragAmount)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        // Check for collisions with other atoms
                        val collided = otherAtoms.find { other ->
                            val distance = (offset.value - other.initialPosition).getDistance()
                            distance < 100f // Simple collision threshold
                        }
                        if (collided != null) {
                            onBondCreated(atom, collided)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = atom.id.take(2).uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp
        )
    }
}

private fun Offset.getDistance(): Float = kotlin.math.sqrt(x * x + y * y)
