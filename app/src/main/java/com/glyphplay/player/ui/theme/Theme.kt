package com.glyphplay.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = GlyphWhite,
    onPrimary = GlyphBlack,
    secondary = GlyphRed,
    background = GlyphBlack,
    surface = GlyphInk,
    onBackground = GlyphWhite,
    onSurface = GlyphWhite,
    onSurfaceVariant = GlyphMute,
    outline = GlyphLine,
    error = GlyphRed,
    scrim = Color.Black,
)

@Composable
fun GlyphPlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = GlyphTypography,
        content = content,
    )
}
