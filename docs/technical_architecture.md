# Technical Architecture

This document breaks down the technical architecture, packages, and core layers of the PCM Player Converter application.

## High-Level Architecture

The application follows the **Model-View-ViewModel (MVVM)** architectural pattern combined with **Clean Architecture** principles.

```mermaid
graph TD
    UI[UI Layer (Jetpack Compose)] --> VM[Presentation Layer (ViewModels)]
    VM --> UC[Domain Layer (Use Cases)]
    UC --> Repo[Domain Repository Interface]
    Repo --> RepoImpl[Data Layer (Repository Implementation)]
    RepoImpl --> Local[Local Data Source / File System]
    RepoImpl --> Service[Conversion Foreground Service]
```

## Layer Breakdown

### 1. UI Layer (`com.rajatnagpure.pcmplayerconverter.ui`)
Built entirely using **Jetpack Compose**.
- **Screens**: `ConverterScreen`, `GeneratorScreen`, `NeedHelpScreen`.
- **Navigation**: `AppNavigation` manages routing and deep-link intent handling.
- **Components**: Reusable UI elements (`AppButton`, `AppCard`, `AudioConfigSelector`, `AppDialog`, `AudioPlayerSheet`).

### 2. Presentation Layer (`com.rajatnagpure.pcmplayerconverter.ui.viewmodel`)
State holders that manage the UI state and handle user interactions.
- **`MainViewModel`**: Manages global state such as the active media player visibility, current playing file, and playback progress.
- **`ConverterViewModel`**: Manages the state for converting standard audio files (WAV, MP3, etc.) to raw PCM format.
- **`GeneratorViewModel`**: Manages the state for generating/recording PCM audio and configuring audio parameters (Sample Rate, Channels, Encoding).

### 3. Domain Layer (`com.rajatnagpure.pcmplayerconverter.domain`)
Contains the core business logic.
- **Models**: `AudioConfig` representing the format configurations.
- **Repository Interface**: `AudioRepository` defines the contract for audio operations.
- **Use Cases**: `ConvertPcmUseCase` encapsulates the logic to execute a conversion job.

### 4. Data & Service Layer (`com.rajatnagpure.pcmplayerconverter.data` / `.service`)
Handles actual data processing, hardware interaction, and background execution.
- **`AudioRepositoryImpl`**: Concrete implementation of the data operations.
- **`LocalFileDataSource`**: Manages file read/write operations.
- **`PcmPlayer` & `PcmRecorder`**: Interfaces directly with Android's `AudioTrack` and `AudioRecord` APIs to play and capture raw audio bytes.
- **`PcmConverter`, `AudioEncoder`, `AudioDecoder`**: Low-level utility classes responsible for decoding compressed audio formats to PCM and encoding PCM back to WAV/compressed formats using `MediaCodec` and `MediaExtractor`.
- **`ConversionService`**: An Android Foreground Service (`foregroundServiceType="dataSync"`) that ensures long-running conversions continue even if the user minimizes the app.

## Dependency Injection

The project uses **Hilt** for dependency injection.
- **`AppModule`**: Provides instances of `AudioRepository`, `LocalFileDataSource`, etc., scoped appropriately (usually `@Singleton` for repositories).
- **`BaseApplication`**: Annotated with `@HiltAndroidApp` to bootstrap the DI graph.
