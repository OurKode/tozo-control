# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-09-23
### Added
- Native Jetpack Compose Utilitarian UI with dark/light mode toggle.
- Reverse-engineered BLE 5.3 protocol communication with TOZO earbud hardware.
- Real-time battery telemetry readout (Left and Right earbud percentages).
- Active Noise Cancellation (ANC) mode switching: Normal, ANC, Transparency, Leisure, and Wind Reduction.
- Low-latency Game Mode toggle with rate-limited hardware transmission.
- 10-band hardware DSP Equalizer (-10 dB to +10 dB) with live hardware synchronization, built-in presets (Flat, Bass Boost, Treble Boost, Vocal Focus, Acoustic), and custom curve persistence.
- Original vector adaptive icon benchmarked on minimalist TOZO branding.
- GitHub Actions CI/CD workflow for automated compilation and release artifact generation on tags.
