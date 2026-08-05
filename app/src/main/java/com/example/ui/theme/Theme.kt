package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BarraDarkColorScheme = darkColorScheme(
    primary = BarraCyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = BarraCyanVariant,
    onPrimaryContainer = Color.White,
    secondary = BarraAmberFolder,
    onSecondary = Color.Black,
    background = BarraDarkBg,
    onBackground = BarraTextPrimary,
    surface = BarraCardBg,
    onSurface = BarraTextPrimary,
    surfaceVariant = BarraCardBorder,
    onSurfaceVariant = BarraTextSecondary
)

private val BarraOledColorScheme = darkColorScheme(
    primary = BarraCyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = BarraCyanVariant,
    onPrimaryContainer = Color.White,
    secondary = BarraAmberFolder,
    onSecondary = Color.Black,
    background = BarraOledBg,
    onBackground = BarraTextPrimary,
    surface = Color(0xFF0F141C),
    onSurface = BarraTextPrimary,
    surfaceVariant = Color(0xFF1E2631),
    onSurfaceVariant = BarraTextSecondary
)

@Composable
fun BarraCloudTheme(
    isOledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isOledMode) BarraOledColorScheme else BarraDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

