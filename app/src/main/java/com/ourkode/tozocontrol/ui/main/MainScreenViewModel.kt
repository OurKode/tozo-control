package com.ourkode.tozocontrol.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ourkode.tozocontrol.data.NoiseMode
import com.ourkode.tozocontrol.data.TozoBleManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    val bleManager = TozoBleManager(application.applicationContext)

    val isConnected = bleManager.isConnected
    val isConnecting = bleManager.isConnecting
    val connectedDeviceName = bleManager.connectedDeviceName
    val connectedDeviceAddress = bleManager.connectedDeviceAddress
    val statusText = bleManager.statusText
    val batteryLeft = bleManager.batteryLeft
    val batteryRight = bleManager.batteryRight
    val firmwareVersion = bleManager.firmwareVersion
    val isGameMode = bleManager.isGameMode
    val currentNoiseMode = bleManager.currentNoiseMode
    val currentEq = bleManager.currentEq

    // Cooldown states to prevent BLE command flooding
    private val _isNoisePending = MutableStateFlow(false)
    val isNoisePending: StateFlow<Boolean> = _isNoisePending.asStateFlow()

    private val _isGameModePending = MutableStateFlow(false)
    val isGameModePending: StateFlow<Boolean> = _isGameModePending.asStateFlow()

    private val _isEqPending = MutableStateFlow(false)
    val isEqPending: StateFlow<Boolean> = _isEqPending.asStateFlow()

    private var noiseJob: Job? = null
    private var gameJob: Job? = null
    private var eqJob: Job? = null

    fun connect(mac: String? = null) {
        bleManager.connect(mac)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun setGameMode(enable: Boolean) {
        if (_isGameModePending.value) return
        gameJob?.cancel()
        _isGameModePending.value = true
        gameJob = viewModelScope.launch {
            bleManager.setGameMode(enable)
            // 450ms cooldown for hardware latency mode stabilization
            delay(450)
            _isGameModePending.value = false
        }
    }

    fun setNoiseMode(mode: NoiseMode) {
        if (_isNoisePending.value) return
        noiseJob?.cancel()
        _isNoisePending.value = true
        noiseJob = viewModelScope.launch {
            bleManager.setNoiseMode(mode)
            // 400ms cooldown between noise cancellation profile switches
            delay(400)
            _isNoisePending.value = false
        }
    }

    fun setEqGains(gains: List<Float>) {
        if (_isEqPending.value) return
        eqJob?.cancel()
        _isEqPending.value = true
        eqJob = viewModelScope.launch {
            bleManager.setEqGains(gains)
            // 400ms cooldown to ensure 24-byte DSP payload persists stably in earbud registers
            delay(400)
            _isEqPending.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.disconnect()
    }
}
