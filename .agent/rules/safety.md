# Safety and data-quality rules

## Recording pipeline (critical)
- No allocations, logging, or file I/O inside SensorEventListener.onSensorChanged.
- Sensor callbacks run on a dedicated HandlerThread, never the main thread.
- Sample hand-off uses pre-allocated ring buffers; overflow increments a dropped counter, never blocks.
- Writing happens in batches on Dispatchers.IO; flush about every 500 ms.
- Store raw event.timestamp as t_ns (nanoseconds, elapsedRealtime base). Do NOT convert, smooth, filter, or resample at collection time.
- Do NOT change timestamp handling, file formats, or the boot_to_wall_offset logic without asking me first.
- Recording must run in a foreground service with a persistent notification and a partial wake lock.
- On stop or interruption: flush, close files, compute quality stats, update session.json and Room.

## Verification
- After every code change run: ./gradlew ktlintCheck testDebugUnitTest assembleDebug
- Do not claim something works until the command output shows it passed.
- Add or update unit tests for any change to buffers, writers, timestamp math, or stats.

## Commands and files
- Ask me before: deleting files or folders, git push/force/reset/rebase, adb shell commands that change device settings, adding dependencies, editing the Gradle wrapper or signing config.
- Show the exact command and why before running it.
- Never read or print the contents of keystores, local.properties secrets, or exported session data.
- Do not commit build/, .gradle/, or *.apk files.
