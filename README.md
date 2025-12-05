# 42 Students Mobile App

Mobile application for retrieving information about 42 students through 42 API using OAuth2 authentication.

## Features

- ✅ **OAuth2 Authentication** - Secure login via 42 Intra OAuth2
- ✅ **User Profile** - View detailed information about students (login, email, level, location, wallet, evaluations)
- ✅ **Skills Display** - View student skills with levels and progress indicators
- ✅ **Projects List** - View completed and failed projects with visual distinction
- ✅ **Error Handling** - Comprehensive error handling with retry mechanism
- ✅ **Responsive Design** - Adaptive layout for different screen sizes and orientations
- ✅ **Token Management** - Automatic token refresh on expiration
- ✅ **Material Design 3** - Modern UI following Material Design 3 guidelines

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
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Project Setup

### 1. Clone and Open Project

1. Open the project in Android Studio
2. Wait for Gradle synchronization

### 2. Configure OAuth2 Credentials

Create a `local.properties` file in the project root

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

## Screens

### Login Screen
- OAuth2 authentication flow
- User search by login
- Error handling with retry mechanism
- Adaptive layout for different screen sizes

### Profile Screen
- User information card (login, email, mobile, level, location, wallet, evaluations)
- Profile picture with circular image
- Skills list with progress indicators
- Projects list with visual distinction (green for completed, red for failed)
- Landscape support with two-column layout for tablets
- Error handling with retry dialog

## Project Structure

```
app/src/main/java/com/students42/app/
├── auth/
│   ├── AuthService.kt           # OAuth2 authentication service
│   └── OAuthCallbackActivity.kt # OAuth callback handler
├── data/
│   ├── models/                  # Data models
│   │   ├── UserModel.kt
│   │   ├── SkillModel.kt
│   │   ├── ProjectModel.kt
│   │   └── TokenResponse.kt
│   ├── api/
│   │   ├── ApiService.kt        # Retrofit API interface
│   │   └── AuthInterceptor.kt  # Token injection & auto-refresh
│   ├── local/
│   │   └── TokenRepository.kt  # DataStore token management
│   └── repositories/
│       └── UserRepository.kt    # User data repository
├── ui/
│   ├── login/
│   │   ├── LoginScreen.kt
│   │   ├── LoginViewModel.kt
│   │   └── LoginState.kt
│   ├── profile/
│   │   ├── ProfileScreen.kt
│   │   ├── ProfileViewModel.kt
│   │   └── ProfileState.kt
│   └── components/
│       ├── UserInfoCard.kt
│       ├── SkillsList.kt
│       └── ProjectsList.kt
├── utils/
│   ├── Constants.kt
│   ├── ErrorHandler.kt          # Centralized error handling
│   └── Result.kt               # Sealed class for results
└── di/
    ├── AppModule.kt
    ├── AuthModule.kt
    └── NetworkModule.kt        # Retrofit & OkHttp setup
```

## Key Features Implementation

### Error Handling
- Centralized error handling through `ErrorHandler`
- User-friendly error messages
- Retry mechanism for failed requests
- Support for network errors, 401, 404, 500, and other HTTP errors

### Responsive Design
- Adaptive layouts using `BoxWithConstraints`
- Support for portrait and landscape orientations
- Two-column layout for tablets in landscape mode
- Window insets handling for system bars
- Material Design 3 components

### Token Management
- Automatic token refresh on expiration via `AuthInterceptor`
- Thread-safe token refresh with `ReentrantLock`
- Seamless token refresh without user interruption
- Token storage in DataStore with expiration tracking

## Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture:

- **Model**: Data models and repositories
- **View**: Jetpack Compose UI screens
- **ViewModel**: State management and business logic
- **Repository**: Data layer abstraction
- **Use Cases**: Business logic (optional, can be added)

### Data Flow

1. User interacts with UI (Compose Screen)
2. ViewModel handles user actions
3. Repository fetches data from API
4. API requests go through AuthInterceptor (token injection)
5. Response flows back through Repository → ViewModel → UI
6. UI updates reactively via StateFlow

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 24+ (Android 7.0)
- Target SDK 34 (Android 14)

## License

This project is created for educational purposes as part of 42 School.
