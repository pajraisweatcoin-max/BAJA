package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpeedTestColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003642),
    onPrimaryContainer = NeonCyan,
    secondary = NeonGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00381C),
    onSecondaryContainer = NeonGreen,
    tertiary = NeonPurple,
    background = CyberDark,
    onBackground = TextPrimary,
    surface = CyberDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCard,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    error = NeonRed
)

@Composable
fun SpeedTestTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpeedTestColorScheme,
        typography = Typography,
        content = content
    )
}

