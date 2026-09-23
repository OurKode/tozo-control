package com.ourkode.tozocontrol.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ============================================================
// Utilitarian Editorial Color Schemes
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

@Composable
fun TOZOControlTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else WarmLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
