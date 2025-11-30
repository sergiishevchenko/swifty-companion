# 42 Students Mobile App

Mobile application for retrieving information about 42 students through 42 API using OAuth2 authentication.

## Technology Stack

- **Language**: Kotlin
- **Platform**: Native Android
- **UI**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **HTTP Client**: Retrofit + OkHttp
- **Dependency Injection**: Hilt
- **Data Storage**: DataStore
- **Navigation**: Navigation Component
- **Image Loading**: Coil

## Project Setup

### 1. Clone and Open Project

1. Open the project in Android Studio
2. Wait for Gradle synchronization

### 2. Configure OAuth2 Credentials

Create a `local.properties` file in the project root (already added to `.gitignore`):

```properties
API_UID=your_client_id_here
API_SECRET=your_client_secret_here
API_REDIRECT_URI=students42://oauth/callback
API_BASE_URL=https://api.intra.42.fr
```

**Important**: Get these credentials by registering an OAuth2 application on [42 Intra](https://profile.intra.42.fr/oauth/applications)

### 3. Configure OAuth2 Application on 42 Intra

1. Go to https://profile.intra.42.fr/oauth/applications
2. Create a new application
3. Set Redirect URI: `students42://oauth/callback`
4. Copy `UID` and `SECRET` to `local.properties`

### 4. Build and Run

1. Sync the project (File → Sync Project with Gradle Files)
2. Connect an Android device or start an emulator
3. Press Run (Shift+F10)

## Project Structure

```
app/src/main/java/com/students42/app/
├── data/
│   ├── models/          # Data models (UserModel, SkillModel, ProjectModel)
│   ├── api/             # Retrofit API interfaces
│   ├── local/           # Local storage (DataStore)
│   └── repositories/    # Data repositories
├── domain/
│   ├── usecases/        # Business logic use cases
│   └── repository/      # Repository interfaces
├── ui/
│   ├── login/           # Login screen
│   ├── profile/         # Profile screen
│   └── components/      # Reusable UI components
├── auth/                # OAuth2 authentication
├── utils/               # Utilities and constants
└── di/                  # Dependency Injection modules (Hilt)
```

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 24+ (Android 7.0)
- Target SDK 34 (Android 14)

## License

This project is created for educational purposes as part of 42 School.
