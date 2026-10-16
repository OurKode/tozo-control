# Application Versioning Specification

This document defines the versioning policy, mathematical formulas, and increment rules for TOZO Control.

---

## 1. Core Principles

TOZO Control follows [Semantic Versioning 2.0.0](https://semver.org/) (`MAJOR.MINOR.PATCH`) paired with a deterministic Android `versionCode` calculation formula.

```
       v 1 . 2 . 4
         │   │   └── PATCH: Backward-compatible bug fixes
         │   └────── MINOR: Backward-compatible new features
         └────────── MAJOR: Breaking changes or architecture shifts
```

---

## 2. Mathematical Versioning Formulas

Both version attributes are defined in [`app/build.gradle.kts`](app/build.gradle.kts):

```kotlin
val versionMajor = 1
val versionMinor = 1
val versionPatch = 0

defaultConfig {
    versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
    versionName = "$versionMajor.$versionMinor.$versionPatch"
}
```

### Formula Definitions

| Identifier | Type | Formula | Description |
| :--- | :--- | :--- | :--- |
| `versionName` | String | `"${MAJOR}.${MINOR}.${PATCH}"` | Version string shown in UI, logs, and GitHub Releases. |
| `versionCode` | Integer | `(MAJOR * 10000) + (MINOR * 100) + PATCH` | Monotonically increasing 32-bit integer for Android OS and package managers. |

### Formula Structure
- Allocates 2 digits for Patch (00 to 99).
- Allocates 2 digits for Minor (00 to 99).
- Allocates remaining upper digits for Major (1 to 214748).
- Strictly monotonic: Any increment to Major, Minor, or Patch produces a higher `versionCode`, ensuring package managers recognize updates.

### Version Mapping Reference

| `versionName` | `MAJOR` | `MINOR` | `PATCH` | `versionCode` | Classification |
| :---: | :---: | :---: | :---: | :---: | :--- |
| `1.0.0` | 1 | 0 | 0 | `10000` | Initial open-source release |
| `1.0.1` | 1 | 0 | 1 | `10001` | Patch: BLE reconnection fix |
| `1.0.2` | 1 | 0 | 2 | `10002` | Patch: UI padding adjustment |
| `1.1.0` | 1 | 1 | 0 | `10100` | Minor: UI redesign, R8 shrinking, tablet layout |
| `1.1.1` | 1 | 1 | 1 | `10101` | Patch: Zero-overflow graphic equalizer spline curve |
| `1.2.0` | 1 | 2 | 0 | `10200` | Minor: TOZO AeroSound 3 reverse-engineered protocol (touch remap, balance, spatial 3D, multipoint, hardware controls) |
| `2.0.0` | 2 | 0 | 0 | `20000` | Major: Architectural rewrite or `minSdk` bump |

---

## 3. Decision Matrix: When to Bump What

### A. MAJOR Bump (`X.0.0`)
Increment `versionMajor += 1`, reset `versionMinor = 0`, reset `versionPatch = 0`.

When to use:
- Breaking platform changes: Raising `minSdk` to drop older Android releases.
- Breaking data schema: Stored preferences or data models change without backward-compatible migration.
- Architectural rewrite: Fundamental overhaul of the BLE stack or state management pipeline.
- Removal of functionality: Dropping support for previously supported earbud models.

---

### B. MINOR Bump (`1.X.0`)
Increment `versionMinor += 1`, reset `versionPatch = 0`.

When to use:
- New device support: Adding protocol support for a new TOZO model.
- New features:
  - Adding DSP presets or custom EQ curve import/export.
  - Adding homescreen widgets or quick settings tiles.
  - Adding charging case telemetry or Bluetooth audio codec indicators.
- User interface enhancements: Significant layout redesigns or new functional panels.

---

### C. PATCH Bump (`1.0.X`)
Increment `versionPatch += 1`.

When to use:
- Bug fixes:
  - Resolving BLE disconnects, GATT 133 timeouts, or MTU negotiation errors.
  - Fixing DSP 24-byte checksum calculation or rounding errors in sliders.
  - Fixing UI rendering glitches, layout overflows, or dark/light theme switching.
- Performance and stability:
  - Tuning fader rate-limiting or cooldown buffers.
- Maintenance:
  - Updating internal dependencies (Gradle, Compose, Kotlin) without API changes.
  - Fixing typos, documentation, or string translations.

---

## 4. Release Checklist

When cutting a new release:

1. Update version variables in [`app/build.gradle.kts`](app/build.gradle.kts) according to the decision matrix above.
2. Update [`CHANGELOG.md`](CHANGELOG.md) by moving items from `[Unreleased]` into a dated release section.
3. Verify that all tests pass and the release APK builds:
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleRelease
   ```
4. Commit and tag:
   ```bash
   git add app/build.gradle.kts CHANGELOG.md
   git commit -m "chore(release): bump version to 1.1.0"
   git tag -a v1.1.0 -m "Release v1.1.0"
   git push origin main --tags
   ```
5. GitHub Actions will automatically detect the `v*` tag, build the optimized release APK, and publish it to GitHub Releases.
