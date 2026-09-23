# Application Versioning Specification

This document defines the official versioning policy, mathematical formulas, and increment rules for **TOZO Control**.

---

## 1. Core Principles

TOZO Control follows **[Semantic Versioning 2.0.0](https://semver.org/)** (`MAJOR.MINOR.PATCH`) paired with a deterministic Android `versionCode` calculation formula.

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
val versionMinor = 0
val versionPatch = 0

defaultConfig {
    versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
    versionName = "$versionMajor.$versionMinor.$versionPatch"
}
```

### Formula Definitions

| Identifier | Type | Formula | Description |
| :--- | :--- | :--- | :--- |
| **`versionName`** | String | `"${MAJOR}.${MINOR}.${PATCH}"` | Human-readable version string shown in UI, logs, and GitHub Releases. |
| **`versionCode`** | Integer | `(MAJOR * 10000) + (MINOR * 100) + PATCH` | Monotonically increasing 32-bit integer for Android OS and package managers. |

### Why this Formula?
- **Allocates 2 digits for Patch** ($00 \dots 99$).
- **Allocates 2 digits for Minor** ($00 \dots 99$).
- **Allocates remaining upper digits for Major** ($1 \dots 214748$).
- **Strictly Monotonic**: Any bump to Major, Minor, or Patch mathematically guarantees a higher `versionCode`, ensuring that Android OS, Google Play, and F-Droid will always recognize the build as an upgrade.

### Version Mapping Reference

| `versionName` | `MAJOR` | `MINOR` | `PATCH` | `versionCode` | Classification |
| :---: | :---: | :---: | :---: | :---: | :--- |
| **`1.0.0`** | 1 | 0 | 0 | `10000` | Initial open-source release |
| **`1.0.1`** | 1 | 0 | 1 | `10001` | Patch: BLE reconnection fix |
| **`1.0.2`** | 1 | 0 | 2 | `10002` | Patch: UI padding adjustment |
| **`1.1.0`** | 1 | 1 | 0 | `10100` | Minor: Add TOZO NC9 device support |
| **`1.1.1`** | 1 | 1 | 1 | `10101` | Patch: Fix NC9 opcode parsing |
| **`1.2.0`** | 1 | 2 | 0 | `10200` | Minor: Add custom preset import/export |
| **`2.0.0`** | 2 | 0 | 0 | `20000` | Major: Architectural rewrite or `minSdk` bump |

---

## 3. Decision Matrix: When to Bump What

### A. MAJOR Bump (`X.0.0`)
Increment `versionMajor += 1`, reset `versionMinor = 0`, reset `versionPatch = 0`.

**When to use:**
- **Breaking platform changes**: Raising `minSdk` (e.g. dropping older Android OS releases).
- **Breaking data schema**: Stored preferences or database models change without backward-compatible automatic migration.
- **Architectural rewrite**: Complete refactoring of the BLE communication stack or state management pipeline.
- **Removal of functionality**: Dropping support for previously supported earbud models or removing core capabilities.

---

### B. MINOR Bump (`1.X.0`)
Increment `versionMinor += 1`, reset `versionPatch = 0`.

**When to use:**
- **New Device Support**: Adding reverse-engineered protocol support for a new TOZO model (e.g., NC2, NC7, NC9, T10, Golden X1, HT2, Open Buds).
- **New Features**:
  - Adding new DSP presets or custom EQ import/export.
  - Adding homescreen widgets or quick settings tiles.
  - Adding charging case telemetry or Bluetooth audio codec indicators.
- **Enhancements**: Non-breaking additions to UI cards or telemetry meters.

---

### C. PATCH Bump (`1.0.X`)
Increment `versionPatch += 1`.

**When to use:**
- **Bug Fixes**:
  - Resolving BLE disconnects, GATT 133 timeouts, or MTU negotiation errors.
  - Fixing DSP 24-byte checksum calculation or rounding errors in EQ sliders.
  - Fixing UI rendering glitches, layout overflows, or dark/light theme flickering.
- **Performance & Stability**:
  - Tuning fader rate-limiting/cooldown buffers.
- **Maintenance**:
  - Updating internal dependencies (Gradle, Compose, Kotlin) without API changes.
  - Fixing typos, documentation, or string translations.

---

## 4. Release Checklist

When cutting a new release:

1. **Update Version Variables**:
   In [`app/build.gradle.kts`](app/build.gradle.kts), update `versionMajor`, `versionMinor`, or `versionPatch` according to the decision matrix above.

2. **Update Changelog**:
   In [`CHANGELOG.md`](CHANGELOG.md), move items from `[Unreleased]` into a new section:
   ```markdown
   ## [1.1.0] - YYYY-MM-DD
   ### Added
   - Support for TOZO NC9 active noise cancellation.
   ```

3. **Verify Build & Tests**:
   ```bash
   ./gradlew test
   ./gradlew assembleRelease
   ```

4. **Commit & Tag**:
   ```bash
   git add app/build.gradle.kts CHANGELOG.md
   git commit -m "chore(release): bump version to 1.1.0"
   git tag -a v1.1.0 -m "Release v1.1.0"
   git push origin main --tags
   ```

5. **Automated CI/CD**:
   GitHub Actions will automatically pick up the `v*` tag, build the signed APKs, and publish them to GitHub Releases.
