package com.glyphplay.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.glyphplay.player.ui.theme.GlyphDim
import com.glyphplay.player.ui.theme.GlyphWhite
import kotlin.math.sin

@Composable
fun GlyphProgress(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = GlyphWhite,
    dim: Color = GlyphDim,
    dots: Int = 28,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            },
    ) {
        val r = 3.5.dp.toPx()
        val usable = size.width - r * 2
        val filled = (progress.coerceIn(0f, 1f) * dots).toInt()
        repeat(dots) { i ->
            val x = r + usable * i / (dots - 1).coerceAtLeast(1)
            drawCircle(
                color = if (i <= filled) color else dim,
                radius = r,
                center = Offset(x, size.height / 2f),
            )
        }
    }
}

@Composable
fun GlyphEq(
    active: Boolean,
    modifier: Modifier = Modifier,
    bars: Int = 12,
) {
    val t by rememberInfiniteTransition(label = "eq").animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    Canvas(modifier = modifier.height(22.dp).fillMaxWidth()) {
        val cols = bars
        val rows = 5
        val gap = 3.dp.toPx()
        val cell = ((size.width - gap * (cols - 1)) / cols).coerceAtMost(4.dp.toPx())
        val stepX = cell + gap
        val stepY = cell + 2.dp.toPx()
        repeat(cols) { x ->
            val wave = if (active) {
                ((sin(t + x * 0.55) + 1.0) / 2.0 * rows).toInt().coerceIn(1, rows)
            } else {
                1
            }
            repeat(rows) { y ->
                val lit = y >= (rows - wave)
                drawCircle(
                    color = if (lit) GlyphWhite.copy(alpha = if (active) 1f else 0.25f) else GlyphDim,
                    radius = cell / 2f,
                    center = Offset(x * stepX + cell / 2f, y * stepY + cell / 2f),
                )
            }
        }
    }
}
