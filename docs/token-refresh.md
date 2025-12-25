# Automatic Token Refresh Implementation

## Overview

This document describes how automatic token refresh is implemented in the application. The application automatically refreshes expired access tokens using refresh tokens, ensuring seamless operation without user intervention.

## Implementation

### Architecture

The token refresh mechanism consists of three main components:

1. **TokenRepository** - Manages token storage and expiration checking
2. **AuthInterceptor** - Intercepts HTTP requests and automatically refreshes tokens
3. **ApiService** - Provides the refresh token API endpoint

**Note:** `AuthService` is **NOT** used for automatic token refresh. The `AuthInterceptor` implements its own refresh logic directly using `TokenRepository` and `ApiService`. However, `AuthService` functions can be used for manual token management and demonstration purposes.

### Token Storage

Tokens are stored in Android DataStore with the following structure:

```kotlin
// TokenRepository.kt
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

**Key Points:**
- Access token is stored as a string
- Expiration time is calculated: `currentTime + expiresIn * 1000` (converts seconds to milliseconds)
- Expiration time is stored as a timestamp (Long)

### Expiration Checking

```kotlin
// TokenRepository.kt
suspend fun isTokenExpired(): Boolean {
    val expiresAt = context.dataStore.data.map { preferences ->
        preferences[tokenExpiresAtKey]
    }.first()

    return expiresAt != null && expiresAt < System.currentTimeMillis()
}
```

**How it works:**
- Retrieves the stored expiration timestamp
- Compares it with current system time
- Returns `true` if token has expired

### Automatic Refresh Mechanism

The `AuthInterceptor` implements a two-level protection system:

#### Level 1: Proactive Refresh (Before Request)

Before each HTTP request, the interceptor checks if the token is expired and refreshes it if needed:

```kotlin
// AuthInterceptor.kt
val token = try {
    runBlocking {
        val tokenValue = tokenRepository.getTokenSync()
        val isExpired = tokenRepository.isTokenExpired()
        
        if (isExpired && tokenValue != null) {
            refreshTokenIfNeeded()
            tokenRepository.getTokenSync()
        } else {
            tokenValue
        }
    }
} catch (e: Exception) {
    null
}
```

**Flow:**
1. Get current access token
2. Check if token is expired
3. If expired, refresh the token
4. Get the new token
5. Add token to request header

#### Level 2: Reactive Refresh (On 401 Response)

If a request fails with 401 (Unauthorized), the interceptor attempts to refresh the token and retry the request:

```kotlin
// AuthInterceptor.kt
var response = chain.proceed(requestBuilder.build())

if (response.code == 401) {
    lock.withLock {
        val refreshed = runBlocking {
            refreshTokenIfNeeded()
        }

        if (refreshed) {
            val newToken = runBlocking {
                tokenRepository.getValidToken()
            }

            if (newToken != null) {
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                response.close()
                response = chain.proceed(newRequest)
            } else {
                tokenRepository.clearToken()
            }
        } else {
            tokenRepository.clearToken()
        }
    }
}
```

**Flow:**
1. Request fails with 401
2. Acquire lock to prevent concurrent refresh attempts
3. Attempt to refresh token
4. If successful, retry original request with new token
5. If failed, clear tokens (user needs to re-authenticate)

### Refresh Token Logic

```kotlin
// AuthInterceptor.kt
private suspend fun refreshTokenIfNeeded(): Boolean {
    val refreshToken = tokenRepository.getRefreshToken() ?: return false
    return try {
        val apiService = apiServiceProvider.get()
        val response = apiService.refreshToken(
            grantType = "refresh_token",
            clientId = clientId,
            clientSecret = clientSecret,
            refreshToken = refreshToken
        )
        tokenRepository.saveToken(
            token = response.accessToken,
            expiresIn = response.expiresIn
        )
        response.refreshToken?.let {
            tokenRepository.saveRefreshToken(it)
        }
        true
    } catch (e: Exception) {
        false
    }
}
```

**Key Features:**
- Uses refresh token to get new access token
- Saves new access token with expiration time
- Updates refresh token if provided by API
- Returns `true` on success, `false` on failure

### Thread Safety

The implementation uses `ReentrantLock` to prevent race conditions:

```kotlin
private val lock = ReentrantLock()

if (response.code == 401) {
    lock.withLock {
        // Refresh token logic
    }
}
```

**Why it's needed:**
- Multiple requests might receive 401 simultaneously
- Without locking, multiple refresh attempts could occur
- Lock ensures only one refresh happens at a time

### Error Handling

The application handles various error scenarios:

1. **Token expired before request**: Automatically refreshed
2. **401 response**: Token refreshed and request retried
3. **Refresh token missing**: Tokens cleared, user redirected to login
4. **Refresh API failure**: Tokens cleared, user redirected to login
5. **Network error during refresh**: Tokens cleared, user redirected to login

## How to Demonstrate

### Method 1: Wait for Natural Expiration

**Steps:**
1. Log in to the application
2. Wait for the access token to expire (typically 2 hours for 42 API)
3. Make any API request (e.g., search for a user)
4. Observe that the request succeeds without requiring re-authentication

**What happens:**
- Token expires
- Next API request triggers automatic refresh
- Request completes successfully with new token
- User experience is seamless

### Method 2: Simulate Expiration (Development)

To test immediately, you can temporarily modify the expiration check:

```kotlin
// TokenRepository.kt - TEMPORARY MODIFICATION FOR TESTING
suspend fun isTokenExpired(): Boolean {
    val expiresAt = context.dataStore.data.map { preferences ->
        preferences[tokenExpiresAtKey]
    }.first()

    // Force expiration for testing (remove after testing)
    return true  // Always return true to simulate expiration
    
    // Original code:
    // return expiresAt != null && expiresAt < System.currentTimeMillis()
}
```

**Steps:**
1. Modify `isTokenExpired()` to always return `true`
2. Build and run the application
3. Log in to get tokens
4. Make any API request
5. Observe automatic token refresh in logs
6. Request should succeed
7. **Important**: Revert the modification after testing

### Method 3: Monitor Logs

Enable HTTP logging to see token refresh in action:

```kotlin
// NetworkModule.kt
fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}
```

**What to look for:**
1. First request with expired token → 401 response
2. Automatic refresh token request to `/oauth/token`
3. Retry of original request with new token → 200 response

### Method 4: Force 401 Response

You can test the 401 handling by temporarily using an invalid token:

```kotlin
// TokenRepository.kt - TEMPORARY MODIFICATION FOR TESTING
suspend fun getTokenSync(): String? {
    // Force invalid token for testing (remove after testing)
    return "invalid_token"
    
    // Original code:
    // return context.dataStore.data.map { preferences ->
    //     preferences[accessTokenKey]
    // }.first()
}
```

**Steps:**
1. Modify `getTokenSync()` to return invalid token
2. Make an API request
3. Observe 401 response
4. Observe automatic refresh attempt
5. Request should be retried with new valid token
6. **Important**: Revert the modification after testing

### Method 5: ADB Commands

You can manually expire the token using ADB:

```bash
# Connect to device
adb shell

# Navigate to app data
run-as com.students42.app

# Edit DataStore (requires root or debug build)
# Or use ADB to clear app data
adb shell pm clear com.students42.app
```

Then log in again and wait for expiration.

### Expected Behavior

**Normal Flow:**
1. User logs in → Access token and refresh token stored
2. User uses app → Requests work normally
3. Token expires → Next request automatically refreshes token
4. User continues using app → No interruption

**Error Flow:**
1. Token expires
2. Refresh attempt fails (network error, invalid refresh token, etc.)
3. Tokens are cleared
4. User is redirected to login screen
5. User logs in again

## AuthService Functions for Demonstration

**Important:** `AuthService` is **NOT** used in the automatic token refresh mechanism. The `AuthInterceptor` implements its own refresh logic. However, `AuthService` provides functions that can be used for manual token management and demonstration:

### Available Functions

1. **`isTokenValid(): Boolean`**
   - Checks if the current access token is still valid
   - Returns `true` if token exists and hasn't expired
   - Can be used to verify token status before making requests

2. **`refreshTokenIfNeeded(): Boolean`**
   - Manually refreshes the token if needed
   - Returns `true` if refresh was successful, `false` otherwise
   - Can be used to explicitly refresh token before important operations

3. **`refreshToken(refreshToken: String): Result<TokenResponse>`**
   - Low-level function to refresh token with a specific refresh token
   - Returns `Result<TokenResponse>` with new token data
   - Used internally by `refreshTokenIfNeeded()`

### Usage in Code

Currently, `AuthService` is only used in `LoginViewModel` for OAuth flow:
- `startOAuthFlow()` - Starts OAuth authorization
- `getToken(code)` - Exchanges authorization code for tokens
- `saveTokenResponse()` - Saves tokens after OAuth

**Not used for automatic refresh:**
- `isTokenValid()` - Not used anywhere
- `refreshTokenIfNeeded()` - Not used anywhere (AuthInterceptor has its own implementation)

### For Demonstration

To demonstrate token refresh functionality, you can use `AuthService` functions:

```kotlin
// Check token validity
val isValid = authService.isTokenValid()

// Manually refresh token
val refreshed = authService.refreshTokenIfNeeded()
if (refreshed) {
    // Token was successfully refreshed
} else {
    // Refresh failed - user needs to re-authenticate
}
```

However, in normal operation, all token refresh happens automatically via `AuthInterceptor` without using `AuthService`.

## Code Locations

- **TokenRepository**: `app/src/main/java/com/students42/app/data/local/TokenRepository.kt`
- **AuthInterceptor**: `app/src/main/java/com/students42/app/data/api/AuthInterceptor.kt`
- **ApiService**: `app/src/main/java/com/students42/app/data/api/ApiService.kt`
- **AuthService**: `app/src/main/java/com/students42/app/auth/AuthService.kt`
- **NetworkModule**: `app/src/main/java/com/students42/app/di/NetworkModule.kt`

## Key Design Decisions

1. **Two-level protection**: Proactive (before request) and reactive (on 401) refresh ensures maximum reliability
2. **Thread-safe refresh**: Lock prevents concurrent refresh attempts
3. **Automatic cleanup**: Failed refresh clears tokens to prevent infinite loops
4. **Transparent to user**: All refresh happens automatically without user awareness
5. **Interceptor pattern**: Centralized token management for all API requests
