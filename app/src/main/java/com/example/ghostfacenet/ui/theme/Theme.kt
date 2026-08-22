package com.example.ghostfacenet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9C9CFF),
    secondary = Color(0xFF6E6EBF),
    background = Color(0xFF121223),
    surface = Color(0xFF1B1B2F)
)

@Composable
fun GhostFaceNetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
