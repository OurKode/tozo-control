package com.ourkode.tozocontrol.ui.main

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.ourkode.tozocontrol.Settings
import com.ourkode.tozocontrol.data.NoiseMode
import com.ourkode.tozocontrol.data.TouchAction
import com.ourkode.tozocontrol.data.TouchConfig
import com.ourkode.tozocontrol.theme.*
import kotlin.math.roundToInt

// ============================================================
// Equalizer Constants & Sound Profiles
// ============================================================

val FREQ_LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

val PRESET_MAP = mapOf(
    "Reference Flat" to listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
    "Harman Target" to listOf(3.5f, 3.0f, 1.5f, 0.0f, 0.0f, 1.5f, 3.0f, 1.5f, -0.5f, 1.0f),
    "Diffuse Field" to listOf(-1.5f, -1.0f, 0.0f, 0.0f, 0.0f, 2.0f, 3.5f, 2.5f, 1.0f, 1.5f),
    "Vocal Clarity" to listOf(-2.0f, -1.5f, -0.5f, 0.5f, 1.5f, 3.0f, 3.5f, 2.0f, -1.0f, 0.5f),
    "Warm Punch" to listOf(4.0f, 3.5f, 2.5f, 1.0f, 0.5f, 0.0f, 0.5f, 0.0f, -1.5f, 0.0f),
    // Backward compatibility aliases for stored settings and remote hardware sync
    "Flat" to listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
    "Bass +" to listOf(4.0f, 3.5f, 2.5f, 1.0f, 0.5f, 0.0f, 0.5f, 0.0f, -1.5f, 0.0f),
    "Bass Boost" to listOf(4.0f, 3.5f, 2.5f, 1.0f, 0.5f, 0.0f, 0.5f, 0.0f, -1.5f, 0.0f),
    "Treble +" to listOf(-1.5f, -1.0f, 0.0f, 0.0f, 0.0f, 2.0f, 3.5f, 2.5f, 1.0f, 1.5f),
    "Treble Boost" to listOf(-1.5f, -1.0f, 0.0f, 0.0f, 0.0f, 2.0f, 3.5f, 2.5f, 1.0f, 1.5f),
    "Vocal" to listOf(-2.0f, -1.5f, -0.5f, 0.5f, 1.5f, 3.0f, 3.5f, 2.0f, -1.0f, 0.5f),
    "Vocal Focus" to listOf(-2.0f, -1.5f, -0.5f, 0.5f, 1.5f, 3.0f, 3.5f, 2.0f, -1.0f, 0.5f),
    "Acoustic" to listOf(3.5f, 3.0f, 1.5f, 0.0f, 0.0f, 1.5f, 3.0f, 1.5f, -0.5f, 1.0f)
)

val PRESET_NAMES = listOf(
    "Reference Flat",
    "Harman Target",
    "Diffuse Field",
    "Vocal Clarity",
    "Warm Punch",
    "Custom"
)

private enum class MobileScreenTab(val label: String) {
    CONTROLS("Controls"),
    EQUALIZER("Equalizer")
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    onNavigateToSettings: () -> Unit = { onItemClick(Settings) },
    isDark: Boolean = false,
    onToggleTheme: () -> Unit = {},
    isHapticEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsStateWithLifecycle()
    val connectedDeviceAddress by viewModel.connectedDeviceAddress.collectAsStateWithLifecycle()
    val statusText by viewModel.statusText.collectAsStateWithLifecycle()
    val batteryLeft by viewModel.batteryLeft.collectAsStateWithLifecycle()
    val batteryRight by viewModel.batteryRight.collectAsStateWithLifecycle()
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val isGameMode by viewModel.isGameMode.collectAsStateWithLifecycle()
    val currentNoiseMode by viewModel.currentNoiseMode.collectAsStateWithLifecycle()
    val currentEq by viewModel.currentEq.collectAsStateWithLifecycle()
    val touchConfig by viewModel.touchConfig.collectAsStateWithLifecycle()

    val isNoisePending by viewModel.isNoisePending.collectAsStateWithLifecycle()
    val isGameModePending by viewModel.isGameModePending.collectAsStateWithLifecycle()
    val isEqPending by viewModel.isEqPending.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val isBtActive = (bluetoothManager?.adapter?.isEnabled == true) || isBluetoothEnabled
    val prefs = remember { context.getSharedPreferences("tozo_prefs", Context.MODE_PRIVATE) }

    val rawSavedPreset = remember { prefs.getString("active_preset", "Reference Flat") ?: "Reference Flat" }
    val savedPreset = remember(rawSavedPreset) {
        when (rawSavedPreset) {
            "Flat" -> "Reference Flat"
            "Bass +", "Bass Boost" -> "Warm Punch"
            "Treble +", "Treble Boost" -> "Diffuse Field"
            "Vocal", "Vocal Focus" -> "Vocal Clarity"
            "Acoustic", "Pop", "Rock", "Electronic", "R&B", "Dance", "Hip-Hop" -> "Harman Target"
            else -> if (PRESET_NAMES.contains(rawSavedPreset)) rawSavedPreset else "Reference Flat"
        }
    }
    val savedCustom = remember {
        prefs.getString("custom_eq_gains", null)?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 10 }
            ?: List(10) { 0f }
    }
    val savedEq = remember {
        prefs.getString("active_eq_gains", null)?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 10 }
            ?: (PRESET_MAP[savedPreset] ?: List(10) { 0f })
    }

    var activePreset by remember { mutableStateOf(savedPreset) }
    var eqGains by remember { mutableStateOf(savedEq) }
    var customGains by remember { mutableStateOf(savedCustom) }
    var selectedTab by remember { mutableStateOf(MobileScreenTab.CONTROLS) }

    fun persistEqState(preset: String, eq: List<Float>, custom: List<Float>) {
        prefs.edit()
            .putString("active_preset", preset)
            .putString("active_eq_gains", eq.joinToString(","))
            .putString("custom_eq_gains", custom.joinToString(","))
            .apply()
    }

    // Sync hardware EQ curve directly from TWS notifications
    LaunchedEffect(currentEq) {
        if (currentEq.size == 10) {
            val matchingPreset = PRESET_MAP.entries.firstOrNull { (_, presetGains) ->
                presetGains.zip(currentEq).all { (a, b) -> kotlin.math.abs(a - b) < 0.15f }
            }?.key

            if (matchingPreset != null) {
                activePreset = matchingPreset
                eqGains = currentEq
                persistEqState(matchingPreset, currentEq, customGains)
            } else if (currentEq.any { it != 0f }) {
                activePreset = "Custom"
                eqGains = currentEq
                customGains = currentEq
                persistEqState("Custom", currentEq, currentEq)
            }
        }
    }

    // BLE Permissions Handshake
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) viewModel.connect()
    }

    LaunchedEffect(Unit) {
        val hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermissions) {
            viewModel.connect()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                )
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val adapterOn = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled == true
        if (isConnected) {
            viewModel.queryDeviceTelemetry()
        } else if (adapterOn && !isConnecting) {
            viewModel.connect()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ============================================================
            // 1. TOP APP BAR (ACCESSIBLE CONTROLS, ZERO DESKTOP KEYS)
            // ============================================================
            TopDeviceBar(
                deviceName = if (isConnected && connectedDeviceName.isNotBlank()) connectedDeviceName else "TOZO AeroSound 3",
                macAddress = if (connectedDeviceAddress.isNotBlank()) connectedDeviceAddress else "Auto-Discovery",
                isConnected = isConnected,
                isConnecting = isConnecting,
                isBtDisabled = !isBtActive,
                statusText = statusText,
                onSettingsClick = onNavigateToSettings,
                onConnectClick = {
                    if (!isBtActive) {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                        } catch (_: Exception) {
                            viewModel.connect()
                        }
                    } else if (isConnected || isConnecting) {
                        viewModel.disconnect()
                    } else {
                        viewModel.connect()
                    }
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = 1.dp
            )

            // ============================================================
            // 2. ADAPTIVE CONTENT SURFACE
            // ============================================================
            if (isWideScreen) {
                // Wide Screen (Tablet / Landscape / Foldable): Side-by-Side Studio Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Controls & Telemetry
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BatteryTelemetryPanel(
                            batteryLeft = batteryLeft,
                            batteryRight = batteryRight,
                            firmwareVersion = firmwareVersion,
                            isConnected = isConnected
                        )

                        NoiseIsolationPanel(
                            currentMode = currentNoiseMode,
                            isConnected = isConnected,
                            isPending = isNoisePending,
                            onSelectMode = { viewModel.setNoiseMode(it) }
                        )

                        LowLatencyPanel(
                            isGameMode = isGameMode,
                            isConnected = isConnected,
                            isPending = isGameModePending,
                            onToggle = { viewModel.setGameMode(it) }
                        )

                        TouchControlsPanel(
                            touchConfig = touchConfig,
                            isConnected = isConnected,
                            onUpdateConfig = { viewModel.setTouchConfig(it) }
                        )

                        DeviceActionsPanel(
                            isConnected = isConnected,
                            onResetEq = {
                                val flat = List(10) { 0f }
                                eqGains = flat
                                customGains = flat
                                activePreset = "Reference Flat"
                                persistEqState("Reference Flat", flat, flat)
                                viewModel.resetEq()
                            },
                            onPowerOff = { viewModel.powerOff() }
                        )

                        AppFooter()
                    }

                    // Right Column: 10-Band Studio Equalizer
                    Column(
                        modifier = Modifier
                            .weight(1.35f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        EqualizerPanel(
                            activePreset = activePreset,
                            eqGains = eqGains,
                            isConnected = isConnected,
                            isPending = isEqPending,
                            onSelectPreset = { presetName ->
                                if (presetName == "Custom") {
                                    activePreset = "Custom"
                                    eqGains = customGains
                                    persistEqState("Custom", customGains, customGains)
                                    if (isConnected && !isEqPending) {
                                        viewModel.setEqGains(customGains)
                                    }
                                } else {
                                    activePreset = presetName
                                    val gains = PRESET_MAP[presetName] ?: List(10) { 0f }
                                    eqGains = gains
                                    persistEqState(presetName, gains, customGains)
                                    if (isConnected && !isEqPending) {
                                        viewModel.setEqGains(gains)
                                    }
                                }
                            },
                            onResetFlat = {
                                val flat = List(10) { 0f }
                                eqGains = flat
                                customGains = flat
                                activePreset = "Reference Flat"
                                persistEqState("Reference Flat", flat, flat)
                                if (isConnected && !isEqPending) {
                                    viewModel.setEqGains(flat)
                                }
                            },
                            onApply = {
                                persistEqState(activePreset, eqGains, customGains)
                                viewModel.setEqGains(eqGains)
                            },
                            onGainChange = { index, value ->
                                val updated = eqGains.toMutableList()
                                updated[index] = value
                                eqGains = updated
                                customGains = updated
                                activePreset = "Custom"
                                persistEqState("Custom", updated, updated)
                            }
                        )
                    }
                }
            } else {
                // Phone View: Segmented Tab Bar (Controls vs Equalizer)
                // Eliminates gesture collision between vertical page scrolling and fader sliders
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    SegmentedModeSelector(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val isGoingRight = targetState.ordinal > initialState.ordinal
                            val direction = if (isGoingRight) 1 else -1
                            (slideInHorizontally(
                                initialOffsetX = { (it * 0.12f * direction).toInt() },
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(200, easing = LinearOutSlowInEasing))) togetherWith
                            (slideOutHorizontally(
                                targetOffsetX = { (-it * 0.12f * direction).toInt() },
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing)))
                        },
                        label = "tab_content",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { targetTab ->
                        when (targetTab) {
                            MobileScreenTab.CONTROLS -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    BatteryTelemetryPanel(
                                        batteryLeft = batteryLeft,
                                        batteryRight = batteryRight,
                                        firmwareVersion = firmwareVersion,
                                        isConnected = isConnected
                                    )

                                    NoiseIsolationPanel(
                                        currentMode = currentNoiseMode,
                                        isConnected = isConnected,
                                        isPending = isNoisePending,
                                        onSelectMode = { viewModel.setNoiseMode(it) }
                                    )

                                    LowLatencyPanel(
                                        isGameMode = isGameMode,
                                        isConnected = isConnected,
                                        isPending = isGameModePending,
                                        onToggle = { viewModel.setGameMode(it) }
                                    )

                                    TouchControlsPanel(
                                        touchConfig = touchConfig,
                                        isConnected = isConnected,
                                        onUpdateConfig = { viewModel.setTouchConfig(it) }
                                    )

                                    DeviceActionsPanel(
                                        isConnected = isConnected,
                                        onResetEq = {
                                             val flat = List(10) { 0f }
                                             eqGains = flat
                                             customGains = flat
                                             activePreset = "Reference Flat"
                                             persistEqState("Reference Flat", flat, flat)
                                             viewModel.resetEq()
                                        },
                                        onPowerOff = { viewModel.powerOff() }
                                    )

                                    AppFooter()
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                            MobileScreenTab.EQUALIZER -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    EqualizerPanel(
                                        activePreset = activePreset,
                                        eqGains = eqGains,
                                        isConnected = isConnected,
                                        isPending = isEqPending,
                                        onSelectPreset = { presetName ->
                                            if (presetName == "Custom") {
                                                activePreset = "Custom"
                                                eqGains = customGains
                                                persistEqState("Custom", customGains, customGains)
                                                if (isConnected && !isEqPending) {
                                                    viewModel.setEqGains(customGains)
                                                }
                                            } else {
                                                activePreset = presetName
                                                val gains = PRESET_MAP[presetName] ?: List(10) { 0f }
                                                eqGains = gains
                                                persistEqState(presetName, gains, customGains)
                                                if (isConnected && !isEqPending) {
                                                    viewModel.setEqGains(gains)
                                                }
                                            }
                                        },
                                        onResetFlat = {
                                            val flat = List(10) { 0f }
                                            eqGains = flat
                                            customGains = flat
                                            activePreset = "Reference Flat"
                                            persistEqState("Reference Flat", flat, flat)
                                            if (isConnected && !isEqPending) {
                                                viewModel.setEqGains(flat)
                                            }
                                        },
                                        onApply = {
                                            persistEqState(activePreset, eqGains, customGains)
                                            viewModel.setEqGains(eqGains)
                                        },
                                        onGainChange = { index, value ->
                                            val updated = eqGains.toMutableList()
                                            updated[index] = value
                                            eqGains = updated
                                            customGains = updated
                                            activePreset = "Custom"
                                            persistEqState("Custom", updated, updated)
                                        }
                                    )

                                    AppFooter()
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 1. TOP APP BAR & CONNECTION HEADER
// ============================================================

@Composable
private fun TopDeviceBar(
    deviceName: String,
    macAddress: String,
    isConnected: Boolean,
    isConnecting: Boolean,
    isBtDisabled: Boolean,
    statusText: String,
    onSettingsClick: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dotColor = when {
                        isConnected -> StatusGreen
                        isConnecting -> StatusAmber
                        isBtDisabled -> StatusRed
                        else -> StatusMuted
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = when {
                            isConnected -> if (macAddress.isNotBlank() && macAddress != "Auto-Discovery") macAddress else "Connected"
                            isConnecting -> if (statusText.isNotBlank()) statusText else "Connecting..."
                            isBtDisabled -> "Bluetooth Disabled"
                            statusText.isNotBlank() && statusText != "Disconnected" && !statusText.contains("Bluetooth is disabled", ignoreCase = true) -> statusText
                            else -> "Disconnected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBtDisabled) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Accessible Settings Button (48x48dp touch target)
                SettingsIconButton(
                    onClick = onSettingsClick
                )

                // Primary Connection Button (Min height 48dp, touch target 48dp)
                Button(
                    onClick = onConnectClick,
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        contentColor = if (isConnected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Connecting",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = when {
                                isConnected -> "Disconnect"
                                isBtDisabled -> "Enable BT"
                                else -> "Connect"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val strokeColor = MaterialTheme.colorScheme.onSurface
        Canvas(modifier = Modifier.size(20.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = 6.5.dp.toPx()
            val innerRadius = 3.dp.toPx()

            drawCircle(
                color = strokeColor,
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )
            drawCircle(
                color = strokeColor,
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            for (i in 0 until 6) {
                val angle = (i * 60f) * (Math.PI / 180f).toFloat()
                val cosA = kotlin.math.cos(angle)
                val sinA = kotlin.math.sin(angle)
                val toothStart = outerRadius - 0.5.dp.toPx()
                val toothEnd = outerRadius + 2.5.dp.toPx()
                drawLine(
                    color = strokeColor,
                    start = Offset(center.x + cosA * toothStart, center.y + sinA * toothStart),
                    end = Offset(center.x + cosA * toothEnd, center.y + sinA * toothEnd),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}


// ============================================================
// 2. SEGMENTED MODE SELECTOR (PHONE VIEW)
// ============================================================

@Composable
private fun SegmentedModeSelector(
    selectedTab: MobileScreenTab,
    onTabSelected: (MobileScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalHapticEnabled.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MobileScreenTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val bg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "tab_bg_${tab.name}"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "tab_text_${tab.name}"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .clickable {
                        haptic.performIf(hapticEnabled, HapticFeedbackType.LongPress)
                        onTabSelected(tab)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

// ============================================================
// 3. BATTERY TELEMETRY PANEL
// ============================================================

@Composable
private fun BatteryTelemetryPanel(
    batteryLeft: Int,
    batteryRight: Int,
    firmwareVersion: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Battery Status",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isConnected && firmwareVersion.isNotBlank()) {
                Text(
                    text = "FW $firmwareVersion",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EarbudBatteryPod(
                channelLabel = "Left Earbud",
                percentage = batteryLeft,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            EarbudBatteryPod(
                channelLabel = "Right Earbud",
                percentage = batteryRight,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EarbudBatteryPod(
    channelLabel: String,
    percentage: Int,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isConnected) (percentage.coerceIn(0, 100) / 100f) else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "battery_fill"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = channelLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (isConnected && percentage > 0) "$percentage" else "--",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (isConnected && percentage > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isConnected && percentage > 0) {
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gauge bar with animated semantic battery warning
            val targetBarColor = when {
                !isConnected -> MaterialTheme.colorScheme.outline
                percentage <= 20 -> StatusRed
                percentage <= 35 -> StatusAmber
                else -> StatusGreen
            }

            val animatedBarColor by animateColorAsState(
                targetValue = targetBarColor,
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                label = "battery_bar_color"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline)
            ) {
                if (isConnected && percentage > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(animatedBarColor)
                    )
                }
            }
        }
    }
}

// ============================================================
// 4. NOISE ISOLATION PANEL (ACCESSIBLE 48DP+ GRID)
// ============================================================

@Composable
private fun NoiseIsolationPanel(
    currentMode: NoiseMode,
    isConnected: Boolean,
    isPending: Boolean,
    onSelectMode: (NoiseMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier.alpha(if (isConnected) 1f else 0.7f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Noise Control",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isPending) {
                Text(
                    text = "Switching...",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ANC and ambient sound modes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Row 1: Primary Modes (ANC, Transparency, Normal)
        val primaryModes = listOf(
            Triple(NoiseMode.ANC, "ANC", "Shield"),
            Triple(NoiseMode.TRANSPARENCY, "Pass", "Aware"),
            Triple(NoiseMode.NORMAL, "Normal", "Direct")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            primaryModes.forEach { (mode, label, sub) ->
                NoiseModeTile(
                    title = label,
                    subtitle = sub,
                    isSelected = isConnected && currentMode == mode,
                    enabled = isConnected && !isPending,
                    onClick = { onSelectMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Specialized Modes (Leisure, Wind Reduction)
        val secondaryModes = listOf(
            Triple(NoiseMode.LEISURE, "Leisure", "Soft Ambient Cut"),
            Triple(NoiseMode.REDUCE_WIND, "Wind Reduction", "Aero Filter")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            secondaryModes.forEach { (mode, label, sub) ->
                NoiseModeTile(
                    title = label,
                    subtitle = sub,
                    isSelected = isConnected && currentMode == mode,
                    enabled = isConnected && !isPending,
                    onClick = { onSelectMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NoiseModeTile(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalHapticEnabled.current

    val bg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "noise_tile_bg_$title"
    )
    val borderCol by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "noise_tile_border_$title"
    )
    val titleCol by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "noise_tile_title_$title"
    )
    val subCol by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "noise_tile_sub_$title"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                onClick = {
                    haptic.performIf(hapticEnabled, HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (enabled) titleCol else titleCol.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) subCol else subCol.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================
// 5. LOW LATENCY AUDIO PANEL
// ============================================================

@Composable
private fun LowLatencyPanel(
    isGameMode: Boolean,
    isConnected: Boolean,
    isPending: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier.alpha(if (isConnected) 1f else 0.7f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Low Latency Game Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isPending) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Syncing...",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusAmber
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isGameMode && isConnected) {
                        "45ms low latency for gaming and video"
                    } else {
                        "Standard latency"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = isGameMode && isConnected,
                enabled = isConnected && !isPending,
                onCheckedChange = onToggle
            )
        }
    }
}

// ============================================================
// 6. 10-BAND STUDIO EQUALIZER PANEL
// ============================================================

@Composable
private fun EqualizerPanel(
    activePreset: String,
    eqGains: List<Float>,
    isConnected: Boolean,
    isPending: Boolean,
    onSelectPreset: (String) -> Unit,
    onResetFlat: () -> Unit,
    onApply: () -> Unit,
    onGainChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier.alpha(if (isConnected) 1f else 0.85f)
    ) {
        // Equalizer Header & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Studio Equalizer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "10-Band DSP (±10 dB)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset / Flat Button
                OutlinedButton(
                    onClick = onResetFlat,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                ) {
                    Text(
                        text = "Flat",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Apply Profile Button
                Button(
                    onClick = onApply,
                    enabled = isConnected && !isPending,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                ) {
                    if (isPending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Applying",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Apply",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Pills (Virtualized LazyRow with smooth 120Hz recycling)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PRESET_NAMES, key = { it }) { presetName ->
                PresetPill(
                    presetName = presetName,
                    isSelected = activePreset == presetName,
                    onClick = { onSelectPreset(presetName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive 10-Band Graphic Equalizer Curve & Studio Fine-Tuning Strip
        // Renders all 10 bands simultaneously with zero horizontal overflow
        InteractiveGraphicEq(
            eqGains = eqGains,
            enabled = true,
            onGainChange = onGainChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PresetPill(
    presetName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalHapticEnabled.current

    val bg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "preset_bg"
    )
    val borderCol by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "preset_border"
    )
    val fg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "preset_fg"
    )

    Surface(
        onClick = {
            haptic.performIf(hapticEnabled, HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderCol),
        modifier = modifier.defaultMinSize(minHeight = 44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(150)) + expandHorizontally(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally(animationSpec = tween(100))
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(fg)
                )
            }
            Text(
                text = presetName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FrequencyBandPill(
    freqLabel: String,
    gainDb: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val bg by animateColorAsState(
        targetValue = if (isSelected) primaryColor else surfaceVariantColor,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "band_bg_$freqLabel"
    )
    val borderCol by animateColorAsState(
        targetValue = if (isSelected) primaryColor else outlineColor,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "band_border_$freqLabel"
    )

    val gainStr = "${if (gainDb > 0) "+" else ""}${"%.1f".format(gainDb)}"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderCol),
        modifier = modifier.defaultMinSize(minWidth = 54.dp, minHeight = 48.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = freqLabel,
                style = TelemetryLabelStyle,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) onPrimaryColor else onSurfaceColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = gainStr,
                style = TelemetryLabelStyle,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) onPrimaryColor.copy(alpha = 0.9f) else onSurfaceVariantColor
            )
        }
    }
}

@Composable
private fun InteractiveGraphicEq(
    eqGains: List<Float>,
    enabled: Boolean,
    onGainChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBandIndex by remember { mutableIntStateOf(4) } // Default selection: 500 Hz
    val haptic = LocalHapticFeedback.current
    val isHaptic = LocalHapticEnabled.current
    val currentOnGainChange by rememberUpdatedState(onGainChange)

    fun triggerHaptic() {
        haptic.performIf(isHaptic, HapticFeedbackType.TextHandleMove)
    }
    val textMeasurer = rememberTextMeasurer()

    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    // Reusable Path allocations to eliminate per-frame GC allocations
    val curvePath = remember { Path() }
    val fillPath = remember { Path() }

    // Pre-measured static label layouts to prevent layout thrashing inside Canvas draw loop
    val cachedDbLayouts = remember(textMeasurer, onSurfaceColor, onSurfaceVariantColor) {
        listOf(
            10f to textMeasurer.measure(
                text = "+10",
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal, color = onSurfaceVariantColor.copy(alpha = 0.6f))
            ),
            0f to textMeasurer.measure(
                text = "0",
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = onSurfaceColor.copy(alpha = 0.8f))
            ),
            -10f to textMeasurer.measure(
                text = "-10",
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal, color = onSurfaceVariantColor.copy(alpha = 0.6f))
            )
        )
    }

    val cachedFreqLayouts = remember(textMeasurer, primaryColor, onSurfaceVariantColor) {
        FREQ_LABELS.mapIndexed { index, freqText ->
            textMeasurer.measure(
                text = freqText,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            ) to textMeasurer.measure(
                text = freqText,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal, color = onSurfaceVariantColor)
            )
        }
    }

    // Pre-measure all 41 possible dB badge text layouts (-10.0 to +10.0 in 0.5dB steps) for 0-allocation Canvas tooltip
    val cachedBadgeLayouts = remember(textMeasurer, onSurfaceColor) {
        (-20..20).associate { step ->
            val db = step * 0.5f
            val str = "${if (db > 0) "+" else ""}${"%.1f".format(db)} dB"
            val layout = textMeasurer.measure(
                text = str,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurfaceColor
                )
            )
            step to layout
        }
    }

    // M3 Expressive tactile spring animation for all 10 hardware EQ bands
    val animatedGains = (0..9).map { index ->
        val target = eqGains.getOrElse(index) { 0f }.coerceIn(-10f, 10f)
        animateFloatAsState(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "band_fader_$index"
        ).value
    }

    Column(modifier = modifier) {
        // 1. Interactive Graphic Curve Canvas Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceVariantColor.copy(alpha = 0.35f))
                .border(1.dp, outlineColor, RoundedCornerShape(12.dp))
                .padding(top = 12.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        val leftPad = 28.dp.toPx()
                        val rightPad = 14.dp.toPx()
                        val topPad = 16.dp.toPx()
                        val bottomPad = 22.dp.toPx()
                        val usableWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                        val usableHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                        val bandStep = usableWidth / 9f

                        fun handlePointer(pos: Offset) {
                            val rawIndex = ((pos.x - leftPad + bandStep / 2f) / bandStep).toInt()
                            val bandIndex = rawIndex.coerceIn(0, 9)
                            selectedBandIndex = bandIndex

                            val clampedY = (pos.y - topPad).coerceIn(0f, usableHeight)
                            val frac = 1f - (clampedY / usableHeight)
                            val rawDb = frac * 20f - 10f
                            val roundedDb = (rawDb * 2).roundToInt() / 2.0f

                            if (roundedDb != eqGains.getOrElse(bandIndex) { 0f }) {
                                triggerHaptic()
                                currentOnGainChange(bandIndex, roundedDb)
                            }
                        }

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            handlePointer(down.position)

                            val pointer = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointer } ?: break
                                if (!change.pressed) break
                                change.consume()
                                handlePointer(change.position)
                            }
                        }
                    }
            ) {
                val leftPad = 28.dp.toPx()
                val rightPad = 14.dp.toPx()
                val topPad = 16.dp.toPx()
                val bottomPad = 22.dp.toPx()
                val usableWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                val usableHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                val bandStep = usableWidth / 9f
                val zeroY = topPad + usableHeight * 0.5f

                // Horizontal Reference Grid Lines & dB Labels (+10, 0, -10 dB)
                val dbMarks = listOf(
                    10f to topPad,
                    5f to topPad + usableHeight * 0.25f,
                    0f to zeroY,
                    -5f to topPad + usableHeight * 0.75f,
                    -10f to topPad + usableHeight
                )

                for ((db, y) in dbMarks) {
                    val isZero = db == 0f
                    drawLine(
                        color = if (isZero) outlineColor.copy(alpha = 0.9f) else outlineColor.copy(alpha = 0.3f),
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = if (isZero) 1.5.dp.toPx() else 0.75.dp.toPx()
                    )

                    val dbLayout = cachedDbLayouts.firstOrNull { it.first == db }?.second
                    if (dbLayout != null) {
                        drawText(
                            textLayoutResult = dbLayout,
                            topLeft = Offset(leftPad - dbLayout.size.width - 4.dp.toPx(), y - dbLayout.size.height / 2f)
                        )
                    }
                }

                // Vertical Band Guides & Bottom Frequency Labels
                val points = mutableListOf<Offset>()
                for (i in 0..9) {
                    val x = leftPad + i * bandStep
                    val isSelected = i == selectedBandIndex

                    drawLine(
                        color = if (isSelected) primaryColor.copy(alpha = 0.35f) else outlineColor.copy(alpha = 0.2f),
                        start = Offset(x, topPad),
                        end = Offset(x, topPad + usableHeight),
                        strokeWidth = if (isSelected) 1.25.dp.toPx() else 0.75.dp.toPx()
                    )

                    val (selectedFreqLayout, normalFreqLayout) = cachedFreqLayouts[i]
                    val freqLayout = if (isSelected) selectedFreqLayout else normalFreqLayout
                    drawText(
                        textLayoutResult = freqLayout,
                        topLeft = Offset(x - freqLayout.size.width / 2f, size.height - bottomPad + 5.dp.toPx())
                    )

                    val gain = animatedGains.getOrElse(i) { 0f }.coerceIn(-10f, 10f)
                    val norm = (gain + 10f) / 20f
                    val y = topPad + (1f - norm) * usableHeight
                    points.add(Offset(x, y))
                }

                // Reusable Spline Curve Path (zero allocation)
                curvePath.reset()
                curvePath.moveTo(points[0].x, points[0].y)
                for (i in 0 until 9) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val dx = p1.x - p0.x
                    val c1 = Offset(p0.x + dx * 0.5f, p0.y)
                    val c2 = Offset(p1.x - dx * 0.5f, p1.y)
                    curvePath.cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)
                }

                // Reusable Translucent Fill Path (zero allocation)
                fillPath.reset()
                fillPath.moveTo(points[0].x, zeroY)
                fillPath.lineTo(points[0].x, points[0].y)
                for (i in 0 until 9) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val dx = p1.x - p0.x
                    val c1 = Offset(p0.x + dx * 0.5f, p0.y)
                    val c2 = Offset(p1.x - dx * 0.5f, p1.y)
                    fillPath.cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)
                }
                fillPath.lineTo(points[9].x, zeroY)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    color = primaryColor.copy(alpha = 0.12f)
                )

                // Draw Smooth Curve Stroke
                drawPath(
                    path = curvePath,
                    color = primaryColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Band Control Nodes
                for (i in 0..9) {
                    val pt = points[i]
                    val isSelected = i == selectedBandIndex

                    if (isSelected) {
                        drawCircle(primaryColor.copy(alpha = 0.25f), radius = 12.dp.toPx(), center = pt)
                        drawCircle(primaryColor, radius = 6.5.dp.toPx(), center = pt)
                        drawCircle(surfaceColor, radius = 2.5.dp.toPx(), center = pt)
                    } else {
                        drawCircle(surfaceColor, radius = 4.5.dp.toPx(), center = pt)
                        drawCircle(outlineColor, radius = 4.5.dp.toPx(), center = pt, style = Stroke(1.5.dp.toPx()))
                        drawCircle(primaryColor.copy(alpha = 0.7f), radius = 1.75.dp.toPx(), center = pt)
                    }
                }

                // Tooltip Pill above or below Active Node (zero-allocation cached layout)
                val selPt = points[selectedBandIndex]
                val selGain = eqGains.getOrElse(selectedBandIndex) { 0f }
                val stepKey = (selGain * 2).roundToInt().coerceIn(-20, 20)
                val badgeText = cachedBadgeLayouts[stepKey] ?: textMeasurer.measure(
                    text = "${if (selGain > 0) "+" else ""}${"%.1f".format(selGain)} dB",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurfaceColor
                    )
                )

                val badgePadH = 6.dp.toPx()
                val badgePadV = 3.dp.toPx()
                val badgeW = badgeText.size.width + badgePadH * 2
                val badgeH = badgeText.size.height + badgePadV * 2

                val badgeCenterY = if (selPt.y - 22.dp.toPx() > topPad) {
                    selPt.y - 18.dp.toPx()
                } else {
                    selPt.y + 18.dp.toPx()
                }
                val maxBadgeX = (size.width - rightPad - badgeW / 2f).coerceAtLeast(leftPad + badgeW / 2f)
                val badgeCenterX = selPt.x.coerceIn(leftPad + badgeW / 2f, maxBadgeX)
                val badgeTopLeft = Offset(badgeCenterX - badgeW / 2f, badgeCenterY - badgeH / 2f)

                drawRoundRect(
                    color = surfaceColor,
                    topLeft = badgeTopLeft,
                    size = Size(badgeW, badgeH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = outlineColor,
                    topLeft = badgeTopLeft,
                    size = Size(badgeW, badgeH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(1.dp.toPx())
                )
                drawText(
                    textLayoutResult = badgeText,
                    topLeft = Offset(badgeCenterX - badgeText.size.width / 2f, badgeCenterY - badgeText.size.height / 2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. 10-Band Frequency Selector Buttons (Instant direct band switching)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Frequency Bands",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = onSurfaceVariantColor
            )
            Text(
                text = "Tap to select",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariantColor.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (0..9).forEach { bandIdx ->
                val isSelected = bandIdx == selectedBandIndex
                val gainVal = eqGains.getOrElse(bandIdx) { 0f }
                val freqLbl = FREQ_LABELS[bandIdx]

                FrequencyBandPill(
                    freqLabel = freqLbl,
                    gainDb = gainVal,
                    isSelected = isSelected,
                    onClick = {
                        triggerHaptic()
                        selectedBandIndex = bandIdx
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Tactile Band Fine-Tuning Card with Slider & Steppers
        val selectedGain = eqGains.getOrElse(selectedBandIndex) { 0f }
        val selectedFreqLabel = FREQ_LABELS.getOrElse(selectedBandIndex) { "" }
        val formattedFreq = if (selectedFreqLabel.endsWith("k")) {
            "${selectedFreqLabel.removeSuffix("k")} kHz"
        } else {
            "$selectedFreqLabel Hz"
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = surfaceVariantColor.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, outlineColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Row 1: Prev Band [<], Active Frequency & dB, Next Band [>]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (selectedBandIndex > 0) {
                                triggerHaptic()
                                selectedBandIndex--
                            }
                        },
                        enabled = enabled && selectedBandIndex > 0,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            text = "< Prev",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formattedFreq,
                            style = TelemetryLabelStyle,
                            color = onSurfaceVariantColor
                        )
                        Text(
                            text = "${if (selectedGain > 0) "+" else ""}${"%.1f".format(selectedGain)} dB",
                            style = TelemetryValueStyle,
                            color = onSurfaceColor
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            if (selectedBandIndex < 9) {
                                triggerHaptic()
                                selectedBandIndex++
                            }
                        },
                        enabled = enabled && selectedBandIndex < 9,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            text = "Next >",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Slider
                Slider(
                    value = selectedGain,
                    onValueChange = { newVal ->
                        val rounded = (newVal * 2).roundToInt() / 2.0f
                        if (rounded != selectedGain) {
                            triggerHaptic()
                            currentOnGainChange(selectedBandIndex, rounded)
                        }
                    },
                    valueRange = -10.0f..10.0f,
                    steps = 39,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: Tactile Stepper Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decrement: -0.5 dB
                    Button(
                        onClick = {
                            val newG = (selectedGain - 0.5f).coerceAtLeast(-10.0f)
                            triggerHaptic()
                            currentOnGainChange(selectedBandIndex, newG)
                        },
                        enabled = enabled && selectedGain > -10.0f,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = surfaceColor,
                            contentColor = onSurfaceColor
                        ),
                        border = BorderStroke(1.dp, outlineColor),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            text = "-0.5 dB",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Reset: 0 dB
                    OutlinedButton(
                        onClick = {
                            triggerHaptic()
                            currentOnGainChange(selectedBandIndex, 0.0f)
                        },
                        enabled = enabled && selectedGain != 0.0f,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            text = "0 dB",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Increment: +0.5 dB
                    Button(
                        onClick = {
                            val newG = (selectedGain + 0.5f).coerceAtMost(10.0f)
                            triggerHaptic()
                            currentOnGainChange(selectedBandIndex, newG)
                        },
                        enabled = enabled && selectedGain < 10.0f,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = surfaceColor,
                            contentColor = onSurfaceColor
                        ),
                        border = BorderStroke(1.dp, outlineColor),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            text = "+0.5 dB",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 6. HARDWARE EXTENSIONS: SPATIAL AUDIO, SOUND BALANCE, TOUCH & ACTIONS
// ============================================================
// 6. HARDWARE EXTENSIONS: TOUCH CONTROLS & EARBUD ACTIONS
// ============================================================

@Composable
private fun TouchControlsPanel(
    touchConfig: TouchConfig,
    isConnected: Boolean,
    onUpdateConfig: (TouchConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEarbud by remember { mutableStateOf(0) } // 0 = Left, 1 = Right
    var editingGesture by remember { mutableStateOf<Pair<String, (TouchAction) -> Unit>?>(null) }

    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Touch Controls",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedEarbud == 0) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { selectedEarbud = 0 }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Left",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedEarbud == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedEarbud == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedEarbud == 1) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { selectedEarbud = 1 }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Right",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedEarbud == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedEarbud == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedEarbud == 0) {
            TouchSlotRow("Single Tap", touchConfig.leftSingle, isConnected) {
                editingGesture = "Left Single Tap" to { action ->
                    onUpdateConfig(touchConfig.copy(leftSingle = action))
                }
            }
            TouchSlotRow("Double Tap", touchConfig.leftDouble, isConnected) {
                editingGesture = "Left Double Tap" to { action ->
                    onUpdateConfig(touchConfig.copy(leftDouble = action))
                }
            }
            TouchSlotRow("Triple Tap", touchConfig.leftTriple, isConnected) {
                editingGesture = "Left Triple Tap" to { action ->
                    onUpdateConfig(touchConfig.copy(leftTriple = action))
                }
            }
            TouchSlotRow("Long Press", touchConfig.leftLong, isConnected) {
                editingGesture = "Left Long Press" to { action ->
                    onUpdateConfig(touchConfig.copy(leftLong = action))
                }
            }
        } else {
            TouchSlotRow("Single Tap", touchConfig.rightSingle, isConnected) {
                editingGesture = "Right Single Tap" to { action ->
                    onUpdateConfig(touchConfig.copy(rightSingle = action))
                }
            }
            TouchSlotRow("Double Tap", touchConfig.rightDouble, isConnected) {
                editingGesture = "Right Double Tap" to { action ->
                    onUpdateConfig(touchConfig.copy(rightDouble = action))
                }
            }
            TouchSlotRow("Triple Tap", touchConfig.rightTriple, isConnected) {
                editingGesture = "Right Triple Tap" to { action ->
                    onUpdateConfig(touchConfig.copy(rightTriple = action))
                }
            }
            TouchSlotRow("Long Press", touchConfig.rightLong, isConnected) {
                editingGesture = "Right Long Press" to { action ->
                    onUpdateConfig(touchConfig.copy(rightLong = action))
                }
            }
        }
    }

    editingGesture?.let { (title, callback) ->
        AlertDialog(
            onDismissRequest = { editingGesture = null },
            title = { Text(title, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TouchAction.entries.forEach { action ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    callback(action)
                                    editingGesture = null
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { editingGesture = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TouchSlotRow(
    gesture: String,
    action: TouchAction,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isConnected, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = gesture,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun DeviceActionsPanel(
    isConnected: Boolean,
    onResetEq: () -> Unit,
    onPowerOff: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPowerOffConfirm by remember { mutableStateOf(false) }

    SectionCard(modifier = modifier) {
        Text(
            text = "Earbud Controls",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onResetEq,
                enabled = isConnected,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Reset EQ", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = { showPowerOffConfirm = true },
                enabled = isConnected,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Power Off", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (showPowerOffConfirm) {
        AlertDialog(
            onDismissRequest = { showPowerOffConfirm = false },
            title = { Text("Power Off Earbuds") },
            text = { Text("Are you sure you want to shut down the TOZO AeroSound 3 earbuds?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPowerOffConfirm = false
                        onPowerOff()
                    }
                ) {
                    Text("Power Off", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPowerOffConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ============================================================
// 7. COMMON UTILITY CONTAINERS & FOOTER
// ============================================================

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun AppFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appVersion = remember {
        try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TOZO Control  •  v$appVersion",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Open Source BLE Audio Companion",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
