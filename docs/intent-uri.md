# Project Notes

This document contains useful information and notes about the project architecture, patterns, and implementation details.

---

## Intent and Uri

### Intent

`Intent` is an Android component that represents an intention to perform an action. It's used for:
- Starting activities
- Passing data between components
- Deep linking
- Inter-component communication

#### Common Intent Actions

- `Intent.ACTION_VIEW` - View data (e.g., open URL in browser or app)
- `Intent.ACTION_SEND` - Send data to another app
- `Intent.ACTION_MAIN` - Main entry point of an app

#### Intent Structure

```kotlin
Intent(
    action: String,        // What action to perform
    uri: Uri? = null       // Data URI (optional)
)
```

#### Intent Properties

- `intent.data` - Contains the `Uri` associated with the Intent
- `intent.action` - The action to be performed
- `intent.extras` - Bundle containing additional data
- `intent.flags` - Flags that control how the Intent is handled

### Uri

`Uri` (Uniform Resource Identifier) represents a reference to a resource. In Android, it's commonly used for:
- Deep linking
- Passing data via Intent
- Parsing query parameters
- Identifying resources

#### Uri Structure

```
scheme://authority/path?query#fragment

Example:
students42://oauth/callback?code=abc123&state=xyz
```

Components:
- **Scheme**: `students42` - Identifies the protocol
- **Authority**: `oauth` - Identifies the domain/authority
- **Path**: `/callback` - Path to the resource
- **Query**: `code=abc123&state=xyz` - Query parameters
- **Fragment**: Optional fragment identifier

#### Uri Parsing

```kotlin
val uri = Uri.parse("students42://oauth/callback?code=abc123")

// Get query parameters
val code = uri.getQueryParameter("code")  // Returns "abc123"
val state = uri.getQueryParameter("state") // Returns null

// Get scheme
val scheme = uri.scheme  // Returns "students42"

// Get authority
val authority = uri.authority  // Returns "oauth"

// Get path
val path = uri.path  // Returns "/callback"

// Build URI with encoded parameters
val encoded = Uri.encode("value with spaces")  // Returns "value%20with%20spaces"
```

### Intent and Uri Interaction

Intent and Uri work together to enable deep linking and data passing:

#### 1. Creating Intent with Uri

```kotlin
// Create Intent with ACTION_VIEW and Uri
val authUrl = "https://api.intra.42.fr/oauth/authorize?client_id=xxx"
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
startActivity(intent)
```

#### 2. Receiving Intent with Uri

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Get Uri from Intent
    val uri: Uri? = intent.data
    
    if (uri != null) {
        // Process the Uri
        val code = uri.getQueryParameter("code")
    }
}
```

#### 3. Handling New Intent (Deep Link)

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)  // Update the current intent
    
    val uri = intent.data
    if (uri != null) {
        // Handle deep link
        handleDeepLink(uri)
    }
}
```

### Use Cases in This Project

#### OAuth Flow

1. **Starting OAuth**: Create Intent with OAuth URL
   ```kotlin
   fun startOAuthFlow(): Intent {
       val authUrl = buildAuthUrl()
       return Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
   }
   ```

2. **Receiving OAuth Callback**: Extract code from callback Uri
   ```kotlin
   fun handleOAuthCallback(uri: Uri): String? {
       return uri.getQueryParameter("code")
   }
   ```

3. **Deep Link Handling**: MainActivity receives callback Uri
   ```kotlin
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       NavGraph(initialUri = intent.data)  // Pass Uri to navigation
   }
   ```

#### Deep Linking Pattern

```kotlin
// AndroidManifest.xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="students42" />
</intent-filter>

// In Activity
override fun onCreate(savedInstanceState: Bundle?) {
    val uri = intent.data  // students42://oauth/callback?code=xxx
    if (uri != null && uri.scheme == "students42") {
        handleDeepLink(uri)
    }
}
```

### Best Practices

1. **Always check for null**: `intent.data` can be null
   ```kotlin
   val uri = intent.data ?: return
   ```

2. **Use Uri.encode()**: When building URIs with user input
   ```kotlin
   val encoded = Uri.encode(userInput)
   val url = "https://example.com?param=$encoded"
   ```

3. **Handle onNewIntent()**: For activities with `launchMode="singleTop"`
   ```kotlin
   override fun onNewIntent(intent: Intent) {
       super.onNewIntent(intent)
       setIntent(intent)  // Important: update current intent
   }
   ```

4. **Validate Uri scheme**: Check scheme before processing
   ```kotlin
   if (uri.scheme == "students42") {
       // Process deep link
   }
   ```

5. **Use Intent flags**: Control how Intent is handled
   ```kotlin
   intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
   ```

### Common Patterns

#### Pattern 1: OAuth Callback

```kotlin
// 1. Start OAuth flow
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl))
startActivity(intent)

// 2. Browser redirects to: students42://oauth/callback?code=xxx

// 3. App receives callback
override fun onCreate(savedInstanceState: Bundle?) {
    val uri = intent.data
    val code = uri?.getQueryParameter("code")
    // Exchange code for token
}
```

#### Pattern 2: Passing Data via Intent

```kotlin
// Sender
val intent = Intent(this, TargetActivity::class.java).apply {
    data = Uri.parse("students42://profile/user123")
}
startActivity(intent)

// Receiver
val uri = intent.data
val userId = uri?.pathSegments?.get(1)  // "user123"
```

#### Pattern 3: Result Intent

```kotlin
// Activity A starts Activity B for result
val intent = Intent(this, ActivityB::class.java)
startActivityForResult(intent, REQUEST_CODE)

// Activity B returns result
val resultIntent = Intent().apply {
    data = Uri.parse("students42://result?success=true")
}
setResult(RESULT_OK, resultIntent)
finish()

// Activity A receives result
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (resultCode == RESULT_OK) {
        val uri = data?.data
        val success = uri?.getQueryParameter("success")
    }
}
```

---

## Additional Notes

*More sections will be added here as the project evolves.*

