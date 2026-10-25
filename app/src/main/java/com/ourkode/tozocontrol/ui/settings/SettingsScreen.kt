package com.ourkode.tozocontrol.ui.settings

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.ourkode.tozocontrol.theme.LocalHapticEnabled
import com.ourkode.tozocontrol.theme.performIf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourkode.tozocontrol.data.AppThemeMode
import com.ourkode.tozocontrol.data.ColorPalette

@Composable
fun SettingsScreen(
    currentThemeMode: AppThemeMode,
    isDynamicColor: Boolean,
    currentPalette: ColorPalette,
    isHapticEnabled: Boolean,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onPaletteChange: (ColorPalette) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ============================================================
        // 1. TOP BAR WITH ACCESSIBLE BACK BUTTON
        // ============================================================
        SettingsTopBar(onBackClick = onBackClick)

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 1.dp
        )

        // ============================================================
        // 2. ADAPTIVE RESPONSIVE SETTINGS CONTENT (TABLET / PHONE)
        // ============================================================
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            val isWideScreen = maxWidth >= 640.dp

            if (isWideScreen) {
                // Tablet & Landscape Foldable: 2-Column Balanced Studio Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ThemeModeSection(
                            currentMode = currentThemeMode,
                            onModeSelected = onThemeModeChange
                        )
                        DynamicColorSection(
                            isDynamicColor = isDynamicColor,
                            onToggle = onDynamicColorChange
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ColorPaletteSection(
                            selectedPalette = currentPalette,
                            enabled = !isDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                            onPaletteSelected = onPaletteChange
                        )
                        HapticFeedbackSection(
                            isHapticEnabled = isHapticEnabled,
                            onToggle = onHapticChange
                        )
                        AboutAppSection()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                // Phone Layout: Focused Vertical Stack with 560dp Max Measure
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ThemeModeSection(
                        currentMode = currentThemeMode,
                        onModeSelected = onThemeModeChange
                    )
                    DynamicColorSection(
                        isDynamicColor = isDynamicColor,
                        onToggle = onDynamicColorChange
                    )
                    ColorPaletteSection(
                        selectedPalette = currentPalette,
                        enabled = !isDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                        onPaletteSelected = onPaletteChange
                    )
                    HapticFeedbackSection(
                        isHapticEnabled = isHapticEnabled,
                        onToggle = onHapticChange
                    )
                    AboutAppSection()
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BackIconButton(onClick = onBackClick)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pengaturan Tampilan",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tema, warna aksen, dan getaran sentuhan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BackIconButton(
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
            val strokeWidth = 2.dp.toPx()
            drawLine(
                color = strokeColor,
                start = Offset(size.width * 0.8f, size.height * 0.5f),
                end = Offset(size.width * 0.2f, size.height * 0.5f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor,
                start = Offset(size.width * 0.45f, size.height * 0.25f),
                end = Offset(size.width * 0.2f, size.height * 0.5f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor,
                start = Offset(size.width * 0.45f, size.height * 0.75f),
                end = Offset(size.width * 0.2f, size.height * 0.5f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ThemeModeItem(
    mode: AppThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalHapticEnabled.current

    val bg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "theme_mode_bg_${mode.name}"
    )
    val borderCol by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "theme_mode_border_${mode.name}"
    )

    val description = when (mode) {
        AppThemeMode.SYSTEM -> "Mengikuti tema perangkat Android"
        AppThemeMode.LIGHT -> "Latar terang dengan teks kontras tinggi"
        AppThemeMode.DARK -> "Latar gelap abu-abu studio"
        AppThemeMode.AMOLED -> "Hitam pekat (#000000) untuk hemat daya layar OLED"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderCol,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable {
                haptic.performIf(hapticEnabled, HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = {
                haptic.performIf(hapticEnabled, HapticFeedbackType.LongPress)
                onClick()
            },
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeModeSection(
    currentMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit
) {
    SectionCard(title = "Mode Tema", subtitle = "Pilih tampilan terang, gelap, atau hemat baterai") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                ThemeModeItem(
                    mode = mode,
                    isSelected = currentMode == mode,
                    onClick = { onModeSelected(mode) }
                )
            }
        }
    }
}

@Composable
private fun DynamicColorSection(
    isDynamicColor: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    SectionCard(
        title = "Warna Dinamis (Material You)",
        subtitle = "Menyesuaikan aksen aplikasi dengan wallpaper sistem"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = if (isSupported) "Gunakan Warna Wallpaper" else "Perlu Android 12+",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isSupported) {
                        "Mengambil palet warna dari wallpaper beranda (Android 12+)."
                    } else {
                        "Fitur ini memerlukan perangkat dengan Android 12 (API 31) ke atas."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = isDynamicColor && isSupported,
                onCheckedChange = { if (isSupported) onToggle(it) },
                enabled = isSupported
            )
        }
    }
}

@Composable
private fun ColorPaletteItem(
    palette: ColorPalette,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalHapticEnabled.current
    val previewColor = Color(palette.previewColor)

    val bg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "palette_bg_${palette.name}"
    )
    val borderCol by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "palette_border_${palette.name}"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderCol,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) {
                haptic.performIf(hapticEnabled, HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(previewColor)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = palette.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(animationSpec = tween(150)) + expandHorizontally(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally(animationSpec = tween(100))
        ) {
            Text(
                text = "Aktif",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ColorPaletteSection(
    selectedPalette: ColorPalette,
    enabled: Boolean,
    onPaletteSelected: (ColorPalette) -> Unit
) {
    SectionCard(
        title = "Palet Warna Manual",
        subtitle = if (enabled) {
            "Pilih aksen warna saat Warna Dinamis dimatikan"
        } else {
            "Matikan Warna Dinamis di atas untuk memilih palet ini"
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ColorPalette.entries.forEach { palette ->
                ColorPaletteItem(
                    palette = palette,
                    isSelected = selectedPalette == palette && enabled,
                    enabled = enabled,
                    onClick = { onPaletteSelected(palette) }
                )
            }
        }
    }
}

@Composable
private fun HapticFeedbackSection(
    isHapticEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SectionCard(
        title = "Getaran Sentuhan (Haptic)",
        subtitle = "Respons getaran saat menyentuh slider dan tombol"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = "Aktifkan Getaran",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Memberikan getaran taktil saat mengatur frekuensi Equalizer dan memilih preset.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = isHapticEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun AboutAppSection() {
    SectionCard(
        title = "Tentang Aplikasi",
        subtitle = "Kontrol langsung hardware earbud tanpa pelacak"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Versi Aplikasi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "v1.4.0 (AeroSound 3)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pelacak & Iklan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Nol / Tanpa Pelacak",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Komunikasi BLE",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Register GATT Langsung (Lokal)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            content()
        }
    }
}
