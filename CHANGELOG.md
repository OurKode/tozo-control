# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.5.0] - 2026-10-25
### Added
- Curated 6 Professional Audiophile EQ Curves replacing 20 generic factory genre presets:
  - `Reference Flat`: Pure neutral studio monitor baseline without frequency coloration.
  - `Harman Target`: Calibrated in-ear research target with natural sub-bass shelf and pinna compensation.
  - `Diffuse Field`: DF-neutral curve tailored for analytical listening and spacious soundstage.
  - `Vocal Clarity`: Optimized dialogue intelligibility and acoustic vocal separation without harshness.
  - `Warm Punch`: Analog tube-style warmth with rich sub-bass punch and fatigue-free upper mids.
  - `Custom`: Fully adjustable 10-band hardware registers.
- Dedicated unit test suite `AudiophilePresetTest.kt` validating 10-band hardware bounds and backward-compatibility aliases.
- Dedicated Material 3 Customization & Settings screen (`SettingsScreen`) integrated into Jetpack Navigation 3 (`Settings : NavKey`).
- Adaptive 2-column studio grid layout in `SettingsScreen` for screens >= 640dp (tablets, foldables, landscape).
- Directional shared-axis horizontal slide and fade transitions for mobile tab navigation (`SegmentedModeSelector`).
- Animated selection dot indicator with expand/shrink visibility inside `PresetPill`.
- Dedicated `FrequencyBandPill` with smooth animated color morphing on band switches.
- Comprehensive M3 theme modes: System Default, Light Mode, Studio Dark Mode, and Pure AMOLED (0-nit black canvas with dark card surfaces for OLED battery savings).
- Five curated Material 3 color palettes: Editorial Utilitarian, Oceanic Cobalt, Forest Emerald, Sunset Amber, and Cyber Amethyst.
- Dynamic Color (Material You) toggle switch for Android 12+ (API 31+) with fallback to curated palettes.
- Global Haptic Feedback setting and composition local (`LocalHapticEnabled`) with tactile toggling on 10-band equalizer sliders and steppers.
- Accessible Settings button (`SettingsIconButton`) on top app bar replacing simple theme switcher.
- Unit test coverage for `AppPreferencesRepository`, theme modes, palette presets, and AMOLED transformation.
- Dedicated Monospace telemetry and hardware register tokens (`TelemetryValueStyle`, `TelemetryLabelStyle`) in `Type.kt` ensuring stable, jitter-free slider dB and frequency displays.

### Changed
- Production hardening against errors, edge cases, and accessibility:
  - Guarded Navigation 3 backstack (`backStack.size > 1`) against empty-stack pops on root back gesture.
  - Implemented Bluetooth disabled error recovery: surfaces red status indicator and 1-tap "Enable BT" shortcut to system Bluetooth settings.
  - Replaced fixed container heights on `NoiseModeTile` and `SegmentedModeSelector` with `defaultMinSize` to prevent label clipping under 1.3x–1.5x font scale.
  - Hardened `TouchSlotRow` with full 48dp clickable row touch target.
  - Bound and defensively clamped equalizer drag coordinates and tooltip badge offsets against small viewports.
- Standardized all interactive touch targets to WCAG/Material 3 48x48dp minimum (`SettingsIconButton`, `BackIconButton`, Equalizer steppers, and Prev/Next buttons).
- Added tactile haptic feedback (`performIf`) to tab navigation, preset pills, noise mode tiles, and settings radio items.
- Smoothed battery gauge level transitions with animated bar colors reflecting semantic warnings without flicker.
- Replaced abrupt selection cuts in `SettingsScreen` and `SegmentedModeSelector` with `animateColorAsState` transitions.
- Flattened battery telemetry cards (`EarbudBatteryPod`) removing nested card-in-card borders for a cleaner visual hierarchy.
- Virtualized 21 equalizer preset pill chips into a keyed `LazyRow` with hoisted `PresetPill` composables, eliminating redundant animation recomposition loops.
- Optimized `InteractiveGraphicEq` Canvas rendering to 0 allocations per frame by reusing Path objects (`curvePath`, `fillPath`) and caching pre-measured text layouts (including all 41 dB tooltip badges).
- Polished `SettingsScreen` and `MainScreen` copy per antislop copywriting standards: removed bureaucratic loanwords and artificial phrasing in favor of direct, human Indonesian labels.
- Completed the full Material 3 typography scale in `Type.kt` (`titleSmall`, `bodySmall`, `labelLarge`, etc.) with tuned line-height and letter-spacing.

### Fixed
- Fixed Bluetooth state desync and infinite "Enable BT" settings loop:
  - Added live hardware adapter checks in `MainScreen` and `TopDeviceBar`, preventing stale `"Bluetooth is disabled"` status from locking the button in "Enable BT" state.
  - Implemented automatic Bluetooth state listener (`BroadcastReceiver`) in `TozoBleManager` for `ACTION_STATE_CHANGED` and `ACTION_ACL_CONNECTED` to instantly clear errors and connect upon Bluetooth activation.
  - Added automatic reconnection on `Lifecycle.Event.ON_RESUME` when returning to the app with Bluetooth active.
  - Expanded BLE scan MAC address prefix filters to support all TOZO hardware OUIs (`94:4B:F8`, `00:42`, `E8:07`, `70:C9`).
- Fixed screen-filling layout bug in `SegmentedModeSelector` by bounding the row container to a fixed `height(48.dp)` and assigning `weight(1f)` to `AnimatedContent`, preventing the navigation tabs from consuming the entire screen.
- Fixed `StackOverflowError` crash when pressing tactile Equalizer band and stepper buttons by properly delegating haptic feedback through `HapticFeedback.performIf` without recursive looping.

## [1.4.0] - 2026-10-23
### Added
- Quick Settings Tiles for Android notification shade: `TozoNoiseTileService` (cycling ANC, Transparency, Normal) and `TozoGameModeTileService` (toggle 45 ms low latency).
- On-demand BLE GATT link execution within tile lifecycle, maintaining zero background battery drain.
- Thread-safe singleton accessor `TozoBleManager.getInstance()` synchronized across UI ViewModels and system TileServices.
- Dynamic Color (Material You) theme support for Android 12+ (API 31+) with fallback to Warm Editorial and Studio Dark schemes.
- Material 3 Expressive spring motion physics for tactile 10-band equalizer curve interpolation and preset morphing.
- Smooth animated color state transitions for noise cancellation mode tiles and equalizer preset pills.
- Comprehensive design specification in DESIGN.md detailing identity, typography, telemetry colors, and motion dials.

### Fixed
- Fixed Quick Settings Tiles interaction when app is killed or removed from Recents by avoiding `STATE_UNAVAILABLE` and executing queued mode commands on connection.
- Fixed unnecessary app reconnection by preserving cached MAC address across disconnect events and prioritizing verified GATT MAC over bonded audio MACs.
- Fixed Quick Tile label to directly display active noise profile ("ANC", "Pass", or "Normal") with responsive connection subtitle.
- Removed premature BLE GATT teardown from ViewModel `onCleared()` to allow background Quick Tiles to operate seamlessly.
- Fixed TWS state desync when reopening app from Recents by handling `onDescriptorWrite` callback before sending queries and refreshing telemetry on app resume.
- Shortened device sub-header to clean MAC display and simplified Equalizer, Noise Control, and Game Mode subtitles per antislop copywriting standards.
- Redesigned Equalizer controls into prominent tactile buttons: 10-band interactive button strip, tactile stepper buttons (-0.5 dB, 0 dB, +0.5 dB), slider scrubber, Prev/Next navigation, and elevated preset pill chips.


## [1.3.0] - 2026-10-16
### Added
- Expanded Studio Equalizer preset collection from 5 to 20 authentic TOZO hardware curves extracted from `DeviceValueContainer`:
  - Added Pop, Rock, Classical, Jazz, Vocal, Electronic, Hip-Hop, R&B, Dance, Deep, Blues, Metal, Piano, Podcast, Bass -, Treble -, and preserved Flat, Bass +, Treble +, and Acoustic.
  - Full backward compatibility alias preservation for previously stored custom profile names.

### Added
- Reverse-engineered TOZO AeroSound 3 (`CA0D` / JieLi SoC) command protocol integration:
  - Touch gesture remapping for Left and Right earbuds (Single, Double, Triple, Long Press with 9 configurable actions).
  - Direct hardware DSP EQ Reset command (`0x100C`) and Earbud Power Off (`0x1010`) with confirmation safety dialog.
  - Autonomous event synchronization for battery (`0x200E`), ANC (`0x2011`), transparency (`0x2010`), wind (`0x2013`), leisure (`0x2014`), game mode (`0x200F`), and DSP EQ (`0x2012`).
- Unit test suite verifying packet framing, checksum calculation, and touch serialization.

### Changed
- Updated GATT Service UUID to authentic JieLi profile `0000b610-0000-1000-8000-00805f9b34fb`, TX `0xB611`, and RX `0xB612`.
- Enhanced BLE scan matching to recognize TOZO AeroSound manufacturer data frames (`WR`/`WOER` marker).

### Fixed
- Fixed noise cancellation mode persistence across reconnects: added `SET_DSP_BOOT_UP_ANC_MODE` (`0x1011`) for hardware NVRAM storage (`isRememberType`) and migrated reconnect query to 5-mode status register `GET_DEVICE_ANC_MODE` (`0x0028`).

## [1.1.1] - 2026-09-27
### Changed
- Replaced 10-band vertical fader rack with an interactive graphic equalizer spline curve.
- Eliminated horizontal scroll and overflow, ensuring all 10 frequency bands are visible across all phone viewport widths.
- Added companion precision fine-tuning control strip with band navigation and step buttons.

## [1.1.0] - 2026-09-27
### Added
- Segmented mode navigation separating quick controls from the studio equalizer on phone screens.
- Adaptive 2-column studio layout for tablets, foldables, and landscape orientations.
- Tactile haptic feedback when adjusting 10-band equalizer faders.
- Dynamic package version display in the application footer.
- Automated unit testing and optimized release APK artifact generation in GitHub Actions workflow.

### Changed
- Replaced desktop keyboard indicators with mobile-native touch controls and accessible tap bounds.
- Restructured noise isolation profiles into a 48dp touch-target grid with readable typography.
- Upgraded typography scale to clean system sans-serif with accessible type sizes (WCAG AA).
- Synchronized versioning documentation across CONTRIBUTING.md and VERSIONING.md.

### Performance
- Enabled R8 full-mode bytecode shrinking and resource shrinking, reducing release APK size from 11.5 MB to 1.02 MB.

## [1.0.0] - 2026-09-23
### Added
- Native Jetpack Compose UI with dark and light theme support.
- Reverse-engineered BLE 5.3 protocol communication with TOZO earbud hardware.
- Real-time battery telemetry readout for left and right earbuds.
- Active Noise Cancellation (ANC) mode switching: Normal, ANC, Transparency, Leisure, and Wind Reduction.
- Low-latency Game Mode toggle with rate-limited hardware transmission.
- 10-band hardware DSP Equalizer (-10.0 dB to +10.0 dB) with hardware synchronization, built-in presets (Flat, Bass Boost, Treble Boost, Vocal Focus, Acoustic), and custom curve persistence.
- Vector adaptive icon benchmarked on minimalist TOZO branding.
- GitHub Actions CI/CD workflow for automated compilation and release artifact generation on tags.
