# Contributing to TOZO Control

Thank you for your interest in contributing to TOZO Control. Community contributions help expand support for additional TOZO earbud models, improve user interface ergonomics, and refine Bluetooth Low Energy communication.

---

## How You Can Contribute

1. Expanding Device Support:
   - Test the app with other TOZO models (such as NC2, NC7, NC9, T10, T12, Golden X1, HT2, or Open Buds).
   - If commands or telemetry behave differently on your model, share logcat output with `TozoBleManager` tags or BLE packet captures.

2. Bug Reports & Feature Requests:
   - Check existing issues before opening a new one.
   - Include your Android version, phone model, and earbud firmware revision.

3. Code Contributions:
   - Fork the repository and create a feature branch (`git checkout -b feature/device-support`).
   - Follow standard Kotlin and Jetpack Compose conventions.
   - Maintain the direct hardware communication design (no third-party tracking, ads, or unnecessary dependencies).
   - Verify that all unit tests pass and the release APK builds cleanly:
     ```bash
     ./gradlew testDebugUnitTest assembleRelease
     ```
   - Open a Pull Request with a clear description of changes.

---

## Versioning & Release Guidelines

This project adheres to [Semantic Versioning 2.0.0](https://semver.org/) and the project versioning specification in [VERSIONING.md](VERSIONING.md):

### 1. Version Codes & Names (`app/build.gradle.kts`)
- `versionCode` (Integer): Deterministic formula defined in `build.gradle.kts`:
  `(versionMajor * 10000) + (versionMinor * 100) + versionPatch`
- `versionName` (String): Standard SemVer format `MAJOR.MINOR.PATCH`:
  - `MAJOR`: Incompatible breaking changes or bumping `minSdk`.
  - `MINOR`: Backward-compatible new features (for example, adding support for a new TOZO model or new UI panels).
  - `PATCH`: Backward-compatible bug fixes, BLE stability improvements, or minor layout adjustments.

### 2. Changelog
- When submitting features or bug fixes, update `CHANGELOG.md` under the `[Unreleased]` section.
- Follow the [Keep a Changelog](https://keepachangelog.com/) structure (`Added`, `Changed`, `Fixed`, `Removed`).

### 3. Commit Conventions
We recommend Conventional Commits for a clear Git history:
- `feat: <description>` for new features
- `fix: <description>` for bug fixes
- `refactor: <description>` for refactoring without behavior change
- `docs: <description>` for documentation updates
- `chore: <description>` for build tooling, dependencies, or configuration

### 4. Git Tagging & Automated Releases
Releases are created by pushing a Git tag matching `v*`:
```bash
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin v1.1.0
```
Pushing a release tag automatically triggers GitHub Actions to run tests, compile the optimized release APK, and publish it to GitHub Releases.

---

## Code of Conduct

Please treat everyone with respect and constructive feedback.
