# OAuth2 Authentication Implementation

## Overview

This document describes the OAuth2 authorization code flow implementation in the application. The app uses OAuth2 to securely authenticate users with the 42 Intra API without storing user credentials.

## OAuth2 Basics

### What is OAuth2?

OAuth2 is an authorization framework that allows applications to obtain limited access to user accounts. Instead of sharing passwords, OAuth2 uses access tokens that grant specific permissions.

### Why OAuth2?

- **Security**: No password storage in the app
- **User Control**: Users can revoke access at any time
- **Limited Scope**: Apps only get requested permissions
- **Industry Standard**: Widely supported and secure

### OAuth2 Authorization Code Flow

The flow consists of these steps:

1. **Authorization Request** - App redirects user to authorization server
2. **User Authorization** - User logs in and grants permissions
3. **Authorization Grant** - Server redirects back with authorization code
4. **Token Exchange** - App exchanges code for access token
5. **API Access** - App uses access token for API requests

## Configuration

### Required Credentials

The application requires OAuth2 credentials configured in `local.properties`:

```properties
API_UID=your_client_id_here
API_SECRET=your_client_secret_here
API_REDIRECT_URI=students42://oauth/callback
```

### Getting Credentials

1. Go to https://profile.intra.42.fr/oauth/applications
2. Click "New application"
3. Configure:
   - **Name**: Your application name
   - **Redirect URI**: `students42://oauth/callback` (must match exactly)
   - **Scopes**: Select required permissions (typically "public")
4. Copy **UID** (Client ID) and **SECRET** (Client Secret)
5. Add to `local.properties`

### Deep Link Configuration

The app uses custom URL scheme `students42://` for OAuth callbacks:

```xml
<!-- AndroidManifest.xml -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="students42" />
</intent-filter>
```

**Redirect URI Format:**
```
students42://oauth/callback?code=AUTHORIZATION_CODE
```

## Implementation Architecture

### Components

1. **AuthService** - Core OAuth logic and token management
2. **OAuthCallbackActivity** - Handles OAuth callback from browser
3. **LoginScreen** - UI for OAuth flow initiation
4. **LoginViewModel** - Coordinates OAuth flow
5. **TokenRepository** - Secure token storage
6. **MainActivity** - Handles deep links

### Component Responsibilities

#### AuthService

**Location**: `app/src/main/java/com/students42/app/auth/AuthService.kt`

**Responsibilities:**
- Build OAuth authorization URL
- Create Intent for OAuth flow
- Extract authorization code from callback URI
- Exchange authorization code for tokens
- Save tokens securely

**Key Methods:**

```kotlin
// Start OAuth flow - creates Intent to open browser
fun startOAuthFlow(): Intent

// Extract authorization code from callback URI
fun handleOAuthCallback(uri: Uri): String?

// Exchange authorization code for access token
suspend fun getToken(code: String): Result<TokenResponse>

// Save tokens to secure storage
suspend fun saveTokenResponse(tokenResponse: TokenResponse)
```

#### OAuthCallbackActivity

**Location**: `app/src/main/java/com/students42/app/auth/OAuthCallbackActivity.kt`

**Responsibilities:**
- Receive OAuth callback from browser
- Extract callback URI
- Return result to calling activity
- Show loading indicator during processing

**Key Features:**
- `launchMode="singleTop"` - Prevents multiple instances
- Handles both `onCreate()` and `onNewIntent()`
- Returns result via `setResult()`

#### LoginScreen

**Location**: `app/src/main/java/com/students42/app/ui/login/LoginScreen.kt`

**Responsibilities:**
- Display OAuth authorization button
- Launch OAuth flow via ActivityResultLauncher
- Handle callback from OAuthCallbackActivity
- Handle deep links from MainActivity
- Navigate to profile after successful auth

**Key Features:**
- Uses `ActivityResultLauncher` for OAuth flow
- Handles `initialUri` parameter for deep links
- Shows loading state during token exchange

#### LoginViewModel

**Location**: `app/src/main/java/com/students42/app/ui/login/LoginViewModel.kt`

**Responsibilities:**
- Coordinate OAuth flow
- Handle authorization code exchange
- Manage login state
- Error handling and retry logic

## Complete OAuth Flow

### Step-by-Step Flow

#### 1. User Initiates OAuth

**Trigger**: User clicks "Authorize" button (shown when no token exists)

**Code Flow:**
```kotlin
// LoginScreen.kt
val oauthLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { result ->
    // Handle callback
}

Button(onClick = {
    val intent = viewModel.startOAuthFlow()
    oauthLauncher.launch(intent)
})
```

**What Happens:**
```kotlin
// AuthService.kt
fun startOAuthFlow(): Intent {
    val authUrl = buildAuthUrl()
    // authUrl = "https://api.intra.42.fr/oauth/authorize?client_id=xxx&redirect_uri=students42://oauth/callback&response_type=code&scope=public"
    return Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
}
```

**Result**: Browser opens with 42 Intra login page

#### 2. User Authorizes

**What Happens:**
- User enters credentials on 42 Intra website
- User grants permissions to the app
- 42 Intra server generates authorization code

**Server Response:**
```
students42://oauth/callback?code=AUTHORIZATION_CODE
```

#### 3. Browser Redirects to App

**Two Possible Paths:**

**Path A: OAuthCallbackActivity (Primary)**
```
Browser → OAuthCallbackActivity (via intent-filter)
```

**Path B: MainActivity (Deep Link)**
```
Browser → MainActivity (via intent-filter) → NavGraph → LoginScreen
```

#### 4. OAuthCallbackActivity Handles Callback

**Code Flow:**
```kotlin
// OAuthCallbackActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    handleCallback(intent)
}

private fun handleCallback(intent: Intent) {
    val uri: Uri? = intent.data
    // uri = "students42://oauth/callback?code=xxx"
    
    uri?.let {
        val resultIntent = Intent().apply {
            data = uri
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
```

**Result**: Activity finishes and returns URI to LoginScreen

#### 5. LoginScreen Receives Callback

**Code Flow:**
```kotlin
// LoginScreen.kt
val oauthLauncher = rememberLauncherForActivityResult(...) { result ->
    if (result.resultCode == RESULT_OK) {
        val uri = result.data?.data
        // uri = "students42://oauth/callback?code=xxx"
        val code = uri?.getQueryParameter("code")
        code?.let { authCode ->
            viewModel.handleOAuthCallback(authCode)
        }
    }
}
```

**Alternative: Deep Link Handling**
```kotlin
// LoginScreen.kt
LaunchedEffect(initialUri) {
    initialUri?.let { uri ->
        if (uri.scheme == "students42" && uri.host == "oauth") {
            val code = uri.getQueryParameter("code")
            code?.let { authCode ->
                viewModel.handleOAuthCallback(authCode)
            }
        }
    }
}
```

#### 6. Exchange Code for Token

**Code Flow:**
```kotlin
// LoginViewModel.kt
fun handleOAuthCallback(code: String) {
    viewModelScope.launch {
        _loginState.value = LoginState.Loading
        
        when (val result = authService.getToken(code)) {
            is Result.Success -> {
                authService.saveTokenResponse(result.data)
                checkToken()
            }
            is Result.Error -> {
                // Handle error
            }
        }
    }
}
```

**API Call:**
```kotlin
// AuthService.kt
suspend fun getToken(code: String): Result<TokenResponse> {
    val response = apiService.getToken(
        grantType = "authorization_code",
        clientId = clientId,
        clientSecret = clientSecret,
        code = code,
        redirectUri = redirectUri
    )
    return Result.Success(response)
}
```

**API Request:**
```
POST https://api.intra.42.fr/oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&client_id=CLIENT_ID
&client_secret=CLIENT_SECRET
&code=AUTHORIZATION_CODE
&redirect_uri=students42://oauth/callback
```

**API Response:**
```json
{
  "access_token": "ACCESS_TOKEN",
  "token_type": "Bearer",
  "expires_in": 7200,
  "refresh_token": "REFRESH_TOKEN",
  "scope": "public"
}
```

#### 7. Save Tokens

**Code Flow:**
```kotlin
// AuthService.kt
suspend fun saveTokenResponse(tokenResponse: TokenResponse) {
    tokenRepository.saveToken(
        token = tokenResponse.accessToken,
        expiresIn = tokenResponse.expiresIn
    )
    tokenResponse.refreshToken?.let {
        tokenRepository.saveRefreshToken(it)
    }
}
```

**Storage:**
- Access token stored in DataStore
- Refresh token stored in DataStore
- Expiration time calculated and stored

#### 8. User Can Now Use App

After successful token exchange:
- Tokens are saved
- User can search for profiles
- API requests include access token automatically

## Deep Link Handling

### MainActivity Deep Link Support

**Configuration:**
```xml
<!-- AndroidManifest.xml -->
<activity android:name=".MainActivity"
    android:launchMode="singleTop">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="students42" />
    </intent-filter>
</activity>
```

**Handling:**
```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    NavGraph(initialUri = intent.data)  // Pass URI to navigation
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.data != null) {
        recreate()  // Recreate to handle new URI
    }
}
```

**Why `singleTop`?**
- Prevents multiple MainActivity instances
- `onNewIntent()` called when activity already exists
- Allows handling deep links when app is already running

### LoginScreen Deep Link Processing

```kotlin
// LoginScreen.kt
LaunchedEffect(initialUri) {
    initialUri?.let { uri ->
        if (uri.scheme == "students42" && 
            uri.host == "oauth" && 
            uri.path == "/callback") {
            val code = uri.getQueryParameter("code")
            code?.let { authCode ->
                viewModel.handleOAuthCallback(authCode)
            }
        }
    }
}
```

## Error Handling

### Common Error Scenarios

#### 1. User Cancels Authorization

**What Happens:**
- User closes browser without authorizing
- No callback received
- App remains on login screen

**Handling:**
```kotlin
// OAuthCallbackActivity.kt
private fun handleCallback(intent: Intent) {
    val uri: Uri? = intent.data
    uri?.let {
        // Success path
    } ?: run {
        setResult(RESULT_CANCELED)  // User canceled
        finish()
    }
}
```

#### 2. Invalid Authorization Code

**What Happens:**
- Code expired or already used
- Token exchange fails
- API returns error

**Handling:**
```kotlin
// LoginViewModel.kt
when (val result = authService.getToken(code)) {
    is Result.Error -> {
        val errorMessage = ErrorHandler.handleError(context, result.exception)
        _loginState.value = LoginState.Error(
            errorMessage,
            retryAction = { handleOAuthCallback(code) }
        )
    }
}
```

#### 3. Network Error

**What Happens:**
- Network unavailable during token exchange
- Request timeout
- Connection error

**Handling:**
- Caught as exception in `getToken()`
- Wrapped in `Result.Error`
- Displayed to user with retry option

#### 4. Invalid Redirect URI

**What Happens:**
- Redirect URI doesn't match registered URI
- 42 Intra rejects authorization
- Error in callback

**Prevention:**
- Ensure redirect URI matches exactly: `students42://oauth/callback`
- Must be registered in 42 Intra OAuth application settings

## Security Considerations

### 1. Client Secret Storage

**Current Implementation:**
- Stored in `local.properties` (not in version control)
- Injected via Dagger Hilt
- Used only server-side (should be kept secret)

**Best Practice:**
- Never commit `local.properties` to version control
- Use `.gitignore` to exclude sensitive files
- Consider using Android Keystore for production

### 2. Token Storage

**Implementation:**
- Tokens stored in Android DataStore
- Encrypted at rest (Android system feature)
- App-specific storage (not accessible by other apps)

**Security:**
- Access tokens expire (typically 2 hours)
- Refresh tokens used for renewal
- Tokens cleared on refresh failure

### 3. Deep Link Validation

**Implementation:**
```kotlin
if (uri.scheme == "students42" && 
    uri.host == "oauth" && 
    uri.path == "/callback") {
    // Process callback
}
```

**Why Important:**
- Prevents malicious apps from intercepting callbacks
- Validates callback source
- Ensures correct OAuth flow

### 4. HTTPS Only

**Configuration:**
```xml
<application android:usesCleartextTraffic="false">
```

**Why:**
- All API communication over HTTPS
- Prevents man-in-the-middle attacks
- Protects tokens in transit

## Token Response Model

```kotlin
// TokenResponse.kt
data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    
    @SerializedName("token_type")
    val tokenType: String = "Bearer",
    
    @SerializedName("expires_in")
    val expiresIn: Int?,  // Seconds until expiration
    
    @SerializedName("refresh_token")
    val refreshToken: String?,
    
    val scope: String?
)
```

**Fields:**
- `accessToken` - Used for API authentication
- `tokenType` - Always "Bearer" for 42 API
- `expiresIn` - Token lifetime in seconds (typically 7200 = 2 hours)
- `refreshToken` - Used to get new access token
- `scope` - Granted permissions

## How to Demonstrate

### Method 1: Normal OAuth Flow

**Steps:**
1. Launch the application
2. If no token exists, "Authorize" button is shown
3. Click "Authorize" button
4. Browser opens with 42 Intra login page
5. Enter 42 Intra credentials
6. Grant permissions
7. Browser redirects back to app
8. App exchanges code for token
9. User can now search for profiles

**What to Observe:**
- Smooth transition from app to browser and back
- No password entry in app
- Tokens stored automatically
- App works after authorization

### Method 2: Deep Link Testing

**Using ADB:**
```bash
# Simulate OAuth callback
adb shell am start -a android.intent.action.VIEW \
  -d "students42://oauth/callback?code=test_code"
```

**What Happens:**
- MainActivity receives deep link
- NavGraph passes URI to LoginScreen
- LoginScreen extracts code
- Attempts token exchange (will fail with test code, but flow is visible)

### Method 3: Monitor Network Traffic

**Enable HTTP Logging:**
```kotlin
// NetworkModule.kt
fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}
```

**What to Look For:**
1. Authorization URL request
2. Token exchange POST request
3. Access token in subsequent API requests

### Method 4: Check Token Storage

**Using ADB:**
```bash
# Check if tokens are stored
adb shell run-as com.students42.app
cd shared_prefs
cat students42_preferences.xml
```

**What to See:**
- `access_token` value
- `token_expires_at` timestamp
- `refresh_token` value

### Method 5: Test Error Scenarios

**Cancel Authorization:**
1. Click "Authorize"
2. Close browser without logging in
3. Observe: App returns to login screen

**Invalid Code:**
1. Manually trigger callback with invalid code
2. Observe: Error message shown
3. Retry option available

**Network Error:**
1. Disable network
2. Attempt OAuth flow
3. Observe: Error handling and retry

## Code Flow Diagram

```
User clicks "Authorize"
    ↓
LoginScreen → viewModel.startOAuthFlow()
    ↓
AuthService.buildAuthUrl() → Creates OAuth URL
    ↓
Intent(ACTION_VIEW, authUrl) → Opens browser
    ↓
User authorizes on 42 Intra
    ↓
42 Intra redirects: students42://oauth/callback?code=xxx
    ↓
OAuthCallbackActivity receives callback
    ↓
OAuthCallbackActivity returns URI to LoginScreen
    ↓
LoginScreen extracts code from URI
    ↓
LoginViewModel.handleOAuthCallback(code)
    ↓
AuthService.getToken(code) → API call
    ↓
TokenResponse received
    ↓
AuthService.saveTokenResponse() → Save to DataStore
    ↓
User can now use app
```

## Key Files

- **AuthService**: `app/src/main/java/com/students42/app/auth/AuthService.kt`
- **OAuthCallbackActivity**: `app/src/main/java/com/students42/app/auth/OAuthCallbackActivity.kt`
- **LoginScreen**: `app/src/main/java/com/students42/app/ui/login/LoginScreen.kt`
- **LoginViewModel**: `app/src/main/java/com/students42/app/ui/login/LoginViewModel.kt`
- **MainActivity**: `app/src/main/java/com/students42/app/MainActivity.kt`
- **TokenRepository**: `app/src/main/java/com/students42/app/data/local/TokenRepository.kt`
- **TokenResponse**: `app/src/main/java/com/students42/app/data/models/TokenResponse.kt`
- **AndroidManifest**: `app/src/main/AndroidManifest.xml`

## Best Practices

1. **Always validate redirect URI** - Prevent callback hijacking
2. **Handle all error cases** - Network, invalid code, user cancellation
3. **Store tokens securely** - Use DataStore, never in SharedPreferences
4. **Clear tokens on failure** - Prevent using invalid tokens
5. **Use HTTPS only** - Protect tokens in transit
6. **Validate deep links** - Check scheme, host, and path
7. **Handle onNewIntent()** - For singleTop activities
8. **Show loading states** - Better UX during token exchange


## References

- [OAuth2 Specification](https://oauth.net/2/)
- [42 Intra API Documentation](https://api.intra.42.fr/)
- [Android Deep Links](https://developer.android.com/training/app-links/deep-linking)
- [Android DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
