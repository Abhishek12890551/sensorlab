# SensorLab: Product Requirements Document (PRD)

**Working name:** SensorLab (rename freely)
**Platform:** Android
**Version:** 0.1 (draft)
**Status:** Planning

---

## 1. Summary

SensorLab is an Android app that discovers every sensor on a phone, records their data into well-structured sessions, and exports it for offline analysis in Python. The first goal is **collection quality** (clean, aligned, well-labelled data). The second goal is **analysis and application**, which happens outside the app and decides which use cases to build later (activity recognition, context detection, etc.).

## 2. Problem Statement

Existing sensor apps either show live values only, log a handful of sensors, or export data without proper timestamps, sensor metadata, or labels. This makes the data hard to analyze, compare across phones, or use for machine learning.

## 3. Goals

1. Collect data from **all** hardware sensors and key system signals available on a device.
2. Produce **time-aligned, lossless, reproducible** recordings.
3. Capture **device and sensor metadata** with every session.
4. Support **labelling** of sessions and segments for supervised learning.
5. Export data in formats that load directly into pandas/NumPy.

## 4. Non-Goals (v1)

- Cloud sync, accounts, or a backend server.
- On-device ML inference or real-time analytics.
- iOS or wearable support.
- Play Store release (sideload/internal testing is enough at first).
- Audio, camera or video recording (optional later module).

## 5. Target Users

| Persona                          | Need                                                                                             |
| -------------------------------- | ------------------------------------------------------------------------------------------------ |
| **Researcher/student (primary)** | Build datasets for AI/ML experiments (activity recognition, context sensing, signal processing). |
| **Developer**                    | Inspect which sensors a device offers, their real sampling rates and noise.                      |
| **Participant**                  | A friend or volunteer who records a session with minimal setup.                                  |

## 6. User Stories

- As a user, I can see every sensor on my phone with its specs so I know what can be recorded.
- As a user, I can choose which sensors to record and at what rate.
- As a user, I can start a recording that keeps running with the screen off.
- As a user, I can label what I am doing (walking, sitting, bus) before or during recording.
- As a user, I can view and manage past sessions (rename, delete, inspect size and duration).
- As a user, I can export a session as a ZIP and move it to my computer.
- As a researcher, I can trust timestamps and see actual sampling rates per sensor.

## 7. Functional Requirements

### 7.1 Sensor Discovery

| ID   | Requirement                                                                                                                              | Priority |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| F-01 | On first launch, enumerate all sensors via `SensorManager.getSensorList(TYPE_ALL)`.                                                      | P0       |
| F-02 | Show per sensor: name, vendor, type, version, max range, resolution, power (mA), min/max delay, FIFO size, wake-up flag, reporting mode. | P0       |
| F-03 | Mark dynamic and uncalibrated variants separately.                                                                                       | P1       |
| F-04 | Save a `device.json` snapshot (model, OS version, API level, build fingerprint, sensor list).                                            | P0       |

### 7.2 Recording

| ID   | Requirement                                                                                                                           | Priority |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| F-10 | Select any subset of sensors; "select all" shortcut.                                                                                  | P0       |
| F-11 | Choose sampling preset: Normal, UI, Game, Fastest, or custom Hz.                                                                      | P0       |
| F-12 | Record in a **foreground service** with a persistent notification (Stop / Pause / Add marker actions).                                | P0       |
| F-13 | Continue recording with screen off, using a partial wake lock.                                                                        | P0       |
| F-14 | Optional duration limit and optional start countdown.                                                                                 | P1       |
| F-15 | Record location (fused provider) at a configurable interval.                                                                          | P1       |
| F-16 | Record system signals: battery level/temperature/charging, thermal status, Wi-Fi RSSI, Bluetooth scan summary, ambient orientation.   | P2       |
| F-17 | Support multi-sensor batching (FIFO) where hardware supports it.                                                                      | P1       |
| F-18 | Survive screen rotation, app swipe-away, and low-memory kills as gracefully as possible (finalize files, mark session "interrupted"). | P0       |

### 7.3 Labelling and Notes

| ID   | Requirement                                                                       | Priority |
| ---- | --------------------------------------------------------------------------------- | -------- |
| F-20 | Session-level label and free-text notes.                                          | P0       |
| F-21 | Segment markers during recording (timestamped events, e.g. "started walking").    | P0       |
| F-22 | Reusable label presets editable by the user.                                      | P1       |
| F-23 | Capture context metadata: phone position (hand, pocket, bag, table), user-chosen. | P1       |

### 7.4 Session Management

| ID   | Requirement                                                                         | Priority |
| ---- | ----------------------------------------------------------------------------------- | -------- |
| F-30 | Session list with name, date, duration, size, sensors, label.                       | P0       |
| F-31 | Session detail: per-sensor sample count, actual mean rate, dropped-sample estimate. | P0       |
| F-32 | Rename, relabel, delete (single and bulk).                                          | P0       |
| F-33 | Mini preview chart for a chosen sensor (read-only, downsampled).                    | P2       |

### 7.5 Export

| ID   | Requirement                                                                               | Priority |
| ---- | ----------------------------------------------------------------------------------------- | -------- |
| F-40 | Export a session as ZIP: per-sensor CSV, `session.json`, `device.json`, `markers.csv`.    | P0       |
| F-41 | Share via Android share sheet or save to a user-chosen folder (Storage Access Framework). | P0       |
| F-42 | Optional Parquet export.                                                                  | P2       |
| F-43 | Bulk export of multiple sessions.                                                         | P1       |

### 7.6 Settings

| ID   | Requirement                                              | Priority |
| ---- | -------------------------------------------------------- | -------- |
| F-50 | Default sampling preset, default sensors, label presets. | P1       |
| F-51 | Storage usage display and auto-delete rules.             | P2       |
| F-52 | Theme (system/light/dark).                               | P2       |

## 8. Data Requirements

- **Timestamp model:** store raw `event.timestamp` (ns since boot, monotonic) and, per session, one `boot_to_wall_offset_ns` plus the wall-clock start time. All sensors share the same clock base, so alignment is exact.
- **Per-sample fields:** `t_ns`, `accuracy`, `values[0..n]`.
- **Per-session metadata:** id, name, label, notes, phone position, start/end wall time, status (complete / interrupted), app version, selected sensors and requested rates.
- **Quality stats (computed at stop):** sample count, mean/median/min/max interval, estimated dropped samples, per sensor.
- **Lossless:** no filtering, smoothing or down-sampling at collection time.

## 9. Non-Functional Requirements

| Area            | Requirement                                                                                   |
| --------------- | --------------------------------------------------------------------------------------------- |
| Reliability     | No data loss on normal stop; at most the last buffered batch (< 1 s) lost on a crash.         |
| Performance     | Sustain 100 Hz on ≥ 10 sensors simultaneously on a mid-range phone with < 1% dropped samples. |
| Battery         | Recording UI must not wake the CPU unnecessarily; show estimated battery drain per session.   |
| Storage         | Efficient writes (batched); warn when free space is below a threshold and auto-stop safely.   |
| Compatibility   | Min Android 8.0 (API 26); target latest stable API.                                           |
| Privacy         | All data stays on-device unless the user exports it. No analytics or network calls in v1.     |
| Usability       | Start a recording in ≤ 3 taps from launch.                                                    |
| Maintainability | Modular code, unit-tested data pipeline, CI on every push.                                    |

## 10. Permissions and Privacy

| Permission                                                                                  | Why                                    | When requested                  |
| ------------------------------------------------------------------------------------------- | -------------------------------------- | ------------------------------- |
| `FOREGROUND_SERVICE` + foreground service type `health`/`location`/`dataSync` as applicable | Continuous collection                  | Declared in manifest            |
| `POST_NOTIFICATIONS` (API 33+)                                                              | Recording notification                 | First recording                 |
| `ACTIVITY_RECOGNITION` (API 29+)                                                            | Step counter/detector                  | When those sensors are selected |
| `BODY_SENSORS` / newer health sensor permissions                                            | Heart-rate and similar                 | When selected                   |
| `HIGH_SAMPLING_RATE_SENSORS`                                                                | Rates above 200 Hz (API 31+)           | When user picks a high rate     |
| `ACCESS_FINE_LOCATION`                                                                      | GPS and Wi-Fi/BT scan info             | When location is enabled        |
| `ACCESS_BACKGROUND_LOCATION`                                                                | Only if needed for screen-off location | Avoid if possible               |
| `WAKE_LOCK`                                                                                 | Keep CPU awake while recording         | Manifest                        |

Principles: request permissions just in time, explain why, never block unrelated features, and let the user delete all data at any time. Location and Wi-Fi/Bluetooth data are sensitive; warn users before sharing exported files.

## 11. Success Metrics

- Sensor discovery works on ≥ 3 different phones.
- A 30-minute recording at 100 Hz with all sensors completes with < 1% dropped samples and no crash.
- Exported data loads into pandas in one command and aligns across sensors.
- At least 5 labelled sessions per activity class collected for the first analysis experiment.

## 12. Milestones

| Phase | Deliverable                                    | Rough effort |
| ----- | ---------------------------------------------- | ------------ |
| M0    | Project setup, CI, architecture skeleton       | 2-3 days     |
| M1    | Sensor discovery + device metadata screen      | 3-4 days     |
| M2    | Foreground service recording + file writer     | 1-1.5 weeks  |
| M3    | Session list/detail, labels, markers           | 1 week       |
| M4    | ZIP export + Python loader notebook            | 3-5 days     |
| M5    | Location/system signals, quality stats, polish | 1 week       |
| M6    | Multi-device testing, bug fixing, release APK  | 1 week       |

## 13. Risks and Mitigations

| Risk                                  | Impact              | Mitigation                                                                                                       |
| ------------------------------------- | ------------------- | ---------------------------------------------------------------------------------------------------------------- |
| OEM battery managers kill the service | Data gaps           | Foreground service, wake lock, in-app guidance to whitelist from battery optimization, mark interrupted sessions |
| Sensor rate differs from request      | Misleading analysis | Log actual intervals; report effective rate                                                                      |
| Dropped samples at high rates         | Bad data            | Lock-free queue, batched writer thread, quality stats                                                            |
| Permission friction on newer Android  | Users cannot record | Just-in-time requests with clear rationale                                                                       |
| Large file sizes                      | Storage pressure    | Binary/compact format option, storage warnings                                                                   |
| Privacy of location/Wi-Fi data        | Misuse              | Local-only storage, explicit consent before export                                                               |

## 14. Open Questions

1. What is the first analysis use case (activity recognition, gait, context, other)?
2. Which phones will be used for testing and comparison?
3. Is a compact binary format needed in addition to CSV?
4. Should the app support recording with a paired wearable later?
5. Will other people record data (needs consent flow)?
