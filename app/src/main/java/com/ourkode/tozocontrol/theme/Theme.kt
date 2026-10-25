package com.ourkode.tozocontrol.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import com.ourkode.tozocontrol.data.AppThemeMode
import com.ourkode.tozocontrol.data.ColorPalette

// ============================================================
// CompositionLocal and Helper for Global Haptic Feedback Control
// ============================================================
val LocalHapticEnabled = compositionLocalOf { true }

fun HapticFeedback.performIf(
    enabled: Boolean,
    type: HapticFeedbackType = HapticFeedbackType.TextHandleMove
) {
    if (enabled) {
        performHapticFeedback(type)
    }
}

// ============================================================
// 1. Utilitarian Editorial (Default Curated Scheme)
// ============================================================

val WarmLightColorScheme = lightColorScheme(
    primary = WarmTextPrimary,
    onPrimary = WarmSurface,
    secondary = WarmTextSecondary,
    onSecondary = WarmSurface,
    tertiary = PastelBlueText,
    background = WarmCanvas,
    surface = WarmSurface,
    surfaceVariant = WarmSurfaceSubtle,
    outline = WarmBorder,
    onBackground = WarmTextPrimary,
    onSurface = WarmTextPrimary,
    onSurfaceVariant = WarmTextSecondary
)

val StudioDarkColorScheme = darkColorScheme(
    primary = DarkTextPrimary,
    onPrimary = DarkSurface,
    secondary = DarkTextSecondary,
    onSecondary = DarkSurface,
    tertiary = PastelBlueTextDark,
    background = DarkCanvas,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceSubtle,
    outline = DarkBorder,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

// ============================================================
// 2. Oceanic Cobalt Palette
// ============================================================

val OceanicLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFF0369A1),
    onSecondary = Color.White,
    tertiary = Color(0xFF0EA5E9),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F9FF),
    outline = Color(0xFFBAE6FD),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

val OceanicDarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF082F49),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF082F49),
    tertiary = Color(0xFF0284C7),
    background = Color(0xFF060D17),
    surface = Color(0xFF0C1929),
    surfaceVariant = Color(0xFF132338),
    outline = Color(0xFF1E3A5F),
    onBackground = Color(0xFFF0F9FF),
    onSurface = Color(0xFFF0F9FF),
    onSurfaceVariant = Color(0xFF94A3B8)
)

// ============================================================
// 3. Forest Emerald Palette
// ============================================================

val ForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    secondary = Color(0xFF047857),
    onSecondary = Color.White,
    tertiary = Color(0xFF10B981),
    background = Color(0xFFF9FCF9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2FDF5),
    outline = Color(0xFFA7F3D0),
    onBackground = Color(0xFF062413),
    onSurface = Color(0xFF062413),
    onSurfaceVariant = Color(0xFF3F624D)
)

val ForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C17),
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF022C17),
    tertiary = Color(0xFF059669),
    background = Color(0xFF05120B),
    surface = Color(0xFF0B1F14),
    surfaceVariant = Color(0xFF122E1F),
    outline = Color(0xFF1B4D31),
    onBackground = Color(0xFFECFDF5),
    onSurface = Color(0xFFECFDF5),
    onSurfaceVariant = Color(0xFF86A893)
)

// ============================================================
// 4. Sunset Amber Palette
// ============================================================

val SunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFFEA580C),
    onPrimary = Color.White,
    secondary = Color(0xFFC2410C),
    onSecondary = Color.White,
    tertiary = Color(0xFFF97316),
    background = Color(0xFFFAF8F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFF7ED),
    outline = Color(0xFFFED7AA),
    onBackground = Color(0xFF291405),
    onSurface = Color(0xFF291405),
    onSurfaceVariant = Color(0xFF6E5647)
)

val SunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFB923C),
    onPrimary = Color(0xFF431407),
    secondary = Color(0xFFFDBA74),
    onSecondary = Color(0xFF431407),
    tertiary = Color(0xFFEA580C),
    background = Color(0xFF140A05),
    surface = Color(0xFF24140B),
    surfaceVariant = Color(0xFF331D10),
    outline = Color(0xFF592D14),
    onBackground = Color(0xFFFFF7ED),
    onSurface = Color(0xFFFFF7ED),
    onSurfaceVariant = Color(0xFFA88B79)
)

// ============================================================
// 5. Cyber Amethyst Palette
// ============================================================

val AmethystLightColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    secondary = Color(0xFF6D28D9),
    onSecondary = Color.White,
    tertiary = Color(0xFF8B5CF6),
    background = Color(0xFFFAF8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFAF5FF),
    outline = Color(0xFFDDD6FE),
    onBackground = Color(0xFF1E1035),
    onSurface = Color(0xFF1E1035),
    onSurfaceVariant = Color(0xFF5B4D73)
)

val AmethystDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF2E1065),
    secondary = Color(0xFFC4B5FD),
    onSecondary = Color(0xFF2E1065),
    tertiary = Color(0xFF7C3AED),
    background = Color(0xFF10091E),
    surface = Color(0xFF1E1333),
    surfaceVariant = Color(0xFF2B1B47),
    outline = Color(0xFF4C2F7C),
    onBackground = Color(0xFFFAF5FF),
    onSurface = Color(0xFFFAF5FF),
    onSurfaceVariant = Color(0xFF9E8DB8)
)

// Helper extension to adapt any Dark ColorScheme into Pure Black AMOLED
fun ColorScheme.toAmoled(): ColorScheme = this.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0C),
    surfaceVariant = Color(0xFF141417),
    outline = Color(0xFF242429)
)

@Composable
fun TOZOControlTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    palette: ColorPalette = ColorPalette.EDITORIAL,
    dynamicColor: Boolean = true,
    hapticEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }
    val isAmoled = themeMode == AppThemeMode.AMOLED

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            when (palette) {
                ColorPalette.EDITORIAL -> if (isDark) StudioDarkColorScheme else WarmLightColorScheme
                ColorPalette.OCEANIC -> if (isDark) OceanicDarkColorScheme else OceanicLightColorScheme
                ColorPalette.FOREST -> if (isDark) ForestDarkColorScheme else ForestLightColorScheme
                ColorPalette.SUNSET -> if (isDark) SunsetDarkColorScheme else SunsetLightColorScheme
                ColorPalette.AMETHYST -> if (isDark) AmethystDarkColorScheme else AmethystLightColorScheme
            }
        }
    }

    val finalScheme = if (isAmoled) baseScheme.toAmoled() else baseScheme

    CompositionLocalProvider(LocalHapticEnabled provides hapticEnabled) {
        MaterialTheme(
            colorScheme = finalScheme,
            typography = Typography,
            content = content
        )
    }
}

// Backward-compatible overload for existing calls
@Composable
fun TOZOControlTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    TOZOControlTheme(
        themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
        palette = ColorPalette.EDITORIAL,
        dynamicColor = dynamicColor,
        hapticEnabled = true,
        content = content
    )
}
