# AuthInterceptor Documentation

## Overview

`AuthInterceptor` is an OkHttp interceptor that automatically manages OAuth2 access tokens for all API requests. It handles token injection, expiration detection, automatic token refresh, and 401 Unauthorized response handling.

**Location**: `app/src/main/java/com/students42/app/data/api/AuthInterceptor.kt`

## Purpose

The interceptor provides transparent authentication for all API calls by:
- Automatically adding `Authorization: Bearer <token>` header to requests
- Detecting expired tokens before making requests
- Automatically refreshing expired tokens
- Handling 401 Unauthorized responses by refreshing and retrying
- Preventing concurrent refresh attempts with thread-safe locking

## Architecture

### Dependencies

```kotlin
class AuthInterceptor(
    private val tokenRepository: TokenRepository,
    private val apiServiceProvider: Provider<ApiService>,
    private val clientId: String,
    private val clientSecret: String
) : Interceptor
```

**Dependencies:**
- `TokenRepository` - Manages token storage and retrieval
- `ApiServiceProvider` - Provides ApiService for token refresh (circular dependency handled via Provider)
- `clientId` - OAuth2 client ID
- `clientSecret` - OAuth2 client secret

### Thread Safety

Uses `ReentrantLock` to prevent concurrent token refresh attempts:

```kotlin
private val lock = ReentrantLock()
```

This ensures only one refresh operation happens at a time, preventing:
- Multiple simultaneous refresh requests
- Race conditions in token updates
- Unnecessary API calls

## Request Flow

### Step 1: Request Interception

When any API request is made, `intercept()` is called:

```kotlin
override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val url = originalRequest.url.toString()
```

### Step 2: OAuth Endpoint Bypass

OAuth token endpoints are excluded from authentication:

```kotlin
if (url.contains("/oauth/token")) {
    return chain.proceed(originalRequest)
}
```

**Why?** Token refresh endpoint doesn't need authentication - it uses refresh token in request body.

### Step 3: Token Retrieval and Expiration Check

```kotlin
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

**Process:**
1. Get current access token synchronously
2. Check if token is expired
3. If expired, attempt refresh
4. Get refreshed token (or original if refresh failed)
5. Handle exceptions gracefully (return null)

**Why `runBlocking`?** OkHttp interceptors are synchronous, but token operations are suspend functions. `runBlocking` bridges the gap.

### Step 4: Add Authorization Header

```kotlin
val requestBuilder = originalRequest.newBuilder()

if (token != null) {
    requestBuilder.header("Authorization", "Bearer $token")
}
```

Adds `Authorization: Bearer <token>` header if token exists.

### Step 5: Execute Request

```kotlin
var response = chain.proceed(requestBuilder.build())
```

Sends the request with authorization header.

### Step 6: Handle 401 Unauthorized

If server returns 401, attempt token refresh and retry:

```kotlin
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
                runBlocking {
                    tokenRepository.clearToken()
                }
            }
        } else {
            runBlocking {
                tokenRepository.clearToken()
            }
        }
    }
}
```

**Process:**
1. Lock to prevent concurrent refresh
2. Attempt token refresh
3. If successful, retry original request with new token
4. If failed, clear tokens (user needs to re-authenticate)
5. Close original response to free resources

**Why lock?** Multiple requests might receive 401 simultaneously. Lock ensures only one refresh happens.

## Token Refresh Implementation

### Private Method: `refreshTokenIfNeeded()`

```kotlin
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

**Process:**
1. Get refresh token from repository
2. Call API to refresh access token
3. Save new access token and expiration
4. Save new refresh token if provided
5. Return success/failure

**Error Handling:** Returns `false` on any exception (network error, invalid refresh token, etc.)

## Integration with OkHttp

### NetworkModule Configuration

```kotlin
@Provides
@Singleton
fun provideOkHttpClient(
    authInterceptor: AuthInterceptor,
    loggingInterceptor: HttpLoggingInterceptor
): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()
}
```

**Interceptor Order:**
1. `authInterceptor` - Adds auth header, handles refresh
2. `loggingInterceptor` - Logs requests/responses

**Why this order?** Auth interceptor must run first to add headers before logging.

## Key Features

### 1. Automatic Token Injection

Every API request automatically gets `Authorization: Bearer <token>` header added.

**Example:**
```
GET /v2/users/123
Authorization: Bearer c43132a90d222adfd000393d8d8d9f42e6194130a96f90e0c7bf4c3789eeb1e2
```

### 2. Proactive Token Refresh

Tokens are refreshed **before** expiration is detected, preventing unnecessary 401 errors.

**Flow:**
1. Request comes in
2. Check if token expired
3. If expired, refresh immediately
4. Use refreshed token for request

### 3. Reactive Token Refresh

If proactive refresh fails or token expires between check and request, handle 401 response:

**Flow:**
1. Request sent with expired token
2. Server returns 401
3. Interceptor catches 401
4. Refresh token
5. Retry original request

### 4. Thread-Safe Refresh

`ReentrantLock` prevents multiple simultaneous refresh attempts:

```kotlin
lock.withLock {
    // Only one thread can execute this block at a time
    refreshTokenIfNeeded()
}
```

**Scenario:**
- Request A gets 401, starts refresh
- Request B gets 401, waits for lock
- Request A completes refresh
- Request B uses refreshed token

### 5. Automatic Token Cleanup

If refresh fails, tokens are cleared:

```kotlin
runBlocking {
    tokenRepository.clearToken()
}
```

**Why?** Prevents infinite refresh loops and forces user to re-authenticate.

### 6. OAuth Endpoint Exclusion

Token refresh endpoint bypasses authentication:

```kotlin
if (url.contains("/oauth/token")) {
    return chain.proceed(originalRequest)
}
```

**Why?** Token refresh doesn't need access token - it uses refresh token in body.

## Error Scenarios

### Scenario 1: No Token Available

**What happens:**
- `getTokenSync()` returns `null`
- Request sent without `Authorization` header
- Server may return 401
- No refresh attempted (no refresh token)

**Result:** Request fails, user needs to authenticate

### Scenario 2: Token Expired, Refresh Succeeds

**What happens:**
1. Token expired detected
2. Refresh token available
3. Refresh API call succeeds
4. New token saved
5. Request retried with new token

**Result:** Request succeeds transparently

### Scenario 3: Token Expired, Refresh Fails

**What happens:**
1. Token expired detected
2. Refresh attempted
3. Refresh fails (network error, invalid refresh token)
4. Tokens cleared
5. Request fails

**Result:** User needs to re-authenticate

### Scenario 4: 401 Received, Refresh Succeeds

**What happens:**
1. Request sent with token
2. Server returns 401 (token invalid/expired)
3. Refresh token available
4. Refresh succeeds
5. Original request retried with new token

**Result:** Request succeeds after retry

### Scenario 5: 401 Received, Refresh Fails

**What happens:**
1. Request sent with token
2. Server returns 401
3. Refresh attempted
4. Refresh fails
5. Tokens cleared

**Result:** Request fails, user needs to re-authenticate

### Scenario 6: Concurrent Requests with 401

**What happens:**
1. Request A gets 401, acquires lock, starts refresh
2. Request B gets 401, waits for lock
3. Request A completes refresh
4. Request B uses refreshed token, retries

**Result:** Both requests succeed, only one refresh call made

## Circular Dependency Handling

### Problem

`AuthInterceptor` needs `ApiService` to refresh tokens, but `ApiService` is created with `OkHttpClient` that includes `AuthInterceptor`.

### Solution

Use `Provider<ApiService>` instead of direct `ApiService`:

```kotlin
private val apiServiceProvider: Provider<ApiService>
```

**Why it works:**
- `Provider` is lazy - doesn't create instance until `get()` is called
- By the time refresh is needed, `ApiService` is already created
- Breaks circular dependency

**Usage:**
```kotlin
val apiService = apiServiceProvider.get()
val response = apiService.refreshToken(...)
```

## Best Practices

### 1. Always Use Lock for Refresh

```kotlin
lock.withLock {
    refreshTokenIfNeeded()
}
```

Prevents race conditions and duplicate refresh calls.

### 2. Close Response Before Retry

```kotlin
response.close()
response = chain.proceed(newRequest)
```

Frees resources from original failed response.

### 3. Clear Tokens on Refresh Failure

```kotlin
if (!refreshed) {
    tokenRepository.clearToken()
}
```

Prevents infinite refresh loops.

### 4. Handle Exceptions Gracefully

```kotlin
try {
    // token operations
} catch (e: Exception) {
    null  // or clear tokens
}
```

Network errors shouldn't crash the app.

### 5. Exclude OAuth Endpoints

```kotlin
if (url.contains("/oauth/token")) {
    return chain.proceed(originalRequest)
}
```

Token refresh endpoint doesn't need authentication.

## Testing Considerations

### Unit Testing

Mock dependencies:
- `TokenRepository` - Control token state
- `ApiService` - Control refresh responses
- `Interceptor.Chain` - Verify request modifications

### Integration Testing

Test scenarios:
1. Valid token - request succeeds
2. Expired token - refresh and retry
3. Invalid refresh token - tokens cleared
4. Network error during refresh - graceful failure
5. Concurrent requests - thread safety

### Manual Testing

1. **Normal flow:** Make API call with valid token
2. **Expired token:** Wait for token expiration, make API call
3. **Invalid token:** Clear token, make API call (should fail gracefully)
4. **Network error:** Disable network, attempt refresh

## Performance Considerations

### Synchronous Operations

`runBlocking` is used to bridge async token operations with sync interceptor:

```kotlin
runBlocking {
    tokenRepository.getTokenSync()
}
```

**Impact:** Blocks thread during token operations. Usually fast (< 10ms), but can block on network refresh.

### Lock Contention

Multiple requests waiting for refresh lock:

**Mitigation:**
- Refresh happens quickly (single API call)
- Lock is held only during refresh
- Most requests don't need refresh

### Token Caching

Tokens are cached in memory by `TokenRepository` (DataStore), so repeated calls are fast.

## Comparison with Alternative Approaches

### Alternative 1: Manual Token Management

**Without interceptor:**
- Each API call must manually add token
- Each call must check expiration
- Each call must handle 401

**With interceptor:**
- Automatic token injection
- Centralized refresh logic
- Transparent to calling code

### Alternative 2: Token Refresh in Repository

**Without interceptor:**
- Repository handles refresh
- Each API call checks token
- Duplicate refresh logic

**With interceptor:**
- Single refresh point
- Automatic retry on 401
- Centralized error handling

## Code Examples

### Basic Usage

No code changes needed - interceptor works automatically:

```kotlin
// In ViewModel or Repository
suspend fun getUserInfo(login: String): UserModel {
    return apiService.getUserInfo(login)  // Token added automatically
}
```

### Handling Authentication Errors

```kotlin
try {
    val user = apiService.getUserInfo(login)
} catch (e: HttpException) {
    if (e.code() == 401) {
        // Token refresh failed, user needs to re-authenticate
        navigateToLogin()
    }
}
```

### Checking Token State

```kotlin
val token = tokenRepository.getToken().first()
if (token == null) {
    // User not authenticated
    navigateToLogin()
}
```

## References

- [OkHttp Interceptors](https://square.github.io/okhttp/interceptors/)
- [OAuth2 Token Refresh](https://oauth.net/2/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [ReentrantLock Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantLock.html)
