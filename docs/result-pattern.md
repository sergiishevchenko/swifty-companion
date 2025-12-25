# Result Pattern in Android

## Overview

The `Result` pattern is a functional programming approach for handling operations that can succeed, fail, or be in progress. It provides a type-safe way to represent the outcome of asynchronous operations without using exceptions for control flow.

## Definition

```kotlin
// Result.kt
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

### Key Characteristics

- **Sealed Class**: Exhaustive when expressions - compiler ensures all cases are handled
- **Generic Type**: `Result<out T>` - covariant generic allows `Result<Nothing>` to be used as any `Result<T>`
- **Three States**: Success, Error, and Loading
- **Type Safety**: Compile-time guarantees about handling all possible outcomes

## Result States

### 1. Success

Represents a successful operation with data.

```kotlin
data class Success<T>(val data: T) : Result<T>()
```

**Properties:**
- `data: T` - The successful result data
- Generic type `T` matches the expected return type

**Example:**
```kotlin
val result: Result<UserModel> = Result.Success(userModel)
val user = result.data  // Access the data
```

### 2. Error

Represents a failed operation with exception information.

```kotlin
data class Error(val exception: Throwable) : Result<Nothing>()
```

**Properties:**
- `exception: Throwable` - The exception that caused the failure
- `Result<Nothing>` - Can be used as any `Result<T>` due to covariance

**Example:**
```kotlin
val result: Result<UserModel> = Result.Error(NetworkException("Connection failed"))
val exception = result.exception  // Access the exception
```

### 3. Loading

Represents an operation in progress.

```kotlin
object Loading : Result<Nothing>()
```

**Properties:**
- Singleton object (no data)
- Indicates operation is ongoing
- Used for UI loading states

**Example:**
```kotlin
val result: Result<UserModel> = Result.Loading
// No data available yet
```

## Usage Patterns

### Pattern 1: Repository Layer

Repositories return `Flow<Result<T>>` to emit state changes:

```kotlin
// UserRepository.kt
fun getUserInfo(login: String): Flow<Result<UserModel>> = flow {
    emit(Result.Loading)  // Emit loading state
    try {
        val user = withContext(Dispatchers.IO) {
            apiService.getUserInfo(login)
        }
        emit(Result.Success(user))  // Emit success with data
    } catch (e: Exception) {
        emit(Result.Error(e))  // Emit error with exception
    }
}
```

**Flow:**
1. Emit `Loading` immediately
2. Perform async operation
3. Emit `Success` with data or `Error` with exception

### Pattern 2: ViewModel Layer

ViewModels collect `Flow<Result<T>>` and transform to UI state:

```kotlin
// ProfileViewModel.kt
userRepository.getUserInfo(login).collect { userResult ->
    when (userResult) {
        is Result.Loading -> {
            _profileState.value = ProfileState.Loading
        }
        is Result.Success -> {
            val userId = userResult.data.id
            loadUserData(userId, userResult.data)
        }
        is Result.Error -> {
            val errorMessage = ErrorHandler.handleError(context, userResult.exception)
            _profileState.value = ProfileState.Error(
                errorMessage,
                retryAction = { performLoadUserProfile(login) }
            )
        }
    }
}
```

**Key Points:**
- Exhaustive `when` expression handles all cases
- Smart casting: `userResult.data` is accessible in `Success` branch
- Error handling with user-friendly messages

### Pattern 3: Direct Result Handling

For non-Flow operations, handle Result directly:

```kotlin
// AuthService.kt
suspend fun getToken(code: String): Result<TokenResponse> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiService.getToken(...)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

// Usage
when (val result = authService.getToken(code)) {
    is Result.Success -> {
        authService.saveTokenResponse(result.data)
    }
    is Result.Error -> {
        val errorMessage = ErrorHandler.handleError(context, result.exception)
        // Handle error
    }
    else -> {}
}
```

### Pattern 4: Filtering Results

Wait for final result (Success or Error, skip Loading):

```kotlin
// ProfileViewModel.kt
val skillsResult = async {
    try {
        userRepository.getUserSkills(userId)
            .first { it is Result.Success || it is Result.Error }
    } catch (e: Exception) {
        Result.Error(e) as Result<List<SkillModel>>
    }
}
```

**Why:** Skip intermediate `Loading` states when waiting for final result.

### Pattern 5: Extracting Data

Safely extract data with fallback:

```kotlin
// ProfileViewModel.kt
val skillsFromApi = when (val result = skillsResult.await()) {
    is Result.Success -> result.data
    else -> emptyList()  // Fallback to empty list on error
}
```

**Alternative with extension function:**
```kotlin
fun <T> Result<T>.getDataOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

val skills = skillsResult.await().getDataOrNull() ?: emptyList()
```

## Advantages

### 1. Type Safety

Compiler enforces handling all cases:

```kotlin
when (result) {
    is Result.Success -> { /* must handle */ }
    is Result.Error -> { /* must handle */ }
    is Result.Loading -> { /* must handle */ }
    // Compiler error if any case is missing
}
```

### 2. No Exception-Based Control Flow

Errors are values, not exceptions:

```kotlin
// Good: Error as value
val result: Result<UserModel> = getUserInfo()
when (result) {
    is Result.Error -> handleError(result.exception)
    // ...
}

// Bad: Exception for control flow
try {
    val user = getUserInfo()  // Throws exception
} catch (e: Exception) {
    handleError(e)
}
```

### 3. Explicit Error Handling

Errors are part of the type signature:

```kotlin
// Function signature shows it can fail
fun getUserInfo(): Flow<Result<UserModel>>

// vs. hidden exceptions
fun getUserInfo(): Flow<UserModel>  // Can throw, but not obvious
```

### 4. Composable

Easy to chain and transform:

```kotlin
fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

fun <T> Result<T>.onError(action: (Throwable) -> Unit): Result<T> = when (this) {
    is Result.Error -> {
        action(exception)
        this
    }
    else -> this
}
```

### 5. UI State Mapping

Natural mapping to UI states:

```kotlin
sealed class ProfileState {
    object Loading : ProfileState()           // Maps from Result.Loading
    data class Success(...) : ProfileState()  // Maps from Result.Success
    data class Error(...) : ProfileState()   // Maps from Result.Error
}
```

## Common Operations

### Check if Success

```kotlin
if (result is Result.Success) {
    val data = result.data
    // Use data
}
```

### Check if Error

```kotlin
if (result is Result.Error) {
    val exception = result.exception
    // Handle error
}
```

### Get Data with Fallback

```kotlin
val data = when (result) {
    is Result.Success -> result.data
    else -> defaultValue
}
```

### Transform Success Data

```kotlin
val transformed = when (result) {
    is Result.Success -> Result.Success(transform(result.data))
    else -> result
}
```

### Chain Operations

```kotlin
fun getUserProfile(login: String): Flow<Result<Profile>> = flow {
    userRepository.getUserInfo(login).collect { userResult ->
        when (userResult) {
            is Result.Success -> {
                val skillsResult = userRepository.getUserSkills(userResult.data.id)
                // Combine results
            }
            else -> emit(userResult)
        }
    }
}
```

## Comparison with Alternatives

### vs. Try-Catch

**Result Pattern:**
```kotlin
val result: Result<UserModel> = getUserInfo()
when (result) {
    is Result.Success -> use(result.data)
    is Result.Error -> handle(result.exception)
}
```

**Try-Catch:**
```kotlin
try {
    val user = getUserInfo()  // Can throw
    use(user)
} catch (e: Exception) {
    handle(e)
}
```

**Advantages of Result:**
- Errors are explicit in type signature
- Can represent Loading state
- No exception overhead for control flow
- Compiler enforces error handling

### vs. Nullable Return

**Result Pattern:**
```kotlin
fun getUser(): Result<UserModel>
// Can distinguish between error and no data
```

**Nullable:**
```kotlin
fun getUser(): UserModel?
// Can't distinguish between error and no data
```

**Advantages of Result:**
- Can carry error information
- Can represent Loading state
- More expressive

### vs. Either/Left-Right

**Result Pattern:**
```kotlin
sealed class Result<T> {
    Success(data), Error(exception), Loading
}
```

**Either:**
```kotlin
sealed class Either<L, R> {
    Left(L), Right(R)
}
```

**Advantages of Result:**
- Three states (Loading is important for UI)
- Specific to success/error semantics
- More intuitive for Android developers

## Best Practices

### 1. Always Handle All Cases

```kotlin
// Good
when (result) {
    is Result.Success -> { /* handle */ }
    is Result.Error -> { /* handle */ }
    is Result.Loading -> { /* handle */ }
}

// Bad - missing Loading case
when (result) {
    is Result.Success -> { /* handle */ }
    is Result.Error -> { /* handle */ }
}
```

### 2. Use Smart Casting

```kotlin
// Good - smart cast works
when (result) {
    is Result.Success -> {
        val data = result.data  // Type is T
    }
}

// Bad - unnecessary casting
if (result is Result.Success) {
    val data = (result as Result.Success<T>).data
}
```

### 3. Extract Data Safely

```kotlin
// Good - explicit fallback
val data = when (result) {
    is Result.Success -> result.data
    else -> defaultValue
}

// Bad - unsafe access
val data = (result as Result.Success).data  // Can crash
```

### 4. Don't Ignore Errors

```kotlin
// Good - handle error
when (result) {
    is Result.Error -> {
        logError(result.exception)
        showErrorToUser(result.exception)
    }
    // ...
}

// Bad - ignore error
when (result) {
    is Result.Success -> { /* use data */ }
    else -> { /* ignore */ }
}
```

### 5. Use Loading State for UI

```kotlin
// Good - show loading indicator
when (result) {
    is Result.Loading -> {
        showLoadingIndicator()
    }
    is Result.Success -> {
        hideLoadingIndicator()
        showData(result.data)
    }
    // ...
}
```

## Extension Functions (Optional)

You can add extension functions for convenience:

```kotlin
// Get data or null
fun <T> Result<T>.getDataOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

// Get data or default
fun <T> Result<T>.getDataOrDefault(default: T): T = when (this) {
    is Result.Success -> data
    else -> default
}

// Map success data
fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

// Flat map (chain operations)
fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
    is Result.Loading -> this
}
```

## Use Cases in This Project

### 1. Repository Layer

All repository methods return `Flow<Result<T>>`:

```kotlin
// UserRepository.kt
fun getUserInfo(login: String): Flow<Result<UserModel>>
fun getUserSkills(userId: Int): Flow<Result<List<SkillModel>>>
fun getUserProjects(userId: Int): Flow<Result<List<ProjectModel>>>
```

### 2. ViewModel Layer

ViewModels collect and transform Results:

```kotlin
// ProfileViewModel.kt
userRepository.getUserInfo(login).collect { userResult ->
    when (userResult) {
        is Result.Success -> { /* process data */ }
        is Result.Error -> { /* show error */ }
        is Result.Loading -> { /* show loading */ }
    }
}
```

### 3. Service Layer

Services return `Result<T>` for operations:

```kotlin
// AuthService.kt
suspend fun getToken(code: String): Result<TokenResponse>
suspend fun refreshToken(refreshToken: String): Result<TokenResponse>
```

## Code Locations

- **Result Definition**: `app/src/main/java/com/students42/app/utils/Result.kt`
- **Repository Usage**: `app/src/main/java/com/students42/app/data/repositories/UserRepository.kt`
- **ViewModel Usage**: 
  - `app/src/main/java/com/students42/app/ui/profile/ProfileViewModel.kt`
  - `app/src/main/java/com/students42/app/ui/login/LoginViewModel.kt`
- **Service Usage**: `app/src/main/java/com/students42/app/auth/AuthService.kt`

## References

- [Kotlin Sealed Classes](https://kotlinlang.org/docs/sealed-classes.html)
- [Functional Error Handling](https://kotlinlang.org/docs/functional-programming.html)
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)
