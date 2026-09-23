package com.ourkode.tozocontrol.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.ourkode.tozocontrol.data.NoiseMode
import com.ourkode.tozocontrol.data.TozoBleManager
import com.ourkode.tozocontrol.theme.*
import kotlin.math.roundToInt

// ============================================================
// Utilitarian Minimalist Constants & Sound Profiles
// ============================================================

val FREQ_LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

val PRESET_MAP = mapOf(
    "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Bass Boost" to listOf(6.0f, 5.0f, 4.0f, 2.0f, 0f, 0f, 0f, 0f, 1.0f, 2.0f),
    "Treble Boost" to listOf(0f, 0f, 0f, 0f, 0f, 1.0f, 2.0f, 3.5f, 5.0f, 6.0f),
    "Vocal Focus" to listOf(-1.0f, 0f, 1.5f, 3.0f, 4.0f, 4.0f, 2.5f, 1.0f, 0f, -1.0f),
    "Acoustic" to listOf(2.0f, 1.5f, 0.5f, 0f, 0f, 0.5f, 1.5f, 2.0f, 1.5f, 1.0f)
)

val PRESET_NAMES = listOf("Flat", "Bass Boost", "Treble Boost", "Vocal Focus", "Acoustic", "Custom")

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    isDark: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsStateWithLifecycle()
    val connectedDeviceAddress by viewModel.connectedDeviceAddress.collectAsStateWithLifecycle()
    val statusText by viewModel.statusText.collectAsStateWithLifecycle()
    val batteryLeft by viewModel.batteryLeft.collectAsStateWithLifecycle()
    val batteryRight by viewModel.batteryRight.collectAsStateWithLifecycle()
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val isGameMode by viewModel.isGameMode.collectAsStateWithLifecycle()
    val currentNoiseMode by viewModel.currentNoiseMode.collectAsStateWithLifecycle()
    val currentEq by viewModel.currentEq.collectAsStateWithLifecycle()

    val isNoisePending by viewModel.isNoisePending.collectAsStateWithLifecycle()
    val isGameModePending by viewModel.isGameModePending.collectAsStateWithLifecycle()
    val isEqPending by viewModel.isEqPending.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tozo_prefs", Context.MODE_PRIVATE) }

    val savedPreset = remember { prefs.getString("active_preset", "Flat") ?: "Flat" }
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
    var hasUserEditedEq by remember { mutableStateOf(true) }

    fun persistEqState(preset: String, eq: List<Float>, custom: List<Float>) {
        prefs.edit()
            .putString("active_preset", preset)
            .putString("active_eq_gains", eq.joinToString(","))
            .putString("custom_eq_gains", custom.joinToString(","))
            .apply()
    }

    // Sync hardware EQ curve directly from TWS
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

    // BLE Permissions Handshake & Auto-Connect
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

    // Warm Bone / Utilitarian Canvas
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ============================================================
            // 1. EDITORIAL HEADER & WORKSPACE NAVIGATION
            // ============================================================
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Eyebrow("AUDIO HARDWARE INTERFACE")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isConnected && connectedDeviceName.isNotBlank()) connectedDeviceName else "TOZO AeroSound",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val macDisplay = if (connectedDeviceAddress.isNotBlank()) connectedDeviceAddress else "AUTO-DETECT"
                        Text(
                            text = "Model A3  •  MAC $macDisplay",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Keystroke micro-UI for theme switching
                    KeystrokeChip(
                        text = if (isDark) "LIGHT [T]" else "DARK [T]",
                        onClick = onToggleTheme
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Connection Status & Primary Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MinimalBadge(
                                text = when {
                                    isConnected -> "ONLINE"
                                    isConnecting -> "CONNECTING"
                                    else -> "OFFLINE"
                                },
                                isSuccess = isConnected || isConnecting,
                                isDark = isDark
                            )
                            Text(
                                text = when {
                                    isConnected -> "BLE 5.3 LINKED"
                                    isConnecting -> statusText
                                    else -> if (statusText.isNotBlank() && statusText != "Ready") statusText else "DISCONNECTED"
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    MinimalButton(
                        text = when {
                            isConnecting -> "CONNECTING..."
                            isConnected -> "DISCONNECT"
                            else -> "CONNECT"
                        },
                        isLoading = isConnecting,
                        enabled = true,
                        onClick = {
                            if (isConnected || isConnecting) {
                                viewModel.disconnect()
                            } else {
                                viewModel.connect()
                            }
                        },
                        isPrimary = !isConnected,
                        modifier = Modifier.defaultMinSize(minWidth = 124.dp)
                    )
                }
            }

            // Hairline separator
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = 1.dp
            )

            // ============================================================
            // 2. BATTERY TELEMETRY BENTO POD
            // ============================================================
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Eyebrow("BATTERY TELEMETRY")
                    Text(
                        text = if (isConnected) "FIRMWARE $firmwareVersion" else "OFFLINE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MinimalBatteryPod(
                        unitName = "LEFT EARBUD",
                        percentage = batteryLeft,
                        isConnected = isConnected,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    MinimalBatteryPod(
                        unitName = "RIGHT EARBUD",
                        percentage = batteryRight,
                        isConnected = isConnected,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ============================================================
            // 3. LOW LATENCY DSP CARD (WITH RATE LIMITING)
            // ============================================================
            BentoCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isConnected) 1f else 0.72f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow("LATENCY BUFFER")
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isGameModePending) {
                                MinimalBadge(
                                    text = "SYNCING...",
                                    isSuccess = true,
                                    isDark = isDark
                                )
                            } else if (isGameMode && isConnected) {
                                MinimalBadge(
                                    text = "45MS DSP",
                                    isSuccess = true,
                                    isDark = isDark
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Low Latency Audio",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!isConnected) {
                                "Connect earbuds to engage hardware buffer"
                            } else if (isGameModePending) {
                                "Switching transmission buffer parameter..."
                            } else if (isGameMode) {
                                "Synchronous transmission buffer engaged for gaming and video"
                            } else {
                                "Standard audio buffer engaged for baseline battery longevity"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Switch(
                        checked = isGameMode && isConnected,
                        enabled = isConnected && !isGameModePending,
                        onCheckedChange = { if (isConnected && !isGameModePending) viewModel.setGameMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            checkedTrackColor = MaterialTheme.colorScheme.onSurface,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                            disabledCheckedThumbColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            disabledUncheckedThumbColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            disabledUncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // ============================================================
            // 4. ACOUSTIC ISOLATION PROFILES (WITH RATE LIMITING)
            // ============================================================
            BentoCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isConnected) 1f else 0.72f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow("ACOUSTIC ENVIRONMENT")
                            if (isNoisePending) {
                                Spacer(modifier = Modifier.width(8.dp))
                                MinimalBadge(
                                    text = "SWITCHING...",
                                    isSuccess = true,
                                    isDark = isDark
                                )
                            }
                        }
                        Text(
                            text = "PHASE CANCELLATION",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Noise Isolation Profile",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Select external microphone feedback filtering parameter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val noiseModes = listOf(
                        Triple(NoiseMode.ANC, "ANC", "Shield"),
                        Triple(NoiseMode.TRANSPARENCY, "Pass", "Aware"),
                        Triple(NoiseMode.LEISURE, "Leisure", "Soft Cut"),
                        Triple(NoiseMode.REDUCE_WIND, "Wind", "Aero"),
                        Triple(NoiseMode.NORMAL, "Normal", "Direct")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        noiseModes.forEach { (mode, label, sub) ->
                            val isSelected = isConnected && currentNoiseMode == mode
                            val tileEnabled = isConnected && !isNoisePending

                            MinimalNoiseTile(
                                title = label,
                                subtitle = if (isSelected && isNoisePending) "WAIT..." else sub,
                                isSelected = isSelected,
                                enabled = tileEnabled,
                                onClick = {
                                    if (isConnected && !isNoisePending) {
                                        viewModel.setNoiseMode(mode)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ============================================================
            // 5. 10-BAND HARDWARE DSP EQUALIZER (CUSTOMIZABLE + RATE LIMITING)
            // ============================================================
            BentoCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isConnected) 1f else 0.85f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Eyebrow("HARDWARE DSP FILTER")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Acoustic Equalizer",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activePreset == "Custom" && eqGains.any { it != 0f }) {
                                KeystrokeChip(
                                    text = "FLAT",
                                    onClick = {
                                        val flat = List(10) { 0f }
                                        eqGains = flat
                                        customGains = flat
                                        hasUserEditedEq = true
                                        persistEqState("Flat", flat, flat)
                                        if (isConnected && !isEqPending) {
                                            viewModel.setEqGains(flat)
                                        }
                                    }
                                )
                            }

                            MinimalButton(
                                text = if (isEqPending) "APPLYING..." else "APPLY PROFILE",
                                isLoading = isEqPending,
                                enabled = isConnected && !isEqPending,
                                onClick = {
                                    if (isConnected && !isEqPending) {
                                        persistEqState(activePreset, eqGains, customGains)
                                        viewModel.setEqGains(eqGains)
                                    }
                                },
                                isPrimary = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "10-band studio frequency response (-10.0 dB to +10.0 dB). Tap or drag to customize.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset capsule selector (including "Custom") - Smooth horizontal scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_NAMES.forEach { presetName ->
                            val isSelected = activePreset == presetName
                            val bg = if (isSelected) {
                                if (isDark) PastelBlueBgDark else PastelBlueBg
                            } else {
                                Color.Transparent
                            }
                            val borderCol = if (isSelected) {
                                if (isDark) PastelBlueTextDark else PastelBlueText
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                            val fg = if (isSelected) {
                                if (isDark) PastelBlueTextDark else PastelBlueText
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(bg)
                                    .border(1.dp, borderCol, RoundedCornerShape(9999.dp))
                                    .clickable {
                                        if (presetName == "Custom") {
                                            activePreset = "Custom"
                                            eqGains = customGains
                                            hasUserEditedEq = true
                                            persistEqState("Custom", customGains, customGains)
                                            if (isConnected && !isEqPending) {
                                                viewModel.setEqGains(customGains)
                                            }
                                        } else {
                                            activePreset = presetName
                                            val gains = PRESET_MAP[presetName] ?: List(10) { 0f }
                                            eqGains = gains
                                            hasUserEditedEq = true
                                            persistEqState(presetName, gains, customGains)
                                            if (isConnected && !isEqPending) {
                                                viewModel.setEqGains(gains)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = presetName,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = fg,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 10 Tactile Vertical Faders Rack (Interactive Canvas + Tap & Drag)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        eqGains.forEachIndexed { idx, gain ->
                            MinimalFaderColumn(
                                freq = FREQ_LABELS[idx],
                                value = gain,
                                enabled = true, // Always editable!
                                onValueChange = { newVal ->
                                    val updated = eqGains.toMutableList()
                                    updated[idx] = newVal
                                    eqGains = updated
                                    customGains = updated
                                    activePreset = "Custom"
                                    hasUserEditedEq = true
                                    persistEqState("Custom", updated, updated)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ============================================================
            // 6. EDITORIAL FOOTER & HARDWARE SPECIFICATIONS
            // ============================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOZO CONTROL  •  SYSTEM REV 1.3.8",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "GATT Bluetooth LE Acoustic Controller",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ============================================================
// Utilitarian Minimalist Architectural Components
// ============================================================

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun MinimalBadge(
    text: String,
    isSuccess: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isSuccess) {
        if (isDark) PastelGreenBgDark else PastelGreenBg
    } else {
        if (isDark) PastelRedBgDark else PastelRedBg
    }
    val fg = if (isSuccess) {
        if (isDark) PastelGreenTextDark else PastelGreenText
    } else {
        if (isDark) PastelRedTextDark else PastelRedText
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            color = fg
        )
    }
}

@Composable
fun KeystrokeChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MinimalButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isPrimary: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPrimary) {
        if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isPrimary) {
        if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderStroke = if (isPrimary && enabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(6.dp)) else Modifier)
            .background(bgColor)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(11.dp),
                    strokeWidth = 1.5.dp,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color = if (enabled) textColor else textColor.copy(alpha = 0.6f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun MinimalBatteryPod(
    unitName: String,
    percentage: Int,
    isConnected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isConnected) (percentage.coerceIn(0, 100) / 100f) else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "battery_progress"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = unitName,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Small muted dot
                val dotColor = if (isConnected && percentage > 20) {
                    if (isDark) PastelGreenTextDark else PastelGreenText
                } else if (isConnected) {
                    if (isDark) PastelRedTextDark else PastelRedText
                } else {
                    MaterialTheme.colorScheme.outline
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (isConnected && percentage > 0) "$percentage" else "--",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    letterSpacing = (-0.5).sp,
                    color = if (isConnected && percentage > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isConnected && percentage > 0) {
                    Text(
                        text = "%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Utilitarian linear meter (flat, zero-gradient)
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
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }
    }
}

@Composable
fun MinimalNoiseTile(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
    val borderCol = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    val titleCol = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    val subCol = if (isSelected) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (enabled) titleCol else titleCol.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = if (enabled) subCol else subCol.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
        }
    }
}

@Composable
fun MinimalFaderColumn(
    freq: String,
    value: Float,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val isNonZero = value != 0f

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Value text readout (+3.5, 0.0, -2.0)
        Text(
            text = "${if (value > 0) "+" else ""}${"%.1f".format(value)}",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isNonZero) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isNonZero) onSurfaceColor else onSurfaceVariantColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Interactive Fader Area (full width and height of column is touchable)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val padding = 8f
                        val effectiveHeight = (size.height - padding * 2).coerceAtLeast(1f)
                        val clampedY = (offset.y - padding).coerceIn(0f, effectiveHeight)
                        val frac = 1f - (clampedY / effectiveHeight)
                        val db = (frac * 20f - 10f)
                        val rounded = (db * 2).roundToInt() / 2.0f
                        currentOnValueChange(rounded)
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val padding = 8f
                            val effectiveHeight = (size.height - padding * 2).coerceAtLeast(1f)
                            val clampedY = (offset.y - padding).coerceIn(0f, effectiveHeight)
                            val frac = 1f - (clampedY / effectiveHeight)
                            val db = (frac * 20f - 10f)
                            val rounded = (db * 2).roundToInt() / 2.0f
                            currentOnValueChange(rounded)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val padding = 8f
                            val effectiveHeight = (size.height - padding * 2).coerceAtLeast(1f)
                            val clampedY = (change.position.y - padding).coerceIn(0f, effectiveHeight)
                            val frac = 1f - (clampedY / effectiveHeight)
                            val db = (frac * 20f - 10f)
                            val rounded = (db * 2).roundToInt() / 2.0f
                            currentOnValueChange(rounded)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padding = 8.dp.toPx()
                val effectiveHeight = size.height - padding * 2
                val centerX = size.width / 2f
                val trackWidth = 8.dp.toPx()
                val trackRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                // 1. Background Groove
                drawRoundRect(
                    color = surfaceVariantColor,
                    topLeft = Offset(centerX - trackWidth / 2f, padding),
                    size = Size(trackWidth, effectiveHeight),
                    cornerRadius = trackRadius
                )
                // Groove 1px border
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(centerX - trackWidth / 2f, padding),
                    size = Size(trackWidth, effectiveHeight),
                    cornerRadius = trackRadius,
                    style = Stroke(width = 1.dp.toPx())
                )

                // 2. Center 0 dB guide line
                val centerY = padding + effectiveHeight / 2f
                drawLine(
                    color = outlineColor,
                    start = Offset(centerX - trackWidth / 2f, centerY),
                    end = Offset(centerX + trackWidth / 2f, centerY),
                    strokeWidth = 1.dp.toPx()
                )

                // 3. Normalized level position: value is -10 .. +10
                // 0 = bottom, 1 = top
                val norm = ((value + 10f) / 20f).coerceIn(0f, 1f)
                val thumbY = padding + (1f - norm) * effectiveHeight

                // 4. Level indicator fill from 0 dB center line
                if (value != 0f) {
                    val fillTop = if (thumbY < centerY) thumbY else centerY
                    val fillHeight = kotlin.math.abs(centerY - thumbY)
                    drawRect(
                        color = onSurfaceColor.copy(alpha = if (enabled) 0.85f else 0.4f),
                        topLeft = Offset(centerX - (trackWidth - 2.dp.toPx()) / 2f, fillTop),
                        size = Size(trackWidth - 2.dp.toPx(), fillHeight)
                    )
                }

                // 5. Tactile Thumb Handle (horizontal capsule: width 22dp, height 6dp)
                val thumbWidth = 22.dp.toPx()
                val thumbHeight = 6.dp.toPx()
                val thumbRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

                drawRoundRect(
                    color = onSurfaceColor,
                    topLeft = Offset(centerX - thumbWidth / 2f, thumbY - thumbHeight / 2f),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = thumbRadius
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Frequency Label
        Text(
            text = freq,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = onSurfaceVariantColor
        )
    }
}
