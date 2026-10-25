package com.ourkode.tozocontrol.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.ourkode.tozocontrol.Main
import com.ourkode.tozocontrol.Settings
import com.ourkode.tozocontrol.theme.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPreferencesRepositoryTest {

    @Test
    fun appThemeMode_hasAllExpectedEntries() {
        val modes = AppThemeMode.entries.map { it.name }
        assertTrue(modes.contains("SYSTEM"))
        assertTrue(modes.contains("LIGHT"))
        assertTrue(modes.contains("DARK"))
        assertTrue(modes.contains("AMOLED"))
        assertEquals(4, modes.size)
    }

    @Test
    fun colorPalette_hasAllFiveCuratedPalettes() {
        val palettes = ColorPalette.entries.map { it.name }
        assertTrue(palettes.contains("EDITORIAL"))
        assertTrue(palettes.contains("OCEANIC"))
        assertTrue(palettes.contains("FOREST"))
        assertTrue(palettes.contains("SUNSET"))
        assertTrue(palettes.contains("AMETHYST"))
        assertEquals(5, palettes.size)
    }

    @Test
    fun toAmoled_convertsCanvasAndSurfaceToPureZeroNitBlack() {
        val amoled = StudioDarkColorScheme.toAmoled()
        assertEquals(Color(0xFF000000), amoled.background)
        assertEquals(Color(0xFF0A0A0C), amoled.surface)
        assertEquals(Color(0xFF141417), amoled.surfaceVariant)
        assertEquals(Color(0xFF242429), amoled.outline)
    }

    @Test
    fun navigationKeys_containMainAndSettings() {
        assertNotNull(Main)
        assertNotNull(Settings)
    }

    @Test
    fun defaultSettings_hasSensibleDefaults() {
        val settings = AppSettings()
        assertEquals(AppThemeMode.SYSTEM, settings.themeMode)
        assertEquals(ColorPalette.EDITORIAL, settings.palette)
        assertTrue(settings.hapticEnabled)
    }

    @Test
    fun typography_hasCompleteMaterial3Ramp() {
        assertNotNull(Typography.displayLarge)
        assertNotNull(Typography.headlineMedium)
        assertNotNull(Typography.titleLarge)
        assertNotNull(Typography.titleMedium)
        assertNotNull(Typography.titleSmall)
        assertNotNull(Typography.bodyLarge)
        assertNotNull(Typography.bodyMedium)
        assertNotNull(Typography.bodySmall)
        assertNotNull(Typography.labelLarge)
        assertNotNull(Typography.labelMedium)
        assertNotNull(Typography.labelSmall)
    }

    @Test
    fun telemetryTypography_usesMonospaceFont() {
        assertEquals(FontFamily.Monospace, TelemetryValueStyle.fontFamily)
        assertEquals(FontFamily.Monospace, TelemetryLabelStyle.fontFamily)
    }
}
