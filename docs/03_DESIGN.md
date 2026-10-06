# SensorLab: Design Document

Covers system architecture, data flow, data model, UI/UX, and error handling. Requirement IDs (F-xx) refer to the PRD.

---

## 1. Architecture Overview

```
┌────────────────────────────── UI (Compose) ──────────────────────────────┐
│ Home │ Sensors │ Record │ Sessions │ Session Detail │ Settings           │
└───────────────┬──────────────────────────────────────────────────────────┘
                │ StateFlow / events
┌───────────────▼───────────────┐
│          ViewModels           │
└───────┬───────────────┬───────┘
        │               │
┌───────▼──────┐  ┌─────▼────────────────────┐
│ Repositories │  │ RecordingController      │──binds──► RecordingService
│ (Session,    │  │ (start/stop/pause/marker)│          (foreground)
│  Sensor,     │  └──────────────────────────┘               │
│  Settings)   │                                  ┌──────────▼───────────┐
└──────┬───────┘                                  │ SensorCollector      │
       │                                          │  - HandlerThread     │
┌──────▼───────┐   ┌─────────────────┐            │  - listeners         │
│ Room DB      │   │ Session files   │◄──batches──┤  - ring buffers      │
│ (metadata)   │   │ (CSV per sensor)│            │ SampleWriter (IO)    │
└──────────────┘   └─────────────────┘            │ Location/System feed │
                                                  └──────────────────────┘
```

**Principle:** the recording path (service, collector, writer) has no dependency on UI. The UI only sends commands and observes state.

---

## 2. Modules and Packages

```
com.sensorlab
├── app/                 Application, Hilt setup, MainActivity, nav graph
├── core/
│   ├── model/           Session, SensorInfo, Marker, Label, QualityStats
│   ├── time/            ClockProvider (boot/wall offset)
│   └── util/            Formatters, Result types
├── data/
│   ├── db/              Room: AppDatabase, DAOs, entities
│   ├── files/           SessionFileManager, CsvWriter, ZipExporter
│   ├── sensors/         SensorRepository (discovery, metadata)
│   └── settings/        DataStore wrappers
├── recording/
│   ├── RecordingService.kt
│   ├── RecordingController.kt
│   ├── SensorCollector.kt
│   ├── SampleBuffer.kt  (ring buffer / queue)
│   ├── SampleWriter.kt
│   ├── LocationCollector.kt
│   └── SystemSignalCollector.kt
└── ui/
    ├── home/ sensors/ record/ sessions/ detail/ settings/
    ├── components/      Charts, SensorCard, LabelChips, PermissionRationale
    └── theme/
```

---

## 3. Recording Pipeline (detailed)

### 3.1 Start sequence
1. User configures sensors, rate, label, phone position on the **Record** screen.
2. `RecordingController.start(config)` checks permissions and free storage.
3. It starts `RecordingService` with `startForegroundService()`; service calls `startForeground()` within a few seconds with the notification.
4. Service creates a session folder, writes `device.json` and an initial `session.json` (status = `recording`), inserts a Room row.
5. Captures `bootNs = SystemClock.elapsedRealtimeNanos()` and `wallMs = System.currentTimeMillis()` back-to-back, stores the offset.
6. Acquires a partial wake lock.
7. `SensorCollector` registers each selected sensor on a dedicated `HandlerThread`.

### 3.2 Sample path
```
SensorEvent (callback thread)
   → copy (t_ns, accuracy, values) into pre-allocated slot in per-sensor ring buffer
   → signal writer (non-blocking)
SampleWriter (IO coroutine)
   → every ~500 ms or buffer ≥ N: drain each ring buffer
   → format rows → BufferedOutputStream per sensor
   → flush; update live counters (sample count, last value)
```
- No allocations or I/O inside `onSensorChanged`.
- If a ring buffer overflows, increment a `dropped` counter and continue (surfaced in quality stats and UI warning).
- Live UI reads counters/last values via a throttled `StateFlow` (about 5-10 Hz) so rendering never slows collection.

### 3.3 Stop sequence
1. Unregister listeners, stop location/system collectors.
2. Drain buffers one last time, flush and close streams.
3. Compute quality stats per sensor (count, mean/median/min/max Δt, dropped, effective Hz).
4. Update `session.json` (status = `complete`, end time, stats) and Room row.
5. Release wake lock, stop foreground, stop service.

### 3.4 Interruption handling
- On `onTaskRemoved` / `onDestroy` / low-storage: run the stop sequence with status `interrupted` and a reason.
- On next launch, scan for sessions with status `recording` (process died), close them as `interrupted`, and compute stats from whatever file contents exist.

### 3.5 Pause
Pause unregisters listeners and writes a `pause`/`resume` marker. Timestamps continue on the same monotonic clock, so gaps are visible in analysis.

---

## 4. Timestamp and Alignment Design

- All sensors report `event.timestamp` on the `elapsedRealtimeNanos` base (on virtually all modern devices). Store as-is: `t_ns`.
- Wall time for a sample: `wall_ns = t_ns + boot_to_wall_offset_ns`.
- Location timestamps: use `elapsedRealtimeNanos` from `Location` for the same base.
- Markers: stamped with `SystemClock.elapsedRealtimeNanos()` at tap time.
- Analysis side: convert to seconds from session start, then align with `merge_asof` or interpolate to a common grid.
- Known caveat: some devices report sensor timestamps on a different base. A **self-check** at session start compares the first few events' timestamps with `elapsedRealtimeNanos()` and flags a mismatch in `session.json` (`timestamp_check: ok | suspect`).

---

## 5. Data Model

### 5.1 Room tables

**session**
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | timestamp + short random |
| name | TEXT | |
| label | TEXT? | main activity |
| notes | TEXT? | |
| phone_position | TEXT? | hand / pocket / bag / table / other |
| start_wall_ms | INTEGER | |
| end_wall_ms | INTEGER? | |
| boot_offset_ns | INTEGER | |
| status | TEXT | recording / complete / interrupted |
| interruption_reason | TEXT? | |
| size_bytes | INTEGER | |
| folder_path | TEXT | |

**session_sensor**: `session_id`, `sensor_key`, `sensor_type`, `name`, `requested_hz`, `effective_hz`, `sample_count`, `dropped_count`, `min_dt_ns`, `max_dt_ns`, `median_dt_ns`.

**marker**: `id`, `session_id`, `t_ns`, `label`, `note`.

**label_preset**: `id`, `name`, `color`, `sort_order`.

### 5.2 Sensor key naming
`<type_name>_<index>` e.g. `accelerometer`, `accelerometer_uncalibrated`, `gyroscope`, `magnetic_field`, `pressure`, `light`, `proximity`, `rotation_vector`, `step_counter`. If a device has two sensors of the same type, append an index.

### 5.3 Column names by sensor type
| Type | Columns |
|---|---|
| Accelerometer, gravity, linear accel, gyroscope, magnetometer | `x,y,z` |
| Uncalibrated variants | `x,y,z,bias_x,bias_y,bias_z` |
| Rotation vector (and game/geomagnetic) | `x,y,z,w,heading_accuracy` |
| Light, pressure, proximity, temperature, humidity | `value` |
| Step counter | `steps` |
| Heart rate | `bpm` |
| Unknown | `v0..vn` |

---

## 6. UI / UX Design

### 6.1 Navigation
Bottom navigation with four tabs: **Sensors**, **Record**, **Sessions**, **Settings**. Session detail opens as a pushed screen. A **Home/dashboard** is optional; the Record tab can be the start destination.

### 6.2 Screens

**A. Sensors (discovery)**
- Header: device model, Android version, sensor count.
- Searchable list of sensor cards: icon, name, type, vendor, range/resolution, min delay (max Hz), power.
- Tap a card for a detail sheet with all fields plus a **live value preview** and a "test rate" button that measures the actual Hz over 5 seconds.
- Filter chips: All, Motion, Position, Environment, Other, Wake-up.

**B. Record**
- Step 1: sensor selection grid with checkboxes and "Select all / None / Recommended" shortcuts.
- Step 2: rate preset (Normal / UI / Game / Fastest / Custom Hz) with a warning if the chosen rate is above the sensor's maximum or needs a special permission.
- Step 3: session info: name (auto-suggested), label chips, phone position, optional duration limit and countdown.
- Toggles: Location, System signals.
- Big **Start** button; estimated storage per minute shown beneath.
- **While recording:** timer, state (Recording / Paused), live sample counters per sensor, a mini live chart for one chosen sensor, dropped-sample warning, **Marker** button, **Pause**, **Stop** (confirm on long press or dialog).

**C. Sessions**
- List with name, date, duration, size, label chip, status badge (Complete / Interrupted).
- Sort and filter by label, date, status; multi-select for bulk delete/export.
- Swipe actions: export, delete.

**D. Session detail**
- Summary header (name, label, position, duration, size, status).
- Tabs: **Overview** (sensor table with samples, effective Hz, dropped %), **Markers**, **Preview** (downsampled chart for chosen sensor), **Info** (device and app versions, timestamp check).
- Actions: Rename, Edit label/notes, Export ZIP, Delete.

**E. Settings**
- Defaults (rate, sensors), label presets editor, storage usage and cleanup, battery optimization help, theme, about/version, "Delete all data".

### 6.3 Notification (recording)
- Title "SensorLab recording", subtitle with elapsed time and sensor count.
- Actions: **Marker**, **Pause/Resume**, **Stop**.
- Tapping opens the Record screen.

### 6.4 Permission UX
- Rationale dialog before each system prompt, explaining why the permission is needed.
- If denied, the related sensor is shown as unavailable with a "Grant permission" action, and recording proceeds with the rest.
- Special onboarding card on first launch: battery optimization whitelist instructions per major OEM.

### 6.5 Visual Style
- Material 3, dynamic color on Android 12+, fallback palette: deep teal primary, amber accent for warnings, red for errors.
- Typography: default Material 3 type scale; monospaced numeric style for live values.
- Dark theme supported; large touch targets (≥ 48 dp) because the phone may be used while moving.
- Accessibility: content descriptions, sufficient contrast, no information conveyed by color alone (icons and text accompany status colors).

### 6.6 Wireframe Sketches

```
Record (idle)                        Record (active)
┌──────────────────────────┐        ┌──────────────────────────┐
│ New recording            │        │ ● Recording    00:12:41  │
│ Sensors [12/18] Select ▾ │        │ ────────────────────────│
│ [✓ Accel][✓ Gyro][✓ Mag] │        │ Accelerometer  76,203    │
│ [✓ Baro ][ ] Light ...   │        │ Gyroscope      76,198    │
│ Rate: [UI][Game][100 Hz] │        │ Magnetometer   38,100    │
│ Label: (walking)(sitting)│        │ ⚠ 0.2% dropped (accel)   │
│ Position: ( pocket )     │        │  ┌────────────────────┐  │
│ ☑ Location  ☐ System     │        │  │ ~~~ live chart ~~~ │  │
│ ≈ 14 MB / min            │        │  └────────────────────┘  │
│ [        START         ] │        │ [Marker] [Pause] [Stop]  │
└──────────────────────────┘        └──────────────────────────┘
```

---

## 7. Error Handling and Edge Cases

| Case | Behavior |
|---|---|
| Sensor unavailable or permission denied | Skip it, show notice, record the rest |
| Requested rate above hardware max | Clamp, warn, log requested vs effective |
| Low storage (< 200 MB) | Warn before start; auto-stop with `interrupted` reason when < 50 MB |
| Process killed | Recover on next launch, mark as interrupted |
| Phone call / app switch | Recording continues (service); no UI dependency |
| Doze / OEM kill | Wake lock + foreground service; show in-app battery help |
| Clock changes (user changes time) | Monotonic `t_ns` unaffected; wall time stored once at start |
| Export fails (no space, SAF denied) | Show retry; keep session untouched |
| Corrupt/partial CSV line (crash) | Loader skips incomplete last line; documented in analysis notebook |

---

## 8. Performance Design

- Dedicated `HandlerThread` for sensor callbacks; avoid main thread.
- Pre-allocated float arrays in ring buffers; reuse to avoid GC pauses.
- Batched, buffered writes (64 KB buffers); one file per sensor to avoid interleaving.
- UI updates throttled; heavy charts use downsampled data.
- Use `maxReportLatencyUs` batching for high-rate sensors to reduce CPU wakeups when allowed.
- Benchmark target: 10 sensors at 100 Hz for 30 min, CPU under ~10% on a mid-range device (verify with Android Studio Profiler).

---

## 9. Security and Privacy Design

- App-private storage (`filesDir`); no external storage access except user-initiated export.
- No network permission in v1 (verify in the manifest).
- Exports contain a `README.txt` warning when location or Wi-Fi/Bluetooth data is included.
- "Delete all data" wipes DB and files.
- Optional later: encrypt session files at rest with Jetpack Security / Keystore.

---

## 10. Testing Strategy

| Level | What |
|---|---|
| Unit | Ring buffer, CSV formatting, timestamp offset, quality stat calculations, label presets |
| Integration | Writer + file manager with fake sample streams; Room DAOs with in-memory DB |
| Instrumented | Service start/stop, permission flows, export ZIP validity |
| Manual device tests | 3+ phones: sensor discovery, 30-min recording, screen-off, battery saver, rotation, kill-and-recover |
| Data validation | Python script that checks monotonic `t_ns`, column counts, effective rates, and cross-sensor alignment |

---

## 11. Implementation Order (suggested)

1. Project skeleton, Hilt, navigation, theme.
2. `SensorRepository` + Sensors screen + `device.json`.
3. `SampleBuffer`, `SampleWriter`, `SensorCollector` with unit tests (no UI).
4. `RecordingService` + controller + Record screen (minimal).
5. Room schema, session list/detail, quality stats.
6. Labels, markers, notification actions.
7. ZIP export + Python loader notebook.
8. Location/system collectors, settings, polish, multi-device testing.
