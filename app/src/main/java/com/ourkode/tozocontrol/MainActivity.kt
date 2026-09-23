package com.ourkode.tozocontrol

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ourkode.tozocontrol.theme.TOZOControlTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val prefs = getSharedPreferences("tozo_prefs", Context.MODE_PRIVATE)

    enableEdgeToEdge()
    setContent {
      var isDark by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
      TOZOControlTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            isDark = isDark,
            onToggleTheme = {
              val next = !isDark
              isDark = next
              prefs.edit().putBoolean("dark_mode", next).apply()
            }
          )
        }
      }
    }
  }
}
