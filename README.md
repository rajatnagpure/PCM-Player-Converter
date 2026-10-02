# PCM Player & Converter

An Android app for working with raw **PCM** audio:
- Play raw PCM files.
- Convert PCM to WAV, M4A (AAC) or FLAC.
- Convert other audio files to PCM.
- Record new PCM audio.

[Get it on Google Play](https://play.google.com/store/apps/details?id=com.rajatnagpure.pcmplayerconverter)

## Features

- **Converter tab**: pick or share a `.pcm` file, choose the sample rate, channels, encoding and output format, then play it or convert it.
- **Generator tab**: record from the microphone to PCM, or convert any audio file (WAV, MP3, M4A, …) to PCM.
- Conversions run in a foreground service. When one finishes, the app shows **"Conversion completed"** both on screen and as a notification. Tap the notification to open the file.
- Open files straight from other apps with **Share** or **Open with**.
- Neumorphic UI with four themes and optional haptics. See [DESIGN_LANGUAGE.md](DESIGN_LANGUAGE.md).
- Privacy-friendly analytics with an in-app opt-out (Settings → Privacy).

## Requirements

| Tool | Version |
|---|---|
| JDK | 21 (Android Studio's bundled JBR, or any installed JDK 21) |
| Android SDK | compileSdk / targetSdk 36, minSdk 23 |
| Gradle | 9.8 (via the wrapper) |
| AGP / Kotlin | 9.4 / 2.4 |

All versions are defined in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

### "Gradle requires JVM 17 or later to run"

You'll see this error when `java` / `JAVA_HOME` in your terminal points to an older JDK, such as Java 8.

[`gradle/gradle-daemon-jvm.properties`](gradle/gradle-daemon-jvm.properties) tells Gradle to run its daemon on **JDK 21**, so `./gradlew` works from any shell **as long as a JDK 21 is installed somewhere Gradle can find it**. Gradle auto-detects JDKs in `/Library/Java/JavaVirtualMachines` and `~/Library/Java/JavaVirtualMachines`.

If Gradle can't find a JDK 21, do one of these:

- Point your shell at Android Studio's bundled JDK by adding this line to `~/.zshrc`:
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  ```
- Or tell Gradle where the JDK is, in `~/.gradle/gradle.properties`:
  ```properties
  org.gradle.java.installations.paths=/Applications/Android Studio.app/Contents/jbr/Contents/Home
  ```

## Build and run

```bash
./gradlew installDebug
```

```bash
./gradlew assembleRelease
```

### Firebase (optional for local development)

Analytics, Crashlytics and Remote Config read their configuration from `app/google-services.json`.
- The file is **git-ignored**. Download your own copy from the Firebase console.
- **Without the file**, the app still builds and runs; analytics simply does nothing.
- **Debug builds send nothing** unless you build with `-PanalyticsInDebug=true`.

Full setup instructions, including Remote Config, key events, custom dimensions and the Play Data safety form, are in **[docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md)**.

Most of the console setup can be done from the terminal:
- `python3 scripts/firebase/ga4.py setup`: creates the key events and custom dimensions/metrics, and sets data retention, from `firebase/ga4_definitions.json`.
- `python3 scripts/firebase/ga4.py report`: prints the standard reports.
- `firebase deploy --only remoteconfig`: publishes the Remote Config parameters.

## Tests

Run the unit tests (JVM and Robolectric):

```bash
./gradlew testDebugUnitTest
```

Run the instrumented UI tests. These need a running emulator or a connected device:

```bash
./gradlew connectedDebugAndroidTest
```

Run lint:

```bash
./gradlew lintDebug
```

## Project docs

- [docs/technical_architecture.md](docs/technical_architecture.md): layers, dependency injection, analytics and the conversion pipeline
- [docs/navigation_map.md](docs/navigation_map.md): screens and how incoming files are routed
- [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md): Firebase console setup and the full list of analytics events
- [DESIGN_LANGUAGE.md](DESIGN_LANGUAGE.md): the neumorphic design system

## Contributing

Issues and pull requests are welcome. Feature requests can also go through the in-app **Request Feature** form.
