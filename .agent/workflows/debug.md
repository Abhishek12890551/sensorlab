# /debug
Diagnose a build error, crash, or wrong behaviour before changing code.

1. Ask me for the error text or symptom if I did not paste it.
2. Reproduce it: run the failing Gradle task, or `adb logcat -d` filtered to the app, or the relevant `adb shell dumpsys` command.
3. State the most likely cause and the evidence for it (file, line, log lines).
4. Propose the smallest fix. If the fix touches the recording pipeline or timestamp logic, explain the impact and wait for my approval.
5. Apply the fix, add a regression test if practical, and run /build-test.
