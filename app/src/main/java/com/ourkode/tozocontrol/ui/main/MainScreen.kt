package com.ourkode.tozocontrol.ui.main

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.ourkode.tozocontrol.theme.*
import kotlin.math.roundToInt

// ============================================================
// Equalizer Constants & Sound Profiles
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

private enum class MobileScreenTab(val label: String) {
    CONTROLS("Controls"),
    EQUALIZER("Equalizer")
}

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
                statusText = statusText,
                isDark = isDark,
                onToggleTheme = onToggleTheme,
                onConnectClick = {
                    if (isConnected || isConnecting) {
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
                                activePreset = "Flat"
                                persistEqState("Flat", flat, flat)
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
                            fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                        },
                        label = "tab_content",
                        modifier = Modifier.fillMaxSize()
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
                                            activePreset = "Flat"
                                            persistEqState("Flat", flat, flat)
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
    statusText: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
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
            Column(modifier = Modifier.weight(1f)) {
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
                            isConnected -> "BLE 5.3 Linked  •  $macAddress"
                            isConnecting -> if (statusText.isNotBlank()) statusText else "Connecting..."
                            else -> "Disconnected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Accessible Theme Switcher (48x48dp touch target)
                ThemeIconButton(
                    isDark = isDark,
                    onClick = onToggleTheme
                )

                // Primary Connection Button (Min height 44dp, touch target 48dp)
                Button(
                    onClick = onConnectClick,
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        contentColor = if (isConnected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
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
                            text = if (isConnected) "Disconnect" else "Connect",
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
private fun ThemeIconButton(
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val strokeColor = if (isDark) Color(0xFFF4F4F2) else Color(0xFF111111)
            val center = Offset(size.width / 2f, size.height / 2f)

            if (isDark) {
                // Sun Icon (tap to switch to light)
                val radius = 5.dp.toPx()
                drawCircle(color = strokeColor, radius = radius, center = center)

                val rayLen = 2.5.dp.toPx()
                val rayStart = radius + 2.dp.toPx()
                val rayEnd = rayStart + rayLen

                for (i in 0 until 8) {
                    val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                    val cosA = kotlin.math.cos(angle)
                    val sinA = kotlin.math.sin(angle)
                    drawLine(
                        color = strokeColor,
                        start = Offset(center.x + cosA * rayStart, center.y + sinA * rayStart),
                        end = Offset(center.x + cosA * rayEnd, center.y + sinA * rayEnd),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            } else {
                // Crescent Moon Icon (tap to switch to dark)
                val path = Path().apply {
                    moveTo(size.width * 0.75f, size.height * 0.15f)
                    cubicTo(
                        size.width * 0.25f, size.height * 0.25f,
                        size.width * 0.25f, size.height * 0.75f,
                        size.width * 0.75f, size.height * 0.85f
                    )
                    cubicTo(
                        size.width * 0.45f, size.height * 0.75f,
                        size.width * 0.45f, size.height * 0.25f,
                        size.width * 0.75f, size.height * 0.15f
                    )
                    close()
                }
                drawPath(path = path, color = strokeColor)
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
            val bg = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
            val textColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .clickable { onTabSelected(tab) },
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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

            // Gauge bar with semantic battery warning
            val barColor = when {
                !isConnected -> MaterialTheme.colorScheme.outline
                percentage <= 20 -> StatusRed
                percentage <= 35 -> StatusAmber
                else -> StatusGreen
            }

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
                            .background(barColor)
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
            text = "Hardware microphone phase cancellation profiles",
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
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val titleCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val subCol = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
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
                        "45ms synchronous buffer engaged for gaming and video"
                    } else {
                        "Standard transmission buffer engaged for baseline battery life"
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
                    text = "10-Band DSP hardware filters (-10.0 to +10.0 dB)",
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

        // Preset Pills (Horizontal Scroll with generous 44dp touch area)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PRESET_NAMES.forEach { presetName ->
                val isSelected = activePreset == presetName
                val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                        .clickable { onSelectPreset(presetName) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
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

        Spacer(modifier = Modifier.height(18.dp))

        // 10 Tactile Vertical Faders Rack
        // Horizontal scroll container with min 48dp width per column prevents finger collision on narrow screens
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val totalWidth = maxWidth
            val columnWidth = (totalWidth / 10).coerceAtLeast(46.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                eqGains.forEachIndexed { idx, gain ->
                    TactileFaderColumn(
                        freq = FREQ_LABELS[idx],
                        value = gain,
                        enabled = true,
                        onValueChange = { onGainChange(idx, it) },
                        modifier = Modifier.width(columnWidth)
                    )
                }
            }
        }
    }
}

@Composable
private fun TactileFaderColumn(
    freq: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val isNonZero = kotlin.math.abs(value) >= 0.2f

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain Readout (+4.5, 0.0, -2.0)
        Text(
            text = "${if (value > 0) "+" else ""}${"%.1f".format(value)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isNonZero) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isNonZero) onSurfaceColor else onSurfaceVariantColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Tactile Fader Track with full column touchable area & haptics
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
                        if (rounded != value) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnValueChange(rounded)
                        }
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
                            if (rounded != value) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnValueChange(rounded)
                            }
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val padding = 8f
                            val effectiveHeight = (size.height - padding * 2).coerceAtLeast(1f)
                            val clampedY = (change.position.y - padding).coerceIn(0f, effectiveHeight)
                            val frac = 1f - (clampedY / effectiveHeight)
                            val db = (frac * 20f - 10f)
                            val rounded = (db * 2).roundToInt() / 2.0f
                            if (rounded != value) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnValueChange(rounded)
                            }
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

                // 1. Groove track
                drawRoundRect(
                    color = surfaceVariantColor,
                    topLeft = Offset(centerX - trackWidth / 2f, padding),
                    size = Size(trackWidth, effectiveHeight),
                    cornerRadius = trackRadius
                )
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
                    start = Offset(centerX - trackWidth / 2f - 3.dp.toPx(), centerY),
                    end = Offset(centerX + trackWidth / 2f + 3.dp.toPx(), centerY),
                    strokeWidth = 1.dp.toPx()
                )

                // 3. Normalized level position (-10 to +10 dB)
                val norm = ((value + 10f) / 20f).coerceIn(0f, 1f)
                val thumbY = padding + (1f - norm) * effectiveHeight

                // 4. Level indicator fill from 0 dB center line
                if (isNonZero) {
                    val fillTop = if (thumbY < centerY) thumbY else centerY
                    val fillHeight = kotlin.math.abs(centerY - thumbY)
                    drawRect(
                        color = accentColor.copy(alpha = if (enabled) 0.85f else 0.4f),
                        topLeft = Offset(centerX - (trackWidth - 2.dp.toPx()) / 2f, fillTop),
                        size = Size(trackWidth - 2.dp.toPx(), fillHeight)
                    )
                }

                // 5. Tactile Thumb Handle (capsule)
                val thumbWidth = 24.dp.toPx()
                val thumbHeight = 7.dp.toPx()
                val thumbRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())

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
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = onSurfaceVariantColor
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
