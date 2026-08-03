package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        secondary = DarkPrimary,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurface,
        onBackground = DarkTextPrimary,
        onSurface = DarkTextPrimary,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkOutline
    )

private val LightColorScheme =
    lightColorScheme(
        primary = CleanPrimary,
        primaryContainer = CleanPrimaryContainer,
        onPrimaryContainer = CleanOnPrimaryContainer,
        secondary = CleanPrimary,
        background = CleanBackground,
        surface = CleanBackground,
        surfaceVariant = CleanSurface,
        onBackground = CleanTextPrimary,
        onSurface = CleanTextPrimary,
        onSurfaceVariant = CleanTextSecondary,
        outline = CleanOutline
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
