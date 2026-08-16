# COVID Health Checker

[![Android CI](https://github.com/avinashbawane27/Covid-Health-Checker/actions/workflows/android-ci.yml/badge.svg)](https://github.com/avinashbawane27/Covid-Health-Checker/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An Android app that estimates two vital signs — **heart rate** and **respiratory rate** — using only phone hardware (camera and accelerometer), and combines them with a self-reported symptom checklist to build a simple COVID-19 symptom log.

- **App name (as installed):** COVID Diagnostics
- **Package:** `com.example.covidsymptoms`
- **Platform:** Android (Java), minSdk 23, targetSdk/compileSdk 31
- **License:** [MIT](LICENSE)

## How it works

### Heart rate (camera-based photoplethysmography)
1. The user records a 45-second video with a finger covering the rear camera lens (`MainPage.startRecording()`), saved to `finger_tip.mp4` on external storage.
2. `HeartRateService` splits the video into nine 5-second windows and, using JavaCV/FFmpeg (`FFmpegFrameGrabber`), extracts frames from each window on a thread pool.
3. For every frame, `getAverageColor()` samples the average red channel value (every 5th pixel) — blood flow through the fingertip changes light absorption, producing a periodic redness signal.
4. The redness signal is denoised with a moving average and run through a zero-crossing peak-finding algorithm (`denoise()` / `peakFinding()` in `MainPage`) to estimate beats per minute.

### Respiratory rate (accelerometer-based)
1. The user places the phone on their abdomen for 45 seconds.
2. `AccelerometerService` samples the device's X-axis accelerometer (`Sensor.TYPE_ACCELEROMETER`) for 300 readings, capturing the rise and fall of the abdomen while breathing.
3. `BreathingRateDetector` denoises the signal and runs the same zero-crossing peak-finding algorithm to estimate breaths per minute.

### Symptom log
`SymptomsScreen` lets the user rate ten symptoms (fever, cough, tiredness, shortness of breath, muscle aches, chills, sore throat, runny nose, headache, chest pain) on a rating bar via a spinner, then saves the full record — computed vitals plus symptom ratings and a timestamp — to a local SQLite database.

## Screens

| Screen | Class | Purpose |
|---|---|---|
| Main / launcher | `MainPage` | Record video, trigger heart-rate/respiratory-rate measurement, view results, navigate to symptoms |
| Symptoms | `SymptomsScreen` | Rate symptoms and submit the full record to the database |

`MainPage` is the app's launcher activity (declared in `AndroidManifest.xml`); `SymptomsScreen` is reached from it via the "Upload Symptoms" button.

## Data storage

Data is persisted with a raw `SQLiteOpenHelper` (`DBHelper`), writing rows to a `Bawane` table with columns for heart rate, respiratory rate, each of the ten symptom ratings, and a timestamp. (A parallel Room database setup — `AppDatabase` / `UserInfoDao` / `UserInfo` entity — also exists in the codebase but is not currently wired into the active screens.)

CSV dumps of raw and denoised sensor/frame data are also written to external storage for each measurement (e.g. `x_values.csv`, `x_values_denoised.csv`, `finger_tip0.csv` … `finger_tip8.csv`) for debugging/inspection.

## Permissions

Declared in `AndroidManifest.xml`:
- `CAMERA` — recording the fingertip video
- `RECORD_AUDIO` — required by the system video-capture intent
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` — saving/reading the video and CSV files (`android:requestLegacyExternalStorage="true"` is set for compatibility with the legacy storage model)

The app requests these at launch via `MainPage.handlePermissions()`.

## Tech stack

- **Language:** Java
- **Build system:** Gradle 6.6.1 (via the Gradle Wrapper) with Android Gradle Plugin 4.0.1
- **Key libraries:**
  - [JavaCV / JavaCPP / FFmpeg](https://github.com/bytedeco/javacv) (`org.bytedeco`) — video frame extraction
  - [OpenCSV](http://opencsv.sourceforge.net/) — writing sensor/frame data to CSV
  - AndroidX Room (`androidx.room`) — present for a (currently unused) structured persistence layer
  - AndroidX AppCompat, ConstraintLayout, LocalBroadcastManager

## Project setup (isolated, project-local build)

This project uses the Gradle Wrapper so the build doesn't depend on any globally installed Gradle. It still needs to be pointed at an Android SDK and a compatible JDK.

1. **Android SDK** — set `sdk.dir` in a local, gitignored `local.properties` file at the project root (never commit this file — it's machine-specific):
   ```properties
   sdk.dir=/absolute/path/to/your/Android/sdk
   ```
2. **JDK** — Gradle 6.6.1 / AGP 4.0.1 require **JDK 8 or 11** (they do not run on newer JDKs like 17/19+). If your system default JDK isn't 8/11, pin one for Gradle *without* touching your global `JAVA_HOME`, by adding this to your **machine-local** `~/.gradle/gradle.properties` (Gradle user home — outside this repo, never committed):
   ```properties
   org.gradle.java.home=/path/to/a/jdk-11 (or jdk-8)
   ```
   Do **not** put `org.gradle.java.home` in this repo's own `gradle.properties` — that file is committed and shared, and a hardcoded absolute JDK path would break the build for every other machine (including CI).
3. **Build:**
   ```bash
   ./gradlew :app:assembleDebug
   ```
   The Gradle Wrapper downloads its own pinned Gradle 6.6.1 distribution into `~/.gradle/wrapper`, so no separate Gradle install is required.

### Required SDK components
`compileSdkVersion 31` and `buildToolsVersion "30.0.2"` (or a compatible 30.x) must be installed via the Android SDK Manager / `sdkmanager`.

## Testing

Pure signal-processing logic (`denoise()` / `peakFinding()`, the moving-average and zero-crossing algorithms behind both vitals) is extracted into `SignalProcessingUtils`, a plain Java class with no Android framework dependency, specifically so it can be unit tested on the JVM without Robolectric or an emulator.

```bash
./gradlew :app:testDebugUnitTest
```

Tests live in `app/src/test/java/com/example/covidsymptoms/SignalProcessingUtilsTest.java` and cover: empty/null/short input handling, flat and periodic signals, moving-average correctness, and noise/spike smoothing.

## Continuous Integration

Every push and pull request runs through GitHub Actions (`.github/workflows/android-ci.yml`):
1. Lint (`:app:lintDebug`)
2. Unit tests (`:app:testDebugUnitTest`)
3. Debug APK assembly (`:app:assembleDebug`)

Build artifacts (APK, test results, lint report) are uploaded from each run.

## Known limitations

- Heart-rate and respiratory-rate estimates are derived from a simple zero-crossing peak-detection algorithm on noisy consumer-camera/accelerometer signals — they are **not clinically validated** and should not be used for medical decision-making.
- Both measurements require a steady 45-second recording; motion, poor lighting, or a loose finger placement will degrade accuracy.
- Requires a rear camera and an accelerometer; the app does not currently gate the relevant UI on hardware availability.

## Disclaimer

This is an academic/personal project exploring smartphone-sensor-based vital sign estimation. It is **not a medical device** and is not intended to diagnose, treat, or provide medical advice for COVID-19 or any other condition.
