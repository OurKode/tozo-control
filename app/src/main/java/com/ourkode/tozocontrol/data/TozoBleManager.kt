package com.ourkode.tozocontrol.data

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.math.roundToInt

enum class NoiseMode(val label: String, val hexCmd: String) {
    ANC("ANC", "1004010101"),
    TRANSPARENCY("Transparency", "1005010101"),
    LEISURE("Leisure", "1008010101"),
    REDUCE_WIND("Reduce Wind", "1007010101"),
    NORMAL("Normal", "1004010000")
}

@SuppressLint("MissingPermission")
class TozoBleManager(private val context: Context) {

    companion object {
        const val TAG = "TozoBleManager"

        val SERVICE_UUID: UUID = UUID.fromString("0000b610-0000-1000-8000-00805f9b34fb")
        val WRITE_UUID: UUID = UUID.fromString("0000b611-0000-1000-8000-00805f9b34fb")
        val NOTIFY_UUID: UUID = UUID.fromString("0000b612-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        val DEFAULT_QS = byteArrayOf(0x05, 0x06, 0x07, 0x08, 0x0a, 0x0c, 0x0e, 0x10, 0x12, 0x14)
    }

    private val prefs = context.getSharedPreferences("tozo_ble_prefs", Context.MODE_PRIVATE)

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow(
        prefs.getString("last_name", "TOZO AeroSound") ?: "TOZO AeroSound"
    )
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    private val _connectedDeviceAddress = MutableStateFlow(
        prefs.getString("last_mac", "")?.takeIf { !it.endsWith(":AB:C7", ignoreCase = true) } ?: ""
    )
    val connectedDeviceAddress: StateFlow<String> = _connectedDeviceAddress.asStateFlow()

    private val _statusText = MutableStateFlow("Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _batteryLeft = MutableStateFlow(0)
    val batteryLeft: StateFlow<Int> = _batteryLeft.asStateFlow()

    private val _batteryRight = MutableStateFlow(0)
    val batteryRight: StateFlow<Int> = _batteryRight.asStateFlow()

    private val _firmwareVersion = MutableStateFlow("--")
    val firmwareVersion: StateFlow<String> = _firmwareVersion.asStateFlow()

    private val savedNoiseName = prefs.getString("last_noise_mode", NoiseMode.NORMAL.name) ?: NoiseMode.NORMAL.name
    private val initialNoiseMode = try {
        NoiseMode.valueOf(savedNoiseName)
    } catch (_: Exception) {
        NoiseMode.NORMAL
    }
    private val _currentNoiseMode = MutableStateFlow(initialNoiseMode)
    val currentNoiseMode: StateFlow<NoiseMode> = _currentNoiseMode.asStateFlow()

    private val _isGameMode = MutableStateFlow(prefs.getBoolean("last_game_mode", false))
    val isGameMode: StateFlow<Boolean> = _isGameMode.asStateFlow()

    private val savedEqList = prefs.getString("last_eq_gains", null)?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 10 } ?: List(10) { 0.0f }
    private val _currentEq = MutableStateFlow(savedEqList)
    val currentEq: StateFlow<List<Float>> = _currentEq.asStateFlow()

    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val name = device.name ?: result.scanRecord?.deviceName ?: ""
            val address = device.address

            val hasTozoService = result.scanRecord?.serviceUuids?.any {
                it.uuid == SERVICE_UUID
            } == true

            val isTozoName = name.contains("TOZO", ignoreCase = true) ||
                    name.contains("AeroSound", ignoreCase = true) ||
                    name.contains("A3", ignoreCase = true)

            val isTozoMac = address.startsWith("94:4B:F8", ignoreCase = true)

            if (isTozoName || hasTozoService || isTozoMac) {
                Log.d(TAG, "Discovered TOZO earbud via BLE: $name ($address)")
                stopScan()
                _connectedDeviceName.value = if (name.isNotBlank()) name else "TOZO AeroSound 3"
                _connectedDeviceAddress.value = address
                connectToDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed: $errorCode")
            isScanning = false
            _isConnecting.value = false
            _statusText.value = "Scan failed ($errorCode)"
        }
    }

    private fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _statusText.value = "BLE scanner unavailable"
            _isConnecting.value = false
            return
        }

        if (isScanning) return
        isScanning = true
        _statusText.value = "Scanning for TOZO devices..."

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
            scope.launch {
                delay(8000)
                if (isScanning && !_isConnected.value) {
                    stopScan()
                    _isConnecting.value = false
                    _statusText.value = "No TOZO devices found"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "startScan error", e)
            isScanning = false
            _isConnecting.value = false
            _statusText.value = "Scan error: ${e.message}"
        }
    }

    private fun stopScan() {
        if (!isScanning) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "stopScan error", e)
        }
        isScanning = false
    }

    fun connect(explicitMac: String? = null) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _statusText.value = "Bluetooth is disabled"
            _isConnecting.value = false
            return
        }

        disconnect()
        _isConnecting.value = true

        // 1. Explicit MAC address provided
        if (!explicitMac.isNullOrBlank() && BluetoothAdapter.checkBluetoothAddress(explicitMac)) {
            try {
                val device = bluetoothAdapter.getRemoteDevice(explicitMac)
                _connectedDeviceAddress.value = explicitMac
                connectToDevice(device)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Invalid explicit MAC: $explicitMac")
            }
        }

        // 2. Check bonded devices (LE or DUAL only, ignore Classic audio MAC)
        try {
            val bonded = bluetoothAdapter.bondedDevices
            val tozoBonded = bonded?.firstOrNull { d ->
                val n = d.name ?: ""
                val matchesName = n.contains("TOZO", ignoreCase = true) ||
                        n.contains("AeroSound", ignoreCase = true) ||
                        n.contains("A3", ignoreCase = true)
                matchesName && (d.type == BluetoothDevice.DEVICE_TYPE_LE || d.type == BluetoothDevice.DEVICE_TYPE_DUAL)
            }
            if (tozoBonded != null) {
                Log.w(TAG, "Connecting to bonded TOZO LE device: ${tozoBonded.name} (${tozoBonded.address})")
                _connectedDeviceName.value = tozoBonded.name ?: "TOZO AeroSound 3"
                _connectedDeviceAddress.value = tozoBonded.address
                connectToDevice(tozoBonded)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking bonded devices", e)
        }

        // 3. Check last known GATT connected MAC address
        val savedMac = prefs.getString("last_mac", null)
        if (!savedMac.isNullOrBlank() && BluetoothAdapter.checkBluetoothAddress(savedMac)) {
            try {
                val device = bluetoothAdapter.getRemoteDevice(savedMac)
                _connectedDeviceAddress.value = savedMac
                Log.w(TAG, "Reconnecting to last known device: $savedMac")
                connectToDevice(device)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Connection to saved address failed, scanning for devices...", e)
            }
        }

        // 4. Dynamically scan for nearby TOZO devices
        startScan()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        val displayName = device.name ?: _connectedDeviceName.value
        _statusText.value = "Connecting to $displayName..."
        Log.w(TAG, "Connecting to ${device.address}")

        try {
            try {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            } catch (_: Exception) {}
            bluetoothGatt = null

            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        } catch (e: Exception) {
            _isConnecting.value = false
            _statusText.value = "Error: ${e.message}"
            Log.e(TAG, "Connect error", e)
        }
    }

    fun disconnect() {
        stopScan()
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error", e)
        }
        bluetoothGatt = null
        writeChar = null
        _isConnecting.value = false
        _isConnected.value = false
        _batteryLeft.value = 0
        _batteryRight.value = 0
        _firmwareVersion.value = "--"
        _statusText.value = "Disconnected"
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onConnectionStateChange failed status: $status")
                val wasConnecting = _isConnecting.value
                try {
                    bluetoothGatt?.close()
                } catch (_: Exception) {}
                bluetoothGatt = null
                writeChar = null
                _isConnected.value = false
                _batteryLeft.value = 0
                _batteryRight.value = 0
                _firmwareVersion.value = "--"

                // Clear cached MAC if connection fails
                prefs.edit().remove("last_mac").apply()

                if (wasConnecting && !isScanning) {
                    Log.w(TAG, "GATT failed ($status), switching to automatic BLE scan...")
                    _statusText.value = "Scanning for TOZO devices..."
                    scope.launch {
                        delay(250)
                        startScan()
                    }
                } else {
                    _isConnecting.value = false
                    _statusText.value = "Connection terminated ($status)"
                }
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.w(TAG, "GATT Connected! Discovering services...")
                _statusText.value = "Connecting services..."
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "GATT Disconnected")
                _isConnecting.value = false
                _isConnected.value = false
                _batteryLeft.value = 0
                _batteryRight.value = 0
                _firmwareVersion.value = "--"
                _statusText.value = "Disconnected from earbud"
                try {
                    bluetoothGatt?.close()
                } catch (_: Exception) {}
                bluetoothGatt = null
                writeChar = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            _isConnecting.value = false
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    writeChar = service.getCharacteristic(WRITE_UUID)
                    val notifyChar = service.getCharacteristic(NOTIFY_UUID)

                    if (writeChar != null) {
                        _isConnected.value = true
                        _statusText.value = "Connected"
                        val dev = gatt.device
                        if (dev != null) {
                            _connectedDeviceAddress.value = dev.address
                            if (!dev.name.isNullOrBlank()) {
                                _connectedDeviceName.value = dev.name
                            }
                            prefs.edit().putString("last_mac", dev.address).putString("last_name", _connectedDeviceName.value).apply()
                        }
                    } else {
                        _isConnected.value = false
                        _statusText.value = "TX characteristic not found"
                    }

                    if (notifyChar != null) {
                        gatt.setCharacteristicNotification(notifyChar, true)
                        val descriptor = notifyChar.getDescriptor(CCCD_UUID)
                        if (descriptor != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            } else {
                                @Suppress("DEPRECATION")
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                @Suppress("DEPRECATION")
                                gatt.writeDescriptor(descriptor)
                            }
                        }

                        scope.launch {
                            delay(300)
                            // Query hardware state directly from earbud registers
                            sendHex("00010000", "Read Firmware")
                            delay(250)
                            sendHex("00020000", "Read Battery")
                            delay(250)
                            sendHex("00060000", "Read Game Mode")
                            delay(250)
                            sendHex("00040000", "Read Noise Mode")
                            delay(250)
                            sendHex("000b0000", "Read Equalizer Status")
                        }
                    }
                } else {
                    _isConnected.value = false
                    _statusText.value = "TOZO service not found"
                }
            } else {
                _statusText.value = "Failed to discover services: $status"
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic?.uuid == NOTIFY_UUID) {
                val data = characteristic.value ?: return
                parseNotification(data)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == NOTIFY_UUID) {
                parseNotification(value)
            }
        }
    }

    private fun parseNotification(data: ByteArray) {
        val hexStr = data.joinToString("") { "%02x".format(it) }
        Log.w(TAG, "RECV Earbud Notification: $hexStr")

        // 1. Battery: 00 02 02 [L] [R] [CHK] or 20 0e 02 [L] [R] [CHK] (Autonomous event)
        if ((hexStr.startsWith("000202") || hexStr.startsWith("200e02")) && data.size >= 5) {
            _batteryLeft.value = data[3].toInt() and 0xFF
            _batteryRight.value = data[4].toInt() and 0xFF
        }
        // 2. Firmware: 00 01 06 00 00 [L] 00 00 [R] [CHK]
        else if (hexStr.startsWith("000106") && data.size >= 10) {
            val l = "${data[3]}.${data[4]}.${data[5]}"
            _firmwareVersion.value = "v$l"
        }
        // 3. Game Mode: 00 06 01 [status] [chk]
        else if (hexStr.startsWith("000601") && data.size >= 4) {
            _isGameMode.value = (data[3].toInt() == 1)
        }
        // 4. Noise Mode:
        else if (hexStr.startsWith("000401") && data.size >= 4) {
            val modeCode = data[3].toInt()
            when (modeCode) {
                1 -> _currentNoiseMode.value = NoiseMode.ANC
                0 -> _currentNoiseMode.value = NoiseMode.NORMAL
            }
        }
        else if (hexStr.startsWith("000501") && data.size >= 4 && data[3].toInt() == 1) {
            _currentNoiseMode.value = NoiseMode.TRANSPARENCY
        }
        else if (hexStr.startsWith("000701") && data.size >= 4 && data[3].toInt() == 1) {
            _currentNoiseMode.value = NoiseMode.REDUCE_WIND
        }
        else if (hexStr.startsWith("000801") && data.size >= 4 && data[3].toInt() == 1) {
            _currentNoiseMode.value = NoiseMode.LEISURE
        }
        // 5. EQ Status: 00 0b 14 [10 bytes] ...
        else if (hexStr.startsWith("000b14") && data.size >= 23) {
            val list = mutableListOf<Float>()
            for (i in 3 until 13) {
                val b = data[i]
                val valDb = if (b < 128) b.toFloat() / 10.0f else (b - 256).toFloat() / 10.0f
                list.add(valDb)
            }
            _currentEq.value = list
        }
    }

    private val sendMutex = Mutex()
    private var lastSendTime = 0L

    fun sendHex(hexStr: String, desc: String = "") {
        val gatt = bluetoothGatt ?: return
        val char = writeChar ?: return

        scope.launch {
            sendMutex.withLock {
                val now = System.currentTimeMillis()
                val elapsed = now - lastSendTime
                if (elapsed < 180) {
                    delay(180 - elapsed)
                }
                val bytes = hexToByteArray(hexStr)
                Log.w(TAG, "SEND Earbud ($desc): $hexStr")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                } else {
                    @Suppress("DEPRECATION")
                    char.value = bytes
                    @Suppress("DEPRECATION")
                    char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(char)
                }
                lastSendTime = System.currentTimeMillis()
            }
        }
    }

    fun setGameMode(enable: Boolean) {
        if (!_isConnected.value || writeChar == null) return
        val hexCmd = if (enable) "1006010101" else "1006010000"
        _isGameMode.value = enable
        prefs.edit().putBoolean("last_game_mode", enable).apply()
        sendHex(hexCmd, "Game Mode ${if (enable) "ON" else "OFF"}")
        scope.launch {
            delay(500)
            sendHex("00060000", "Verify Game Mode")
        }
    }

    fun setNoiseMode(mode: NoiseMode) {
        if (!_isConnected.value || writeChar == null) return
        _currentNoiseMode.value = mode
        prefs.edit().putString("last_noise_mode", mode.name).apply()
        sendHex(mode.hexCmd, "Mode ${mode.label}")
    }

    fun setEqGains(gainsDb: List<Float>) {
        if (!_isConnected.value || writeChar == null) return
        prefs.edit().putString("last_eq_gains", gainsDb.joinToString(",")).apply()
        val gainBytes = ByteArray(10)
        for (i in 0 until 10) {
            val raw = (gainsDb.getOrElse(i) { 0.0f } * 10).roundToInt()
            gainBytes[i] = (raw and 0xFF).toByte()
        }

        val payload = ByteArray(21)
        System.arraycopy(gainBytes, 0, payload, 0, 10)
        System.arraycopy(DEFAULT_QS, 0, payload, 10, 10)
        payload[20] = 0x01

        var chk = 0
        for (b in payload) {
            chk = (chk + (b.toInt() and 0xFF)) and 0xFF
        }

        val fullPacket = ByteArray(24)
        fullPacket[0] = 0x10
        fullPacket[1] = 0x0B
        fullPacket[2] = 0x15
        System.arraycopy(payload, 0, fullPacket, 3, 21)
        fullPacket[23] = chk.toByte()

        _currentEq.value = gainsDb
        val hexStr = fullPacket.joinToString("") { "%02x".format(it) }
        sendHex(hexStr, "Apply 10-Band EQ")
    }

    private fun hexToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
