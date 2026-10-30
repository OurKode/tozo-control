# TOZO Control Design System

## 1. Identity & Personality
- **Core Character**: Studio Audio Console, utilitarian, hardware-focused, clean and distraction-free.
- **Design Philosophy**: Direct hardware control with honest feedback. No decorative fluff, no simulated charts without real telemetry, and zero unneeded animations.

## 2. Color System
- **Dynamic Color (Material You, Android 12+ / API 31+)**:
  - Dynamically extracts primary, secondary, tertiary, and tonal surface containers from system wallpaper.
  - Honors user system personalization while maintaining high-contrast audio indicators.
- **Utilitarian Editorial Fallback (Pre-Android 12 or Manual Preference)**:
  - **Light (Warm Editorial)**:
    - Background / Canvas: `#FBF9F5` (Warm Canvas)
    - Surface / Cards: `#FFFFFF` (Warm White)
    - Surface Subtle: `#F4F0E8`
    - Outline / Borders: `#E2DDD3`
    - Text Primary: `#1C1917` (Stone 900)
    - Text Secondary: `#78716C` (Stone 500)
    - Accent: `#2563EB` (Pastel Blue)
  - **Dark (Studio Dark)**:
    - Background / Canvas: `#0E0F12` (Studio Deep)
    - Surface / Cards: `#16181D` (Studio Charcoal)
    - Surface Subtle: `#1E222A`
    - Outline / Borders: `#2D323E`
    - Text Primary: `#F3F4F6` (Gray 100)
    - Text Secondary: `#9CA3AF` (Gray 400)
    - Accent: `#60A5FA` (Blue 400)
  - **Telemetry Status Colors**:
    - Linked: `#22C55E` (Emerald Green)
    - Syncing / Pending: `#F59E0B` (Amber)
    - Disconnected: `#6B7280` (Muted Slate)

## 3. Typography
- **Primary Font**: Clean System Sans-Serif (Google Sans / Roboto) for all UI hierarchy, headings, and labels.
- **Telemetry & Hardware Registers**: Dedicated Monospace tabular tokens (TelemetryValueStyle, TelemetryLabelStyle) for dB gain indicators (+3.5 dB) and frequency labels (31 Hz, 16 kHz) to eliminate horizontal jitter when dragging sliders.
- **Material 3 Type Scale**:
  - displayLarge: 24sp Bold, line-height 30sp, tracking -0.4sp (Hero metrics)
  - headlineMedium: 20sp SemiBold, line-height 26sp, tracking -0.2sp (Primary titles)
  - titleLarge: 18sp SemiBold, line-height 24sp, tracking -0.1sp (Top bar device title)
  - titleMedium: 15sp SemiBold, line-height 20sp, tracking 0sp (Card section headers)
  - titleSmall: 14sp SemiBold, line-height 18sp, tracking 0sp (Nested group headers)
  - bodyLarge: 15sp Regular, line-height 22sp (Readable dialog descriptions)
  - bodyMedium: 13sp Regular, line-height 18sp (Card body copy & interactive options)
  - bodySmall: 12sp Regular, line-height 16sp, tracking 0.1sp (Helper text & subtitles)
  - labelLarge: 14sp SemiBold, line-height 20sp (Primary button labels)
  - labelMedium: 12sp Medium, line-height 16sp (Filter pills & tab navigation)
  - labelSmall: 11sp Medium, line-height 15sp (Badges & status metadata)

## 4. Motion & Expressive Transitions (Material 3)
- **Fader Curve Interpolation**:
  - 10-Band EQ nodes and spline interpolation animate smoothly when presets change or reset to Flat.
  - Motion Spec: `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)` for fluid, tactile response without overshoot.
- **Tab & Mode Transitions**:
  - Fade-through transition (`tween(200ms)`) between Controls and Equalizer panels.
  - Pill selection color morphing via `animateColorAsState` for active presets and noise profiles.

## 5. Dials
- **ENERGY**: 2 (Balanced: clean utilitarian hardware look, elevated by Material You tones)
- **RHYTHM**: 2 (Consistent card structures with specialized studio canvas for EQ)
- **MOTION**: 2 (Tactile spring faders and smooth theme transitions; no distracting decorative loops)
