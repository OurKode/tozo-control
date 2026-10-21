package com.ourkode.tozocontrol.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// ============================================================
// Utilitarian Editorial Color Schemes (Fallback & Default)
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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> StudioDarkColorScheme
        else -> WarmLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
