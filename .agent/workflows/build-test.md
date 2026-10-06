# /build-test
Build, test, install, and check the app on the connected phone.

1. Run `./gradlew ktlintCheck testDebugUnitTest assembleDebug`.
2. If anything fails, show the first error, explain the cause in one or two sentences, fix it, and rerun. Stop and ask me if the same error persists after 3 attempts.
3. Run `adb devices`. If no device is listed, tell me and stop.
4. Run `./gradlew installDebug`.
5. Launch the app: `adb shell am start -n com.sensorlab/.MainActivity`.
6. Capture 20 seconds of logs for the app only: `adb logcat --pid=$(adb shell pidof -s com.sensorlab)`.
7. Summarize in a short list: build status, test status, install status, any crash or warning in logcat.
