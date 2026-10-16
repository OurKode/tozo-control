# TOZO Control (Unofficial)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material3-blue.svg)](https://developer.android.com/jetpack/compose)

An open-source Android companion app for TOZO AeroSound 3 wireless earbuds. Communicates directly with earbud hardware registers over Bluetooth Low Energy (GATT) without analytics, ads, or background battery drain.

> [!IMPORTANT]
> Legal Disclaimer: This is an independent, community-driven open-source project and is not affiliated with, authorized, maintained, sponsored, or endorsed by TOZO Inc. or any of its affiliates. "TOZO" is a registered trademark of its respective owner.

---

## Capabilities

- Hardware telemetry: Reads left and right earbud battery levels, firmware revision, active noise cancellation state, and current 10-band DSP curve upon connection.
- 10-Band studio equalizer: Adjust gain (-10.0 dB to +10.0 dB) across 10 bands with tactile faders and haptic feedback. Includes presets (Flat, Bass Boost, Treble Boost, Vocal Focus, Acoustic) and custom curve persistence.
- Noise isolation profiles: Switch between Active Noise Cancellation (ANC), Transparency, Leisure, Wind Reduction, and Normal modes.
- Low-latency mode: Toggles the 45 ms synchronous transmission buffer for gaming and video playback.
- Adaptive interface: Automatically reflows between a thumb-friendly tabbed layout on phones and a 2-column studio console on tablets and landscape screens. Supports dark and light themes.
- Lightweight footprint: Optimized release APK under 1.1 MB via R8 full-mode bytecode and resource shrinking.
- Command rate limiting: Cooldown buffers prevent packet congestion and DSP register freeze during rapid toggling.

---

## Reverse-Engineered BLE Protocol Specification

Communication with TOZO earbuds occurs over standard Bluetooth Low Energy GATT characteristics.

### GATT UUIDs

| Descriptor / Characteristic | UUID |
| :--- | :--- |
| Service UUID | `0000b610-0000-1000-8000-00805f9b34fb` |
| TX (Write Without Response) | `0000b611-0000-1000-8000-00805f9b34fb` |
| RX (Notify Characteristic) | `0000b612-0000-1000-8000-00805f9b34fb` |
| Client Characteristic Config (CCCD) | `00002902-0000-1000-8000-00805f9b34fb` |

---

### Command Protocol Table

#### 1. Hardware State Queries (App -> Earbud)

| Parameter | Query Command (Hex) | Expected Notification Response (Hex) | Description |
| :--- | :--- | :--- | :--- |
| Firmware Version | `00010000` | `000106 [00 00 L] [00 00 R] [chk]` | Firmware revision (for example, `v0.0.6`) |
| Battery Percentage | `00020000` | `000202 [Left] [Right] [chk]` | Battery percentage (0% to 100%) |
| Autonomous Telemetry | *(autonomous)* | `200e02 [Left] [Right] [chk]` | Autonomous battery event emitted on link |
| Game Mode Status | `00060000` | `000601 [00 / 01] [chk]` | `01` = Active, `00` = Disabled |
| Noise Mode Status | `00040000` | `000401 [00 / 01] [chk]` | `01` = ANC, `00` = Normal |
| Equalizer DSP Status | `000b0000` | `000b14 [10 gain bytes] [10 Qs]` | 10 frequency band gains in tenths of dB |
| Touch Controls | `00030000` | `00030a [5B L] [5B R] [chk]` | 10-byte touch config for Left & Right |

#### 2. Control Commands (App -> Earbud)

| Control | Command (Hex) | Description |
| :--- | :--- | :--- |
| Game Mode ON / OFF | `100601 [01/00] [chk]` | Engages / restores low-latency buffer |
| Normal Mode | `1004010000` | Microphone phase cancellation off |
| ANC Mode | `1004010101` | Active Noise Cancellation |
| Transparency (Pass) | `1005010101` | Ambient microphone pass-through |
| Wind Reduction | `1007010101` | Anti-wind turbulence filter |
| Leisure Mode | `1008010101` | Soft ambient cut |
| Set Touch Controls | `10030a [5B L] [5B R] [chk]` | Remap tap, double, triple, long press |
| Reset DSP Equalizer | `100c0000` | Reverts DSP hardware EQ registers |
| Power Off Earbuds | `1010010000` | Direct power-down command |

---

### Equalizer 24-Byte Payload Anatomy

When applying a custom EQ curve or preset, a 24-byte packet is constructed and sent to characteristic `0003`:

```
[0x10] [0x0B] [0x15] [10 Gain Bytes] [10 Q-Factor Bytes] [0x01] [Checksum]
```

1. Header: `0x10, 0x0B, 0x15` (Command Opcode `0x100B`, length 21 bytes).
2. Gain Array (10 bytes): Frequencies: 31 Hz, 62 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, 16 kHz.
   - Formula: `raw = round(gain_dB * 10)`
   - Range: `-10.0 dB` (`0x9C` / `-100`) to `+10.0 dB` (`0x64` / `+100`).
3. Q-Factor Array (10 bytes): Default filter widths: `[0x05, 0x06, 0x07, 0x08, 0x0A, 0x0C, 0x0E, 0x00, 0x00, 0x00]`.
4. Tail: Fixed byte `0x01`.
5. Checksum (1 byte): Sum of all 21 payload bytes modulo 256 (`sum(payload) % 256`).

---

## Building from Source

### Prerequisites
- JDK 21 or higher
- Android SDK with Platform 36 (Android 15) and Build-Tools `36.0.0`
- Gradle 8.12+ (or use the included `./gradlew` wrapper)

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Build Optimized Release APK (1.02 MB)
```bash
./gradlew assembleRelease
```
The output APK is generated at:
`app/build/outputs/apk/release/TOZO-Control-v1.1.0-AeroSound3-release.apk`

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Install to Device via ADB
```bash
adb install -r app/build/outputs/apk/release/TOZO-Control-v1.1.0-AeroSound3-release.apk
```

---

## Contributing & Versioning

To contribute support for additional TOZO earbud models (such as NC9, T10, T12, Golden X1, or Open Buds):
1. Capture Bluetooth HCI snoop logs or notify characteristic events using `adb logcat -s TozoBleManager`.
2. Map model-specific opcodes or characteristic UUID variations.
3. Submit an issue or pull request.

Review [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines, commit conventions, and versioning rules. See [CHANGELOG.md](CHANGELOG.md) for release history.

---

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0). See [LICENSE](LICENSE) for the full license text.
