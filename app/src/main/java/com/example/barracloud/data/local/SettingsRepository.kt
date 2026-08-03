package com.example.barracloud.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val gridColumns: Int = 3,
    val thumbnailQuality: String = "Medium" // Small, Medium, Large
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("barracloud_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeOrdinal = prefs.getInt(KEY_THEME_MODE, AppThemeMode.DARK.ordinal)
        val themeMode = AppThemeMode.entries.getOrElse(themeOrdinal) { AppThemeMode.DARK }
        val columns = prefs.getInt(KEY_GRID_COLUMNS, 3)
        val quality = prefs.getString(KEY_THUMB_QUALITY, "Medium") ?: "Medium"

        return AppSettings(
            themeMode = themeMode,
            gridColumns = columns.coerceIn(2, 5),
            thumbnailQuality = quality
        )
    }

    fun updateThemeMode(mode: AppThemeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun updateGridColumns(columns: Int) {
        val validCols = columns.coerceIn(2, 5)
        prefs.edit().putInt(KEY_GRID_COLUMNS, validCols).apply()
        _settings.value = _settings.value.copy(gridColumns = validCols)
    }

    fun updateThumbnailQuality(quality: String) {
        prefs.edit().putString(KEY_THUMB_QUALITY, quality).apply()
        _settings.value = _settings.value.copy(thumbnailQuality = quality)
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_THUMB_QUALITY = "thumb_quality"
    }
}
