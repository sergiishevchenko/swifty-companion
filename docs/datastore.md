# DataStore

## Overview

**DataStore** is a modern data storage solution for Android applications, introduced by Google as a replacement for SharedPreferences. It provides a type-safe, asynchronous, and reactive way to store key-value pairs or typed objects.

## What is DataStore?

DataStore is part of the Android Jetpack library that offers two implementations:

1. **Preferences DataStore** - Stores key-value pairs (similar to SharedPreferences)
2. **Proto DataStore** - Stores typed objects using Protocol Buffers

This project uses **Preferences DataStore** for storing authentication tokens and related data.

## Architecture

### Core Components

1. **DataStore Interface**
   - Generic interface: `DataStore<T>`
   - For Preferences: `DataStore<Preferences>`
   - Provides reactive data access via Kotlin Flow

2. **Preferences Keys**
   - Type-safe keys for different data types:
     - `stringPreferencesKey()` - for String values
     - `intPreferencesKey()` - for Int values
     - `longPreferencesKey()` - for Long values
     - `booleanPreferencesKey()` - for Boolean values
     - `floatPreferencesKey()` - for Float values
     - `doublePreferencesKey()` - for Double values

3. **Data Access**
   - Asynchronous operations using Kotlin Coroutines
   - Reactive data streams using Kotlin Flow
   - Transactional updates with `edit {}` block

### How It Works

```
┌─────────────────┐
│   Application   │
└────────┬────────┘
         │
         │ uses
         ▼
┌─────────────────┐
│  TokenRepository│
└────────┬────────┘
         │
         │ accesses
         ▼
┌─────────────────┐      ┌──────────────┐
│  DataStore      │─────▶│  Preferences │
│  (Preferences)  │      │  (Key-Value) │
└─────────────────┘      └──────────────┘
         │
         │ stores in
         ▼
┌─────────────────┐
│  Internal       │
│  Storage        │
│  (File System)  │
└─────────────────┘
```

## Implementation in This Project

### Setup

DataStore is initialized as an extension property on `Context`:

```kotlin
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.DATASTORE_NAME
)
```

This creates a singleton DataStore instance that persists data to a file named `students42_preferences.preferences_pb` in the app's internal storage.

### Usage in TokenRepository

The project uses DataStore to store authentication tokens:

```16:21:app/src/main/java/com/students42/app/data/local/TokenRepository.kt
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

class TokenRepository(private val context: Context) {
    private val accessTokenKey = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)
    private val tokenExpiresAtKey = longPreferencesKey(Constants.KEY_TOKEN_EXPIRES_AT)
    private val refreshTokenKey = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)
```

### Writing Data

Data is written using the `edit {}` block, which provides transactional updates:

```23:31:app/src/main/java/com/students42/app/data/local/TokenRepository.kt
    suspend fun saveToken(token: String, expiresIn: Int? = null) {
        context.dataStore.edit { preferences ->
            preferences[accessTokenKey] = token
            if (expiresIn != null) {
                val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)
                preferences[tokenExpiresAtKey] = expiresAt
            }
        }
    }
```

The `edit {}` block:
- Is a suspend function (must be called from a coroutine)
- Provides a `MutablePreferences` object for modifications
- Automatically commits changes when the block completes
- Is thread-safe and handles concurrent access

### Reading Data

Data can be read in two ways:

#### 1. Reactive Flow (Observable)

Returns a Flow that emits updates whenever data changes:

```39:43:app/src/main/java/com/students42/app/data/local/TokenRepository.kt
    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[accessTokenKey]
        }
    }
```

This is useful for UI components that need to react to data changes.

#### 2. Synchronous Read (One-time)

Reads the current value once:

```45:49:app/src/main/java/com/students42/app/data/local/TokenRepository.kt
    suspend fun getTokenSync(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[accessTokenKey]
        }.first()
    }
```

Uses `.first()` to get the first emission from the Flow.

### Removing Data

Data is removed within the `edit {}` block:

```65:71:app/src/main/java/com/students42/app/data/local/TokenRepository.kt
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(tokenExpiresAtKey)
            preferences.remove(refreshTokenKey)
        }
    }
```

## Key Features

### 1. Type Safety

Keys are strongly typed, preventing runtime errors:

```kotlin
private val accessTokenKey = stringPreferencesKey("access_token")
// Can only store String values
preferences[accessTokenKey] = "token123"  // ✅ Valid
preferences[accessTokenKey] = 123         // ❌ Compile error
```

### 2. Asynchronous Operations

All operations are suspend functions, ensuring they don't block the main thread:

```kotlin
suspend fun saveToken(token: String) {
    context.dataStore.edit { preferences ->
        preferences[accessTokenKey] = token
    }
}
```

### 3. Reactive Updates

DataStore exposes data as Flow, enabling reactive programming:

```kotlin
fun getToken(): Flow<String?> {
    return context.dataStore.data.map { preferences ->
        preferences[accessTokenKey]
    }
}

// In ViewModel or Composable
viewModelScope.launch {
    tokenRepository.getToken().collect { token ->
        // React to token changes
    }
}
```

### 4. Transactional Updates

The `edit {}` block ensures atomic updates:

```kotlin
context.dataStore.edit { preferences ->
    preferences[accessTokenKey] = newToken
    preferences[tokenExpiresAtKey] = expiresAt
    // Both updates happen atomically
}
```

### 5. Thread Safety

DataStore handles concurrent access automatically. Multiple coroutines can read and write simultaneously without explicit synchronization.

## Advantages Over SharedPreferences

| Feature | SharedPreferences | DataStore |
|---------|------------------|-----------|
| **API** | Synchronous (blocking) | Asynchronous (non-blocking) |
| **Type Safety** | Runtime errors possible | Compile-time type safety |
| **Reactive** | Manual listeners | Kotlin Flow support |
| **Error Handling** | Exceptions can crash app | Handles errors gracefully |
| **Thread Safety** | Requires manual synchronization | Built-in thread safety |
| **Data Consistency** | Can be inconsistent | Transactional updates |
| **Main Thread** | Can block UI | Never blocks UI |

## Storage Location

DataStore stores data in the app's internal storage directory:

```
/data/data/com.students42.app/files/datastore/students42_preferences.preferences_pb
```

This location is:
- Private to the app (other apps cannot access it)
- Automatically backed up (if app backup is enabled)
- Cleared when the app is uninstalled

## Error Handling

DataStore operations can throw `IOException` if disk I/O fails. Always wrap operations in try-catch:

```kotlin
try {
    context.dataStore.edit { preferences ->
        preferences[accessTokenKey] = token
    }
} catch (e: IOException) {
    // Handle error
}
```

## Migration from SharedPreferences

If migrating from SharedPreferences, DataStore provides a migration mechanism:

```kotlin
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "preferences",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context,
                "old_preferences_name"
            )
        )
    }
)
```

## Best Practices

1. **Use suspend functions** - All DataStore operations are suspend functions
2. **Handle errors** - Wrap operations in try-catch blocks
3. **Use Flow for reactive updates** - Prefer `Flow` over one-time reads when data might change
4. **Group related data** - Store related data together in the same DataStore
5. **Use meaningful key names** - Define keys as constants for reusability
6. **Avoid blocking operations** - Never use `runBlocking` in production code

## Project-Specific Usage

In this project, DataStore is used exclusively for token management:

- **Access Token** - OAuth2 access token for API authentication
- **Token Expiration** - Timestamp when the token expires
- **Refresh Token** - OAuth2 refresh token for obtaining new access tokens

All token operations are handled through `TokenRepository`, which provides a clean abstraction over DataStore operations.

## Dependencies

The project uses the following dependency:

```kotlin
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

## References

- [Android DataStore Documentation](https://developer.android.com/topic/libraries/architecture/datastore)
- [Preferences DataStore Guide](https://developer.android.com/codelabs/android-preferences-datastore)
- [Kotlin Flow Documentation](https://kotlinlang.org/docs/flow.html)
