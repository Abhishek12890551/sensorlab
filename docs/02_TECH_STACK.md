# SensorLab: Tech Stack

Versions below are guidance; use the latest stable releases when you start and pin them in the Gradle version catalog.

---

## 1. Summary Table

| Layer | Choice | Why |
|---|---|---|
| Language | **Kotlin** | First-class Android support, coroutines, concise |
| UI | **Jetpack Compose + Material 3** | Modern, less boilerplate, easy live-updating screens |
| Architecture | **MVVM + unidirectional data flow, Clean-ish layers** | Testable, standard on Android |
| Async | **Kotlin Coroutines + Flow** | Streams of sensor/UI state, structured concurrency |
| DI | **Hilt** | Standard, integrates with ViewModel and services |
| Local DB | **Room (SQLite)** | Session metadata, markers, stats |
| Raw sample storage | **Per-sensor files (CSV v1, binary option later)** | Fast sequential append, trivial to export |
| Preferences | **Jetpack DataStore** | Settings, presets |
| Navigation | **Navigation Compose** | Type-safe routes |
| Background work | **Foreground Service** (recording), **WorkManager** (export/zip, cleanup) | Right tool for each |
| Location | **Fused Location Provider** (Play Services) | Best accuracy/battery trade-off |
| Build | **Gradle (Kotlin DSL) + version catalog** | Reproducible builds |
| Min / Target SDK | **minSdk 26, targetSdk latest stable** | Covers the vast majority of devices |
| Testing | **JUnit5/JUnit4, MockK, Turbine, Compose UI tests, Robolectric** | Unit, Flow, and UI coverage |
| Static analysis | **ktlint + detekt** | Consistent, clean code |
| CI/CD | **GitHub Actions** | Build, lint, test, attach APK artifact |
| Analysis | **Python 3.11+, Jupyter, pandas, NumPy, SciPy, matplotlib/plotly, scikit-learn, pyarrow** | Standard data/ML toolchain |

---

## 2. Android App

### 2.1 Language and Tooling
- Kotlin (latest stable), Android Studio (latest stable), JDK 17+.
- Gradle Kotlin DSL; dependencies in `gradle/libs.versions.toml`.
- Single `app` module at first, with packages split by feature. Move to multi-module (`core-sensors`, `core-data`, `feature-record`, `feature-sessions`) only if the project grows.

### 2.2 UI
- Jetpack Compose, Material 3, edge-to-edge layouts.
- Compose Charts: start with a lightweight custom Canvas line chart for live preview; consider Vico or MPAndroidChart later.
- Accompanist/permissions: use the official `rememberLauncherForActivityResult` or a small permission helper.

### 2.3 Sensor Collection Core
- `SensorManager` + `SensorEventListener` (or `registerListener` with a `Handler` on a dedicated `HandlerThread` to keep callbacks off the main thread).
- Batching with `maxReportLatencyUs` where supported.
- Sample hand-off: **lock-free single-producer queue** (e.g. `ConcurrentLinkedQueue` or a pre-allocated ring buffer / `Channel` with `BUFFERED` capacity) to avoid allocations in the callback.
- Writer: one coroutine on `Dispatchers.IO` draining the queue into `BufferedOutputStream`s, flushing every ~500 ms or N samples.
- Timing: `SystemClock.elapsedRealtimeNanos()` at session start plus `System.currentTimeMillis()` to compute the boot-to-wall offset.

### 2.4 Persistence
- **Room entities:** `Session`, `SessionSensor`, `Marker`, `SensorStats`, `LabelPreset`.
- **Files:** `filesDir/sessions/<sessionId>/<sensorKey>.csv`, plus `session.json`, `device.json`, `markers.csv`.
- Export zips the session folder via `ZipOutputStream` in a WorkManager job, shared through `FileProvider` or written to a SAF-selected location.
- Future option: Protocol Buffers or a custom fixed-width binary format for 5-10x smaller files.

### 2.5 Dependency Injection and Config
- Hilt for ViewModels, repositories, DB, DataStore, and the recording controller.
- Build variants: `debug` (extra logging, StrictMode), `release` (R8 minification).

### 2.6 Libraries (indicative)
```
androidx.core:core-ktx
androidx.lifecycle:lifecycle-runtime-compose, lifecycle-viewmodel-compose, lifecycle-service
androidx.activity:activity-compose
androidx.compose:compose-bom (material3, ui, ui-tooling)
androidx.navigation:navigation-compose
androidx.room:room-runtime, room-ktx, room-compiler (KSP)
androidx.datastore:datastore-preferences
androidx.work:work-runtime-ktx
com.google.dagger:hilt-android, hilt-compiler (KSP), androidx.hilt:hilt-navigation-compose
org.jetbrains.kotlinx:kotlinx-coroutines-android
org.jetbrains.kotlinx:kotlinx-serialization-json   (session.json, device.json)
com.google.android.gms:play-services-location
com.jakewharton.timber:timber                        (logging)
```
Test: `junit`, `mockk`, `app.cash.turbine`, `kotlinx-coroutines-test`, `androidx.compose.ui:ui-test-junit4`, `robolectric`.

### 2.7 Manifest Essentials
- Foreground service declaration with proper `foregroundServiceType`.
- Permissions listed in the PRD (section 10).
- `FileProvider` for sharing exports.
- Optional: `android:largeHeap="false"` (avoid; design for low memory instead).

---

## 3. Data File Specification (v1)

**Per-sensor CSV** (`accelerometer.csv`):
```
t_ns,accuracy,x,y,z
123456789012,3,0.12,9.81,0.03
```
Column names depend on the sensor type; unknown sensor types use `v0,v1,...`.

**`session.json`**
```json
{
  "id": "2026-10-06T10-15-00_ab12",
  "name": "Walk test 1",
  "label": "walking",
  "phone_position": "pocket",
  "start_wall_ms": 1791281700000,
  "boot_to_wall_offset_ns": 1791281699000000000,
  "duration_s": 1800,
  "status": "complete",
  "app_version": "0.1.0",
  "sensors": [{"key": "accelerometer", "requested_hz": 100, "effective_hz": 99.7, "samples": 179460}]
}
```

**`device.json`**: manufacturer, model, Android version, API level, build fingerprint, full sensor metadata list.

**`markers.csv`**: `t_ns,label,note`

---

## 4. Analysis Environment (Python)

| Purpose | Tools |
|---|---|
| Environment | `uv` or `conda`, Python 3.11+ |
| Notebooks | JupyterLab or VS Code notebooks |
| Data handling | pandas, NumPy, pyarrow (Parquet) |
| Signal processing | SciPy (`signal`, `fft`), `tsfresh` or `tsfel` for feature extraction |
| Visualization | matplotlib, seaborn, plotly |
| ML | scikit-learn first; PyTorch for deep models (1D CNN / LSTM / Transformer) |
| Experiment tracking | MLflow or Weights & Biases (optional) |
| Data versioning | DVC or plain Git LFS (optional) |

**Starter notebook outline:** load session ZIP, read `device.json` and `session.json`, load per-sensor CSVs, convert `t_ns` to seconds, resample to a common rate with `merge_asof` or interpolation, quality report (rate histograms, gaps), plots, windowing (e.g. 2 s with 50% overlap), feature extraction, baseline classifier.

---

## 5. DevOps and Project Hygiene

- **Git:** GitHub repository, `main` plus short-lived feature branches, conventional commits.
- **CI (GitHub Actions):** on each push: `./gradlew ktlintCheck detekt testDebugUnitTest assembleDebug`, upload debug APK as an artifact.
- **Release:** signed release APK/AAB via GitHub Releases (keystore stored in encrypted secrets).
- **Docs:** `README.md` (setup, permissions, data format), `docs/` for these three documents.
- **Issue tracking:** GitHub Issues/Projects, one issue per PRD requirement ID.
- **Crash reporting:** none in v1 (privacy); local log export instead.

---

## 6. Alternatives Considered

| Option | Verdict |
|---|---|
| Flutter / React Native | Sensor plugins limit rates, metadata and background control; native Kotlin gives full access. |
| Java | Fine, but Kotlin is the Android default with better coroutine support. |
| XML Views | Works, but Compose is faster to build and maintain. |
| Room-only sample storage | Slower inserts and larger files at 100+ Hz; flat files are simpler and faster for raw streams. |
| Firebase/cloud backend | Out of scope for v1; adds privacy and cost concerns. |
| Termux/Python on device | Poor background reliability and timing control. |
