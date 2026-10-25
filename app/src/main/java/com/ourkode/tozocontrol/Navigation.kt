package com.ourkode.tozocontrol

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ourkode.tozocontrol.data.AppSettings
import com.ourkode.tozocontrol.data.AppThemeMode
import com.ourkode.tozocontrol.data.ColorPalette
import com.ourkode.tozocontrol.ui.main.MainScreen
import com.ourkode.tozocontrol.ui.settings.SettingsScreen

@Composable
fun MainNavigation(
    appSettings: AppSettings,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onPaletteChange: (ColorPalette) -> Unit,
    onHapticChange: (Boolean) -> Unit
) {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider =
            entryProvider {
                entry<Main> {
                    MainScreen(
                        onItemClick = { navKey -> backStack.add(navKey) },
                        onNavigateToSettings = { backStack.add(Settings) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                entry<Settings> {
                    SettingsScreen(
                        currentThemeMode = appSettings.themeMode,
                        isDynamicColor = appSettings.dynamicColor,
                        currentPalette = appSettings.palette,
                        isHapticEnabled = appSettings.hapticEnabled,
                        onThemeModeChange = onThemeModeChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onPaletteChange = onPaletteChange,
                        onHapticChange = onHapticChange,
                        onBackClick = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
    )
}

// Backward-compatible overload
@Composable
fun MainNavigation(
    isDark: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    MainNavigation(
        appSettings = AppSettings(
            themeMode = if (isDark) AppThemeMode.DARK else AppThemeMode.LIGHT
        ),
        onThemeModeChange = { onToggleTheme() },
        onDynamicColorChange = {},
        onPaletteChange = {},
        onHapticChange = {}
    )
}
