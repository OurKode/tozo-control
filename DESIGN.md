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
- **Primary Font**: Clean System Sans-Serif (Roboto / Google Sans) with high legibility.
- **Telemetry & Audio Values**: Tabular numerals for gain dB indicators (`+3.5 dB`) and frequency labels (`31 Hz`, `16 kHz`) to prevent visual jitter when dragging sliders.
- **Type Scale**:
  - Headline: 20sp SemiBold (Device header)
  - Section Title: 16sp SemiBold (Studio Equalizer, Noise Isolation)
  - Body / Subtitle: 13sp-14sp Regular / Medium
  - Labels & Chips: 11sp-12sp Medium / Bold

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
