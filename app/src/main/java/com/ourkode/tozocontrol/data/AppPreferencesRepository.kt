package com.ourkode.tozocontrol.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val label: String) {
    SYSTEM("Ikuti Sistem"),
    LIGHT("Mode Terang"),
    DARK("Mode Gelap"),
    AMOLED("Pure AMOLED")
}

enum class ColorPalette(val label: String, val previewColor: Long) {
    EDITORIAL("Editorial (Hitam Arang)", 0xFF19191C),
    OCEANIC("Oceanic (Biru Laut)", 0xFF0284C7),
    FOREST("Forest (Hijau Pinus)", 0xFF059669),
    SUNSET("Sunset (Jingga Senja)", 0xFFEA580C),
    AMETHYST("Amethyst (Ungu Lembayung)", 0xFF7C3AED)
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    val palette: ColorPalette = ColorPalette.EDITORIAL,
    val hapticEnabled: Boolean = true
)

class AppPreferencesRepository private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeModeStr = prefs.getString(KEY_THEME_MODE, null)
        val themeMode = if (themeModeStr != null) {
            try {
                AppThemeMode.valueOf(themeModeStr)
            } catch (e: IllegalArgumentException) {
                AppThemeMode.SYSTEM
            }
        } else {
            // Backward compatibility for old "dark_mode" boolean
            if (prefs.contains("dark_mode")) {
                if (prefs.getBoolean("dark_mode", false)) AppThemeMode.DARK else AppThemeMode.LIGHT
            } else {
                AppThemeMode.SYSTEM
            }
        }

        val dynamicColorDefault = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, dynamicColorDefault)

        val paletteStr = prefs.getString(KEY_COLOR_PALETTE, ColorPalette.EDITORIAL.name)
        val palette = try {
            ColorPalette.valueOf(paletteStr ?: ColorPalette.EDITORIAL.name)
        } catch (e: IllegalArgumentException) {
            ColorPalette.EDITORIAL
        }

        val hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)

        return AppSettings(
            themeMode = themeMode,
            dynamicColor = dynamicColor,
            palette = palette,
            hapticEnabled = hapticEnabled
        )
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit()
            .putString(KEY_THEME_MODE, mode.name)
            .putBoolean("dark_mode", mode == AppThemeMode.DARK || mode == AppThemeMode.AMOLED)
            .apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _settings.value = _settings.value.copy(dynamicColor = enabled)
    }

    fun setPalette(palette: ColorPalette) {
        prefs.edit().putString(KEY_COLOR_PALETTE, palette.name).apply()
        _settings.value = _settings.value.copy(palette = palette)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(hapticEnabled = enabled)
    }

    companion object {
        private const val PREFS_NAME = "tozo_prefs"
        private const val KEY_THEME_MODE = "m3_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_COLOR_PALETTE = "m3_color_palette"
        private const val KEY_HAPTIC_ENABLED = "m3_haptic_enabled"

        @Volatile
        private var instance: AppPreferencesRepository? = null

        fun getInstance(context: Context): AppPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: AppPreferencesRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
