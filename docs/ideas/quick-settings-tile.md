# Quick Settings Tile (Android Notification Shade)

## Problem Statement
Bagaimana kita memungkinkan pengguna beralih mode ANC, Transparansi, Normal, dan Game Mode secara instan (1 ketukan) dari Quick Settings sistem Android tanpa membuka aplikasi dan tanpa background battery drain?

## Recommended Direction
Implementasikan dua layanan `TileService` native Android terpisah:
1. `TozoNoiseTileService`: Tile siklus 3-state untuk Noise Control (ANC -> Transparency -> Normal -> ANC). Label dan ikon tile menampilkan mode yang sedang aktif.
2. `TozoGameModeTileService`: Tile toggle 2-state untuk Game Mode (Aktif dengan latensi 45 ms vs Non-aktif).

Pendekatan ini dipilih karena memberikan akses tercepat dari bilah notifikasi sistem tanpa memerlukan Activity aplikasi berada di foreground. Seluruh operasi BLE dijalankan on-demand saat tile diketuk (`onClick()`), sehingga tidak memerlukan background worker atau polling konstan, menjaga prinsip zero background battery drain.

## Key Assumptions to Validate
- [ ] **Koneksi On-Demand GATT**: Earbud yang sudah ter-pair dapat menerima write packet via `BluetoothGatt` langsung dari lifecycle `TileService` tanpa perlu membuka UI utama.
- [ ] **Sinkronisasi Status Tile**: Panggilan `qsTile.state` dan `updateTile()` dapat mencerminkan status earbud (Active, Inactive, Unavailable) secara akurat saat panel notifikasi dibuka (`onStartListening()`).
- [ ] **Penanganan Kondisi Offline**: Jika earbud tidak terhubung atau Bluetooth mati, tile langsung menampilkan `STATE_UNAVAILABLE` dan tidak mengalami freeze atau timeout tak terbatas.

## MVP Scope
- **Komponen**:
  - `TozoNoiseTileService`: Siklus mode peredam bising (ANC / Transparency / Normal).
  - `TozoGameModeTileService`: Toggle status Game Mode (On / Off).
  - Registrasi kedua service di `AndroidManifest.xml` dengan permission `android.permission.BIND_QUICK_SETTINGS_TILE`.
- **Interaksi**:
  - Ketukan pada tile mengirim byte command resmi yang sudah terverifikasi di protocol TOZO.
  - Feedback visual state langsung ter-update di panel Quick Settings.

## Not Doing (and Why)
- **Auto-Profile berbasis aplikasi aktif**: Ditolak karena membutuhkan service latar belakang terus-menerus dan izin sensitif `PACKAGE_USAGE_STATS`.
- **Auto-Wind Reduction berbasis sensor/GPS**: Ditolak karena meningkatkan konsumsi daya baterai secara signifikan.
- **Tile ketiga untuk Custom EQ Switch**: Ditunda agar tidak memadati area notifikasi pengguna dengan terlalu banyak tile.

## Open Questions
- Apakah perlu menampilkan notifikasi toast singkat jika user mengetuk tile saat earbud berada di luar jangkauan Bluetooth?
