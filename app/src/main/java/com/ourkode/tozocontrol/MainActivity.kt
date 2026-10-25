package com.ourkode.tozocontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ourkode.tozocontrol.data.AppPreferencesRepository
import com.ourkode.tozocontrol.theme.TOZOControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefsRepo = AppPreferencesRepository.getInstance(applicationContext)

        enableEdgeToEdge()
        setContent {
            val settings by prefsRepo.settings.collectAsStateWithLifecycle()

            TOZOControlTheme(
                themeMode = settings.themeMode,
                palette = settings.palette,
                dynamicColor = settings.dynamicColor,
                hapticEnabled = settings.hapticEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        appSettings = settings,
                        onThemeModeChange = { prefsRepo.setThemeMode(it) },
                        onDynamicColorChange = { prefsRepo.setDynamicColor(it) },
                        onPaletteChange = { prefsRepo.setPalette(it) },
                        onHapticChange = { prefsRepo.setHapticEnabled(it) }
                    )
                }
            }
        }
    }
}
