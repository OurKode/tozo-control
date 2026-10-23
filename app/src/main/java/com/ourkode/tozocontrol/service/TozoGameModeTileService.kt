package com.ourkode.tozocontrol.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ourkode.tozocontrol.data.TozoBleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TozoGameModeTileService : TileService() {
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

        // If disconnected when notification shade is pulled down, initiate background reconnect
        if (!bleManager.isConnected.value && !bleManager.isConnecting.value) {
            bleManager.connect()
        }

        coroutineScope.launch {
            bleManager.isConnected.collect {
                updateTileState()
            }
        }

        coroutineScope.launch {
            bleManager.isConnecting.collect {
                updateTileState()
            }
        }

        coroutineScope.launch {
            bleManager.isGameMode.collect {
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
        val current = bleManager.isGameMode.value
        bleManager.setGameMode(!current)
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isConnected = bleManager.isConnected.value
        val isConnecting = bleManager.isConnecting.value
        val isGameMode = bleManager.isGameMode.value

        tile.label = "Game Mode"

        // Never set STATE_UNAVAILABLE so Android SystemUI never blocks onClick events
        tile.state = when {
            !isConnected -> Tile.STATE_INACTIVE
            isGameMode -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                isConnected -> if (isGameMode) "45 ms (On)" else "Off"
                isConnecting -> "Connecting..."
                else -> "Offline"
            }
        }

        tile.updateTile()
    }
}
