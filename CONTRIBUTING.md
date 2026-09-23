# Contributing to TOZO Control

Thank you for your interest in contributing to **TOZO Control**! We welcome community contributions to help expand support for more TOZO earbud models, improve UI responsiveness, and optimize Bluetooth Low Energy communication.

---

## How You Can Contribute

1. **Expanding Device Support**:
   - Test the app with other TOZO models (e.g. NC2, NC7, NC9, T10, T12, Golden X1, HT2, Open Buds).
   - If a feature behaves differently on your model, share logcat output with `TozoBleManager` tags or BLE packet captures.

2. **Bug Reports & Feature Requests**:
   - Check existing issues before creating a new one.
   - Provide your Android version, device model, and earbud firmware version.

3. **Code Contributions**:
   - Fork the repository and create a feature branch (`git checkout -b feature/model-support`).
   - Follow standard Kotlin and Jetpack Compose best practices.
   - Maintain the minimalist, bloat-free design philosophy (no third-party tracking, ads, or heavy unnecessary dependencies).
   - Ensure the project builds cleanly with `./gradlew assembleDebug`.
   - Open a Pull Request with a clear summary of changes.

---

## Versioning & Release Guidelines

This project adheres to **[Semantic Versioning 2.0.0](https://semver.org/)** and Android versioning conventions:

### 1. Version Codes & Names (`app/build.gradle.kts`)
- **`versionCode` (Integer)**: An incrementing counter (1, 2, 3...) required by Android OS and package installers (F-Droid, GitHub Releases). **Must be incremented by +1 for every public build or release.**
- **`versionName` (String)**: SemVer format `MAJOR.MINOR.PATCH` (e.g., `1.0.0`):
  - **`MAJOR`**: Incompatible breaking changes, fundamental architectural rewrites, or bumping `minSdk` that drops older Android versions.
  - **`MINOR`**: New features added in a backward-compatible manner (e.g., adding support for a new TOZO model, new DSP presets, auto-reconnect).
  - **`PATCH`**: Backward-compatible bug fixes, BLE transmission stabilization, or minor UI adjustments.

### 2. Changelog
- When submitting features or bug fixes, update `CHANGELOG.md` under the `[Unreleased]` section.
- Follow the [Keep a Changelog](https://keepachangelog.com/) structure (`Added`, `Changed`, `Fixed`, `Removed`, `Security`).

### 3. Commit Conventions
We recommend Conventional Commits for clear Git history:
- `feat: <description>` for new features
- `fix: <description>` for bug fixes
- `refactor: <description>` for refactoring without behavior change
- `docs: <description>` for documentation changes
- `chore: <description>` for build tooling, dependencies, or configuration

### 4. Git Tagging & Automated Releases
Releases are cut by creating and pushing a Git tag matching `v*`:
```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```
Pushing a release tag automatically triggers the GitHub Actions CI workflow to compile and attach release APKs to the GitHub Releases page.

---

## Code of Conduct

Please treat everyone with respect, kindness, and constructive feedback. Let's build a clean, respectful open-source community together!
