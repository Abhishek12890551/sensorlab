# SensorLab project rules

Android app that records all available phone sensors into labelled sessions and exports them for Python analysis.

## Source of truth
- docs/01_PRD.md (requirements, IDs F-xx)
- docs/02_TECH_STACK.md (libraries, file formats)
- docs/03_DESIGN.md (architecture, data model, UI, build order)
Read the relevant doc sections before planning. If the docs conflict with a request, tell me instead of silently choosing.

## Stack
Kotlin, Jetpack Compose + Material 3, Hilt, Room, DataStore, WorkManager, Coroutines/Flow, Navigation Compose, kotlinx-serialization. minSdk 26. Gradle Kotlin DSL with version catalog (gradle/libs.versions.toml).

## Scope control
- Work on ONE milestone or requirement group per task.
- Do not add features, screens, or dependencies that are not in the docs without asking.
- Package layout follows docs/03_DESIGN.md section 2.
- Reference requirement IDs (e.g. F-12) in commit messages and code comments where useful.

## Code style
- Kotlin official style, ktlint must pass.
- Prefer small, testable classes. No business logic in Composables.
- UI observes StateFlow from ViewModels; ViewModels talk to repositories/controllers.
- No hardcoded user-facing strings; use string resources.
- No network permission or analytics in v1.
