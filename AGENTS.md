# Project: TOZO Control (Android)

Companion app open-source untuk earbud TOZO (khususnya AeroSound 3). Berkomunikasi langsung dengan hardware registers earbud via Bluetooth Low Energy (GATT) tanpa analytics, iklan, atau background tracking.

## Tech Stack
- **Language**: Kotlin 2.0 (JVM Target 21)
- **UI Framework**: Jetpack Compose + Material 3 (Adaptive phone/tablet layout)
- **Architecture**: MVVM with unidirectional data flow (`StateFlow`, Kotlin Coroutines)
- **Android Platform**: Min SDK 26 (Android 8.0), Target/Compile SDK 36 (Android 15)
- **Connectivity**: Android BLE GATT (`BluetoothGatt`, `BluetoothGattCallback`, Mutex-guarded commands)
- **Build System**: Gradle 8.12+ (Kotlin DSL), R8 full-mode shrinking (< 1.5 MB release APK)
- **Testing**: JUnit 4, unit tests for packet serialization, DSP curves, and StateFlow logic

## Commands
- **Run Unit Tests**: `.\gradlew.bat testDebugUnitTest`
- **Build Debug APK**: `.\gradlew.bat assembleDebug`
- **Build Release APK**: `.\gradlew.bat assembleRelease`
- **Explore Codebase**: `codegraph explore "<symbol or query>"`

## Code Conventions
- **BLE Concurrency**: Selalu gunakan `Mutex` untuk sinkronisasi antrean GATT write. Jangan pernah mengirim concurrent writes tanpa menunggu callback `onCharacteristicWrite`.
- **Rate Limiting**: Gunakan cooldown buffer (minimal 100-300 ms) saat user menggeser fader EQ atau toggle ANC untuk mencegah register freeze pada earbud.
- **State Management**: Simpan single source of truth hardware state di [`TozoBleManager`](app/src/main/java/com/ourkode/tozocontrol/data/TozoBleManager.kt) diekspos melalui `StateFlow`. UI hanya mengonsumsi state via [`MainScreenViewModel`](app/src/main/java/com/ourkode/tozocontrol/ui/main/MainScreenViewModel.kt).
- **Packet Protocol**:
  - Header: Opcode (2 byte) + Payload Length (1 byte)
  - Checksum: `(sum(payload) % 256).toByte()`
  - Pastikan setiap payload gain dikonversi dengan rumus: `round(gainDb * 10).toInt()`.
- **UI Responsiveness**: Gunakan `BoxWithConstraints` atau adaptive layout agar nyaman dipakai di handphone layar kecil maupun tablet/landscape.

## Boundaries
- **Zero Third-Party Tracking**: Dilarang menambahkan SDK analitik, iklan, Firebase Analytics, atau pelacak pihak ketiga.
- **Hardware Protocol Safety**: Jangan memodifikasi urutan byte paket tanpa verifikasi checksum dan opcode resmi di [README.md](README.md).
- **Deterministic Versioning**: Patuhi formula SemVer 2.0.0 di [VERSIONING.md](VERSIONING.md) dan [app/build.gradle.kts](app/build.gradle.kts):
  `versionCode = (versionMajor * 10000) + (versionMinor * 100) + versionPatch`.
- **Changelog Hygiene**: Setiap penambahan fitur atau perbaikan bug wajib mencatat entri di `CHANGELOG.md` pada bagian `[Unreleased]`.

## Pattern Example: Packet Building

```kotlin
// Format standar framing paket command TOZO BLE
fun buildPacket(opcode: ByteArray, payload: ByteArray = byteArrayOf()): ByteArray {
    val length = payload.size.toByte()
    val checksum = (payload.sumOf { it.toInt() and 0xFF } % 256).toByte()
    return opcode + byteArrayOf(length) + payload + byteArrayOf(checksum)
}
```

<!-- antislop:start -->
## antislop
For UI, copy, people, mobile layout, or code comments work, read `antislop.md` (core) and then the skill for the task:
- UI / visual: `skills/antislop-ui/SKILL.md`
- Copy & text: `skills/antislop-copywriting/SKILL.md`
- People: `skills/antislop-human/SKILL.md`
- Mobile / responsive: `skills/antislop-layoutmobile/SKILL.md`
- Code comments: `skills/antislop-code/SKILL.md`
Before starting, ask the user when antislop applies: during the work, or after it is done.
To update antislop later: download `antislop.md` again, or run `npx antislop-ai --update` if it was installed as skill folders.
<!-- antislop:end -->

