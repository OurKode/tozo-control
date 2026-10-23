# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
