package com.glyphplay.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glyphplay.player.ui.theme.GlyphDim
import com.glyphplay.player.ui.theme.GlyphWhite

private val GlyphMap = mapOf(
    '0' to listOf(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
    '1' to listOf("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."),
    '2' to listOf(".###.", "#...#", "....#", "..##.", ".#...", "#....", "#####"),
    '3' to listOf(".###.", "#...#", "....#", "..##.", "....#", "#...#", ".###."),
    '4' to listOf("#...#", "#...#", "#...#", "#####", "....#", "....#", "....#"),
    '5' to listOf("#####", "#....", "#....", "####.", "....#", "#...#", ".###."),
    '6' to listOf(".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."),
    '7' to listOf("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
    '8' to listOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
    '9' to listOf(".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."),
    ':' to listOf(".....", "..#..", "..#..", ".....", "..#..", "..#..", "....."),
    ' ' to listOf(".....", ".....", ".....", ".....", ".....", ".....", "....."),
)

@Composable
fun GlyphText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GlyphWhite,
    dim: Color = GlyphDim,
    cell: Dp = 3.dp,
    gap: Dp = 1.dp,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(gap * 2)) {
        text.forEach { ch ->
            GlyphChar(
                pattern = GlyphMap[ch] ?: GlyphMap['0']!!,
                color = color,
                dim = dim,
                cell = cell,
                gap = gap,
            )
        }
    }
}

@Composable
private fun GlyphChar(
    pattern: List<String>,
    color: Color,
    dim: Color,
    cell: Dp,
    gap: Dp,
) {
    val cols = pattern.first().length
    val rows = pattern.size
    Canvas(
        modifier = Modifier.size(
            width = cell * cols + gap * (cols - 1),
            height = cell * rows + gap * (rows - 1),
        ),
    ) {
        val c = cell.toPx()
        val g = gap.toPx()
        val step = c + g
        pattern.forEachIndexed { y, line ->
            line.forEachIndexed { x, mark ->
                drawCircle(
                    color = if (mark == '#') color else dim,
                    radius = c / 2f,
                    center = Offset(x * step + c / 2f, y * step + c / 2f),
                )
            }
        }
    }
}

fun formatGlyphTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s).padStart(5, ' ')
}
