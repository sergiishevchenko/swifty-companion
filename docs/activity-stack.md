# Activity Stack in Android Applications

## Overview

The Activity Stack (also known as the Back Stack) is a fundamental concept in Android that manages the navigation history of activities in an application. Understanding how the Activity Stack works is crucial for implementing proper navigation and managing the application lifecycle.

## What is the Activity Stack?

The Activity Stack is a Last-In-First-Out (LIFO) data structure that maintains the order of activities that the user has visited. When a user navigates through an app, each new activity is pushed onto the stack. When the user presses the back button, the current activity is popped from the stack, and the previous activity resumes.

## Stack Structure

```
┌─────────────────┐
│  Activity C     │  ← Top of stack (currently visible)
├─────────────────┤
│  Activity B     │
├─────────────────┤
│  Activity A     │  ← Bottom of stack (launcher activity)
└─────────────────┘
```

## Launch Modes

Android provides several launch modes that control how activities are added to the stack:

### 1. `standard` (Default)
- Creates a new instance of the activity every time it's launched
- Multiple instances can exist in the stack
- Each instance can have different data

```xml
<activity
    android:name=".MyActivity"
    android:launchMode="standard" />
```

### 2. `singleTop`
- If the activity is already at the top of the stack, reuses it instead of creating a new instance
- If it's not at the top, creates a new instance
- Useful for activities that can receive multiple intents (e.g., notification handlers)

```xml
<activity
    android:name=".MyActivity"
    android:launchMode="singleTop" />
```

### 3. `singleTask`
- Ensures only one instance of the activity exists in the entire task
- If an instance already exists, brings it to the front and clears all activities above it
- Useful for main activities (e.g., home screen)

```xml
<activity
    android:name=".MyActivity"
    android:launchMode="singleTask" />
```

### 4. `singleInstance`
- Similar to `singleTask`, but the activity is the only activity in its task
- No other activities can be launched into the same task
- Rarely used, typically for launcher activities

```xml
<activity
    android:name=".MyActivity"
    android:launchMode="singleInstance" />
```

## Task Affinity

Task affinity determines which task an activity belongs to. By default, all activities in an app have the same affinity (the app's package name).

```xml
<activity
    android:name=".MyActivity"
    android:taskAffinity="com.example.customtask" />
```

## Back Button Behavior

When the user presses the back button:
1. The current activity is finished (destroyed)
2. The activity below it in the stack becomes visible
3. If the stack is empty, the app returns to the home screen

### Customizing Back Behavior

You can override `onBackPressed()` to customize back button behavior:

```kotlin
override fun onBackPressed() {
    if (shouldHandleBackPress()) {
        // Custom back handling
        handleCustomBack()
    } else {
        super.onBackPressed()
    }
}
```

In Android 13+ (API 33+), use `OnBackPressedDispatcher`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (shouldHandleBackPress()) {
                handleCustomBack()
            } else {
                finish()
            }
        }
    })
}
```

## Managing the Stack

### Clearing the Stack

To clear all activities above the current one:

```kotlin
val intent = Intent(this, TargetActivity::class.java)
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
startActivity(intent)
finish()
```

### Finishing Activities

```kotlin
// Finish current activity
finish()

// Finish with result
setResult(Activity.RESULT_OK, intent)
finish()

// Finish all activities in the task
finishAffinity()
```

## Intent Flags

Intent flags control how activities are added to the stack:

- `FLAG_ACTIVITY_NEW_TASK`: Start activity in a new task
- `FLAG_ACTIVITY_CLEAR_TOP`: If activity exists, clear all activities above it
- `FLAG_ACTIVITY_SINGLE_TOP`: Same as `singleTop` launch mode
- `FLAG_ACTIVITY_CLEAR_TASK`: Clear entire task before starting activity
- `FLAG_ACTIVITY_NO_HISTORY`: Don't add activity to stack

```kotlin
val intent = Intent(this, TargetActivity::class.java)
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
startActivity(intent)
```

## Activity Lifecycle and Stack

The Activity Stack affects the lifecycle callbacks:

1. **Activity pushed onto stack**: `onCreate()` → `onStart()` → `onResume()`
2. **Another activity pushed on top**: `onPause()` → `onStop()`
3. **Activity popped from stack**: `onRestart()` → `onStart()` → `onResume()` (if resumed) or `onDestroy()` (if finished)

## Best Practices

1. **Use appropriate launch modes**: Choose the right launch mode for each activity based on its purpose
2. **Handle back navigation**: Implement proper back button handling for better UX
3. **Avoid deep stacks**: Deep navigation stacks can cause memory issues
4. **Use tasks wisely**: Don't create unnecessary tasks
5. **Test navigation**: Always test back button behavior thoroughly

## Common Patterns

### Single Activity Architecture

Modern Android apps often use a single activity with multiple fragments or Compose screens:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NavGraph()
        }
    }
}
```

This approach:
- Simplifies stack management
- Reduces memory usage
- Provides better control over navigation

### Deep Linking

Handle deep links while managing the stack:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val uri = intent.data
    if (uri != null) {
        // Handle deep link
        navigateToDeepLink(uri)
    }
}
```

## Debugging the Stack

View the current activity stack using ADB:

```bash
adb shell dumpsys activity activities
```

Or in code:

```kotlin
val taskInfo = activityManager.getRunningTasks(1)[0]
Log.d("Stack", "Top activity: ${taskInfo.topActivity}")
```

## References

- [Android Developer Guide: Tasks and Back Stack](https://developer.android.com/guide/components/activities/tasks-and-back-stack)
- [Activity Launch Modes](https://developer.android.com/guide/components/activities/tasks-and-back-stack#TaskLaunchModes)
- [Managing Tasks](https://developer.android.com/guide/components/activities/tasks-and-back-stack#ManagingTasks)
