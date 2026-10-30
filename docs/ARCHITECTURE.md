# Architecture Map

Orientation for anyone (human or agent) about to change code here. Read the
section for the layer you are touching, not the whole file.

All source lives under `app/src/main/java/com/ourkode/tozocontrol/` unless noted.

## 1. Layers and data flow

```
MainActivity
  └─ AppPreferencesRepository          (theme/palette/haptics, SharedPreferences)
  └─ TOZOControlTheme(...)
       └─ MainNavigation               (Navigation 3 back stack)
            ├─ MainScreen              (controls + EQ)
            │    └─ MainScreenViewModel
            │         └─ TozoBleManager  ← single source of truth for device state
            └─ SettingsScreen          (pure UI, callbacks in / values out)
```

Everything is one-way:

- **Device state** flows up: `TozoBleManager` → `StateFlow` → `MainScreenViewModel` → Compose.
- **Intent** flows down: Compose callback → `MainScreenViewModel` → `TozoBleManager` → GATT write.
- Compose never touches `BluetoothGatt`, never builds a packet, never writes a characteristic.

## 2. File map

| File | Role |
|---|---|
| `MainActivity.kt` | Entry point. Owns the single `AppPreferencesRepository` instance, applies `TOZOControlTheme`, hosts `MainNavigation`. |
| `Navigation.kt` | `MainNavigation` composable: `rememberNavBackStack(Main)`, `NavDisplay`, `entryProvider`. Has a **legacy `isDark`/`onToggleTheme` overload** kept for compatibility — new code uses the `AppSettings` overload. |
| `NavigationKeys.kt` | `@Serializable data object Main : NavKey` and `Settings`. Add new destinations here. |
| `data/TozoBleManager.kt` | The hardware layer. Singleton via `getInstance(context)`. Owns the GATT connection, scan, packet framing, command queue, and every device `StateFlow`. |
| `data/AppPreferencesRepository.kt` | Singleton over SharedPreferences (`tozo_prefs`). Exposes `settings: StateFlow<AppSettings>`; setters update prefs *and* the flow. Handles migration from the old `dark_mode` boolean. |
| `data/DataRepository.kt` | **Dead template code.** Nothing uses it; do not extend or copy it. |
| `ui/main/MainScreen.kt` | The controls + EQ surface. Also holds the top-level `PRESET_MAP` / `PRESET_NAMES` EQ preset tables and the `active_preset` / custom-gain persistence. |
| `ui/main/MainScreenViewModel.kt` | Thin adapter over `TozoBleManager`: re-exports its flows as properties and enforces per-control cooldowns. |
| `ui/settings/SettingsScreen.kt` | Theme, palette, dynamic colour, haptics. Stateless: values in, callbacks out. |
| `theme/Theme.kt` | `TOZOControlTheme` (two overloads), `ColorScheme.toAmoled()`, `HapticFeedback.performIf()`. |
| `theme/Color.kt`, `theme/Type.kt` | Palette constants and the Material 3 type scale. Must match `DESIGN.md`. |
| `service/TozoGameModeTileService.kt` | Quick Settings tile. Uses `TozoBleManager.getInstance(context)` — never constructs its own manager. |
| `service/TozoNoiseTileService.kt` | Quick Settings tile, same rule. |

Tests live in `app/src/test/` (JVM) and `app/src/androidTest/` (instrumented).
`AudiophilePresetTest` asserts the invariants of `PRESET_MAP` — changing a preset
curve or its name is a test-breaking change, by design.

## 3. Contracts worth not breaking

**State ownership.** `TozoBleManager` is the only owner of device state.
`MainScreenViewModel` does *not* copy flows into its own `MutableStateFlow`; it
aliases them (`val isConnected = bleManager.isConnected`). The only VM-owned
state is `isNoisePending` / `isGameModePending` / `isEqPending`. Adding a
duplicated state copy here would create two sources of truth.

**Cooldown guard pattern.** The VM is where rate limiting lives, not the manager:

```kotlin
fun setGameMode(enable: Boolean) {
    if (_isGameModePending.value) return   // drop the event, do not queue it
    gameJob?.cancel()
    _isGameModePending.value = true
    gameJob = viewModelScope.launch {
        bleManager.setGameMode(enable)
        delay(450)                          // hardware needs this much air
        _isGameModePending.value = false
    }
}
```

Cooldowns: 450 ms game mode, 400 ms noise profile. Apply the same shape to any
new control that hits a hardware register.

**Offline command queue.** If the earbuds are not connected, `TozoBleManager`
stores the intent in `pendingNoiseMode` / `pendingGameMode`, calls `connect()`,
and replays it after the link comes up (`flushPendingAndQueryTelemetry`). New
controls that need to work when disconnected should follow this pattern rather
than silently no-op.

**Two-step hardware writes.** Some commands are a pair. `setNoiseMode` sends the
live DSP switch, then 120 ms later the NVRAM persist command (`bootHexCmd`) so
the setting survives a reconnect. Do not collapse these into one packet.

**Packet framing.** `buildPacket(opcode, payload)` = opcode + length byte +
payload + checksum, where checksum is `(sum(payload) % 256).toByte()`. Gains are
`round(gainDb * 10)`. Verified against `README.md` — do not reorder bytes
without a captured trace.

## 4. Known seam

EQ state is split across layers:

- The *applied* hardware curve is `TozoBleManager.currentEq`.
- The *selected preset name* and custom gains are Compose state in
  `MainScreen.kt`, persisted under `active_preset` / custom keys in the same
  SharedPreferences file.

So a preset can be persisted while the applied curve is not yet pushed to the
device. When touching EQ, check both sides. Unifying this into the manager is a
reasonable future refactor but not a safe drive-by change.

## 5. Where to make a change

| You want to… | Touch |
|---|---|
| Add a device command | `TozoBleManager` (+ `README.md` protocol section, + a serialization test) |
| Add a control to the main screen | `MainScreen.kt` → `MainScreenViewModel` (cooldown guard) → `TozoBleManager` |
| Add a screen | `NavigationKeys.kt` → `Navigation.kt` `entryProvider` |
| Add a preference | `AppPreferencesRepository` (`AppSettings` + getter + setter) then thread it through `MainNavigation` |
| Change colours or type | `theme/Color.kt` / `theme/Type.kt`, mirrored in `DESIGN.md` |
| Change a QS tile | `service/*.kt`, always via `getInstance` |
