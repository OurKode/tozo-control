# TOZO Control (Unofficial)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material3-blue.svg)](https://developer.android.com/jetpack/compose)

A lightweight, bloat-free, open-source Android companion client for **TOZO AeroSound 3** (and compatible TOZO Bluetooth LE earbuds). 

Unlike official companion applications, **TOZO Control** has **zero ads**, **zero tracking or telemetry**, **zero background battery drain**, and connects directly to the earbud's hardware DSP registers using Bluetooth Low Energy (GATT).

> [!IMPORTANT]
> **Legal Disclaimer**: This is an independent, community-driven open-source project and is **not** affiliated with, authorized, maintained, sponsored, or endorsed by TOZO Inc. or any of its affiliates. "TOZO" is a registered trademark of its respective owner.

---

## Features

- **Direct Hardware Querying**: Reads actual battery percentages (L & R), firmware version, active noise cancellation mode, game mode, and current 10-band DSP curve directly from the earbud registers upon connection.
- **10-Band Studio DSP Equalizer**: Real-time tactile faders (-10.0 dB to +10.0 dB) with presets (*Flat, Bass Boost, Treble Boost, Vocal Focus, Acoustic*) and fully custom parametric curve persistence.
- **Noise Isolation Profiles**: Toggle between **ANC (Shield)**, **Pass (Transparency)**, **Leisure (Soft Cut)**, **Wind Reduction**, and **Normal (Direct)**.
- **Low-Latency Game Mode**: One-tap toggle for hardware synchronous transmission buffer.
- **Modern Minimalist UI**: Built entirely in Jetpack Compose with Material 3, supporting persistent Dark/Light modes and Android 13+ Themed Adaptive Icons.
- **Dynamic BLE Connection**: Automatic discovery and seamless reconnection to bonded or nearby TOZO LE earbuds.
- **Flooding Protection**: Integrated cooldown muting prevents BLE packet jamming and DSP freeze on rapid state toggling.

---

## Reverse-Engineered BLE Protocol Specification

Communication with TOZO earbuds occurs over standard Bluetooth Low Energy GATT characteristics.

### GATT UUIDs

| Descriptor / Characteristic | UUID |
| :--- | :--- |
| **Service UUID** | `00000001-0000-1000-8000-00805f9b34fb` |
| **TX (Write Without Response)** | `00000003-0000-1000-8000-00805f9b34fb` |
| **RX (Notify Characteristic)** | `00000002-0000-1000-8000-00805f9b34fb` |
| **Client Characteristic Config (CCCD)** | `00002902-0000-1000-8000-00805f9b34fb` |

---

### Command Protocol Table

#### 1. Hardware State Queries (App $\rightarrow$ Earbud)

| Parameter | Query Command (Hex) | Expected Notification Response (Hex) | Description |
| :--- | :--- | :--- | :--- |
| **Firmware Version** | `00010000` | `000106 [00 00 L] [00 00 R] [chk]` | Firmware revision (e.g. `v0.0.6`) |
| **Battery Percentage** | `00020000` | `000202 [Left] [Right] [chk]` | Battery percentage (0–100%) |
| **Autonomous Telemetry** | *(autonomous)* | `200e02 [Left] [Right] [chk]` | Autonomous battery event emitted on link |
| **Game Mode Status** | `00060000` | `000601 [00 / 01] [chk]` | `01` = Active, `00` = Disabled |
| **Noise Mode Status** | `00040000` | `000401 [00 / 01] [chk]` | `01` = ANC, `00` = Normal |
| **Equalizer DSP Status** | `000b0000` | `000b14 [10 gain bytes] [10 Qs]` | 10 frequency band gains in tenths of dB |

#### 2. Control Commands (App $\rightarrow$ Earbud)

| Control | Command (Hex) | Notes |
| :--- | :--- | :--- |
| **Game Mode ON** | `1006010101` | Engages low-latency synchronous buffer |
| **Game Mode OFF** | `1006010000` | Baseline transmission buffer |
| **Normal Mode** | `1004010000` | Microphone phase cancellation off |
| **ANC Mode** | `1004010101` | Active Noise Cancellation |
| **Transparency (Pass)** | `1005010101` | Ambient microphone pass-through |
| **Wind Reduction** | `1007010101` | Anti-wind turbulence filter |
| **Leisure Mode** | `1008010101` | Soft ambient cut |

---

### Equalizer 24-Byte Payload Anatomy

When applying a custom EQ curve or preset, a 24-byte packet is constructed and sent to Characteristic `0003`:

```
[0x10] [0x0B] [0x15] [10 Gain Bytes] [10 Q-Factor Bytes] [0x01] [Checksum]
```

1. **Header**: `0x10, 0x0B, 0x15` (Command Opcode `0x100B`, length `21 bytes`).
2. **Gain Array (10 bytes)**: Frequencies: `31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz`.
   - Formula: $\text{raw} = \text{round}(\text{gain\_dB} \times 10)$
   - Range: $-10.0\text{ dB}$ (`0x9C` / $-100$) to $+10.0\text{ dB}$ (`0x64` / $+100$).
3. **Q-Factor Array (10 bytes)**: Default filter widths: `[0x05, 0x06, 0x07, 0x08, 0x0A, 0x0C, 0x0E, 0x00, 0x00, 0x00]`.
4. **Tail**: Fixed value `0x01`.
5. **Checksum (1 byte)**: Sum of all 21 payload bytes modulo 256 ($\sum \text{payload} \pmod{256}$).

---

## Building from Source

### Prerequisites
- JDK 21 or higher
- Android SDK with Platform 36 (Android 15) and Build-Tools `36.0.0`
- Gradle 8.12+ (or use included `./gradlew` wrapper)

### Build Debug APK
```bash
git clone https://github.com/ourkode/tozo-control.git
cd tozo-control
./gradlew assembleDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Install to Device via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Contributing & Versioning

Contributions are welcome! If you have another model of TOZO earbuds (such as NC9, T10, T12, T6, Golden X1, Open Buds):
1. Capture Bluetooth HCI snoop logs or notify characteristic logs using `adb logcat -s TozoBleManager`.
2. Map model-specific opcodes or characteristic variations.
3. Submit an issue or pull request!

Please review [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines, commit conventions, and semantic versioning policies. See [CHANGELOG.md](CHANGELOG.md) for release history.

---

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for the full license text.
