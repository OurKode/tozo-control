# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
