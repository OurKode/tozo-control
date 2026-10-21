package com.ourkode.tozocontrol.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ourkode.tozocontrol.data.NoiseMode
import com.ourkode.tozocontrol.data.TozoBleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TozoNoiseTileService : TileService() {
    private var scope: CoroutineScope? = null
    private lateinit var bleManager: TozoBleManager

    override fun onCreate() {
        super.onCreate()
        bleManager = TozoBleManager.getInstance(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        scope?.cancel()
        val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope = coroutineScope

        coroutineScope.launch {
            bleManager.isConnected.collect {
                updateTileState()
            }
        }

        coroutineScope.launch {
            bleManager.currentNoiseMode.collect {
                updateTileState()
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        scope?.cancel()
        scope = null
    }

    override fun onClick() {
        super.onClick()
        if (!bleManager.isConnected.value) {
            bleManager.connect()
            updateTileState()
            return
        }

        val current = bleManager.currentNoiseMode.value
        val next = when (current) {
            NoiseMode.ANC -> NoiseMode.TRANSPARENCY
            NoiseMode.TRANSPARENCY -> NoiseMode.NORMAL
            NoiseMode.NORMAL -> NoiseMode.ANC
            else -> NoiseMode.ANC
        }

        bleManager.setNoiseMode(next)
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isConnected = bleManager.isConnected.value
        val currentMode = bleManager.currentNoiseMode.value

        tile.label = "Noise Control"

        if (!isConnected) {
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Disconnected"
            }
        } else {
            when (currentMode) {
                NoiseMode.NORMAL -> {
                    tile.state = Tile.STATE_INACTIVE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "Normal"
                    }
                }
                NoiseMode.ANC -> {
                    tile.state = Tile.STATE_ACTIVE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "ANC"
                    }
                }
                NoiseMode.TRANSPARENCY -> {
                    tile.state = Tile.STATE_ACTIVE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "Transparency"
                    }
                }
                NoiseMode.REDUCE_WIND -> {
                    tile.state = Tile.STATE_ACTIVE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "Wind"
                    }
                }
                NoiseMode.LEISURE -> {
                    tile.state = Tile.STATE_ACTIVE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "Leisure"
                    }
                }
            }
        }
        tile.updateTile()
    }
}
