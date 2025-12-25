# Testing Guide

## Overview

This document describes how to test the application, what tests are available, and how to run them.

## Test Structure

Tests are located in `app/src/test/java/com/students42/app/` directory and organized by package structure matching the main source code.

## Available Tests

### 1. ErrorHandlerTest

**Location**: `app/src/test/java/com/students42/app/utils/ErrorHandlerTest.kt`

**Purpose**: Tests error handling logic for different exception types.

**Test Cases**:
- HTTP 401 Unauthorized error
- HTTP 404 Not Found error
- HTTP 500 Server error
- Other HTTP error codes
- IOException handling
- SocketTimeoutException handling
- UnknownHostException handling
- JsonSyntaxException handling
- Unknown exception types

**Example**:
```kotlin
@Test
fun `getErrorStringRes returns error_unauthorized for 401 HttpException`() {
    val exception = HttpException(...)
    val result = ErrorHandler.getErrorStringRes(exception)
    assertEquals(R.string.error_unauthorized, result)
}
```

### 2. ProjectModelTest

**Location**: `app/src/test/java/com/students42/app/data/models/ProjectModelTest.kt`

**Purpose**: Tests project filtering and status logic.

**Test Cases**:
- `isPiscine` - Detecting Piscine projects
- `isAdvancedCore` - Detecting Advanced Core projects
- `isCommonCore` - Detecting Common Core projects
- `isCommonOrAdvanced` - Filtering logic
- `isFailed` - Failed project detection (validated=false, status=failed, negative marks)
- `isCompleted` - Completed project detection (various conditions)
- `name` - Project name retrieval

**Example**:
```kotlin
@Test
fun `isPiscine returns true when cursus contains piscine`() {
    val cursus = listOf(CursusModel(id = 1, name = "Piscine", slug = "piscine"))
    val project = createProject(cursus = cursus)
    assertTrue(project.isPiscine)
}
```

### 3. ResultTest

**Location**: `app/src/test/java/com/students42/app/utils/ResultTest.kt`

**Purpose**: Tests the Result sealed class implementation.

**Test Cases**:
- Success result with data
- Error result with exception
- Loading singleton object

**Example**:
```kotlin
@Test
fun `Success contains data`() {
    val result = Result.Success("test data")
    assertTrue(result is Result.Success)
    assertEquals("test data", (result as Result.Success).data)
}
```

### 4. AuthServiceTest

**Location**: `app/src/test/java/com/students42/app/auth/AuthServiceTest.kt`

**Purpose**: Tests OAuth authentication service.

**Test Cases**:
- OAuth callback code extraction from URI
- OAuth flow Intent creation
- OAuth URL building with correct parameters
- Token retrieval (success and error cases)
- Token response saving

**Example**:
```kotlin
@Test
fun `handleOAuthCallback extracts code from URI`() {
    val uri = Uri.parse("students42://oauth/callback?code=test_code_123")
    val code = authService.handleOAuthCallback(uri)
    assertEquals("test_code_123", code)
}
```

### 5. TokenRepositoryTest

**Location**: `app/src/test/java/com/students42/app/data/local/TokenRepositoryTest.kt`

**Purpose**: Basic tests for token repository initialization.

**Test Cases**:
- Repository instantiation

**Note**: Full testing of TokenRepository requires Android context and DataStore, which is better suited for instrumented tests.

### 6. UserRepositoryTest

**Location**: `app/src/test/java/com/students42/app/data/repositories/UserRepositoryTest.kt`

**Purpose**: Tests user data repository with mocked API service.

**Test Cases**:
- `getUserInfo` - Success and error cases
- `getUserSkills` - Success and error cases
- `getUserProjects` - Success and error cases
- Flow emission (Loading → Success/Error)

**Example**:
```kotlin
@Test
fun `getUserInfo emits Loading then Success when API call succeeds`() = runTest {
    val user = UserModel(...)
    whenever(apiService.getUserInfo("testuser")).thenReturn(user)
    // Test flow emission
}
```

### 7. LoginViewModelTest

**Location**: `app/src/test/java/com/students42/app/ui/login/LoginViewModelTest.kt`

**Purpose**: Tests login screen ViewModel logic.

**Test Cases**:
- `checkToken` - Token validation (null vs exists)
- `searchUser` - User search with blank input, success, error
- `startOAuthFlow` - OAuth Intent creation
- `handleOAuthCallback` - Token exchange (success and error)
- `retry` - Retry functionality
- `clearError` - Error state clearing

**Example**:
```kotlin
@Test
fun `checkToken sets NoToken when token is null`() = runTest {
    whenever(tokenRepository.getToken()).thenReturn(flowOf(null))
    viewModel.checkToken()
    assertEquals(LoginState.NoToken, viewModel.loginState.value)
}
```

### 8. ProfileViewModelTest

**Location**: `app/src/test/java/com/students42/app/ui/profile/ProfileViewModelTest.kt`

**Purpose**: Tests profile screen ViewModel logic.

**Test Cases**:
- `loadUserProfileByLogin` - Loading user profile (success and error)
- Skills merging from API and cursus
- Projects filtering (only completed and failed)
- `retry` - Retry functionality

**Example**:
```kotlin
@Test
fun `loadUserProfileByLogin sets Loading then Success when user data loaded`() = runTest {
    // Test profile loading with skills and projects
}
```

### 9. AuthInterceptorTest

**Location**: `app/src/test/java/com/students42/app/data/api/AuthInterceptorTest.kt`

**Purpose**: Tests OAuth token interceptor with MockWebServer.

**Test Cases**:
- OAuth token endpoint bypass
- Authorization header injection
- Token refresh on expiration
- Token refresh failure handling
- 401 response handling with retry
- Token clearing on refresh failure

**Example**:
```kotlin
@Test
fun `intercept adds Authorization header when token exists`() = runTest {
    // Test with MockWebServer
}
```

### 10. UserModelTest

**Location**: `app/src/test/java/com/students42/app/data/models/UserModelTest.kt`

**Purpose**: Tests UserModel computed properties.

**Test Cases**:
- `imageUrl` - Image URL retrieval
- `level` - Level calculation from cursus (active and ended)
- `locationName` - Location name resolution
- `evaluations` - Correction points retrieval

## Running Tests

### From Android Studio

1. **Run All Tests**:
   - Right-click on `app/src/test` directory
   - Select "Run 'Tests in 'test''"

2. **Run Single Test Class**:
   - Right-click on test file
   - Select "Run 'ClassNameTest'"

3. **Run Single Test Method**:
   - Click on green arrow next to test method
   - Or right-click on method and select "Run 'methodName()'"

### From Command Line

**Run all unit tests**:
```bash
./gradlew test
```

**Run tests for specific variant**:
```bash
./gradlew testDebugUnitTest
./gradlew testReleaseUnitTest
```

**Run single test class**:
```bash
./gradlew test --tests "com.students42.app.utils.ErrorHandlerTest"
```

**View test results**:
```bash
./gradlew test
# Results are in: app/build/test-results/test/
# HTML report: app/build/reports/tests/test/index.html
```

## Test Dependencies

The following testing libraries are used:

- **JUnit 4.13.2** - Basic testing framework
- **Mockito Core 5.1.1** - Mocking framework
- **Mockito Kotlin 5.1.0** - Kotlin extensions for Mockito
- **Kotlin Coroutines Test 1.7.3** - Coroutine testing utilities
- **AndroidX Core Testing 2.2.0** - Android testing utilities
- **Hilt Testing 2.51.1** - Dependency injection testing

## Writing New Tests

### Test Naming Convention

Use descriptive test names with backticks for readability:

```kotlin
@Test
fun `methodName returns expected value when condition is met`() {
    // Test implementation
}
```

### Test Structure

Follow AAA pattern (Arrange-Act-Assert):

```kotlin
@Test
fun `test description`() {
    // Arrange - Set up test data
    val input = "test"
    
    // Act - Execute code under test
    val result = functionUnderTest(input)
    
    // Assert - Verify results
    assertEquals(expected, result)
}
```

### Mocking with Mockito

```kotlin
@Before
fun setup() {
    mockObject = mock()
    whenever(mockObject.method()).thenReturn(value)
}

@Test
fun `test with mocks`() {
    verify(mockObject).method()
}
```

### Testing Coroutines

Use `runTest` for coroutine testing:

```kotlin
@Test
fun `suspend function test`() = runTest {
    val result = suspendFunction()
    assertEquals(expected, result)
}
```

## Test Coverage

Current test coverage includes:

- ✅ Error handling logic (ErrorHandler)
- ✅ Project model filtering logic (ProjectModel)
- ✅ Result sealed class (Result)
- ✅ OAuth service functionality (AuthService)
- ✅ User repository (UserRepository)
- ✅ Login ViewModel (LoginViewModel)
- ✅ Profile ViewModel (ProfileViewModel)
- ✅ Auth interceptor (AuthInterceptor with MockWebServer)
- ✅ User model computed properties (UserModel)
- ⚠️ TokenRepository (basic only - requires instrumented tests for full coverage)
- ❌ UI Components (requires Compose UI testing - better suited for instrumented tests)

## Instrumented Tests

For components that require Android context (ViewModels, Repositories with DataStore), use instrumented tests in `app/src/androidTest/`.

### Example Instrumented Test Structure

```kotlin
@RunWith(AndroidJUnit4::class)
class TokenRepositoryInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Test
    fun testTokenStorage() {
        // Test with real Android context
    }
}
```

## Best Practices

1. **Isolate Tests**: Each test should be independent and not rely on other tests
2. **Use Descriptive Names**: Test names should clearly describe what is being tested
3. **Test Edge Cases**: Include tests for null values, empty collections, boundary conditions
4. **Mock External Dependencies**: Use mocks for API calls, database, file system
5. **Keep Tests Fast**: Unit tests should run quickly without I/O operations
6. **Test One Thing**: Each test should verify one specific behavior
7. **Use Meaningful Assertions**: Use specific assertions (assertEquals, assertTrue) instead of generic ones

## Troubleshooting

### Tests Not Running

- Ensure test dependencies are synced: `File → Sync Project with Gradle Files`
- Check that test classes are in `app/src/test/` directory
- Verify test methods are annotated with `@Test`

### Mockito Issues

- Ensure `mockito-kotlin` is used for Kotlin code
- Use `whenever` instead of `when` (Kotlin keyword conflict)
- For final classes, may need `mockito-inline`

### Coroutine Test Issues

- Use `runTest` from `kotlinx.coroutines.test`
- Use `TestDispatcher` for controlling coroutine execution
- Avoid `runBlocking` in tests when possible

### DataStore Testing

- DataStore requires Android context
- Use instrumented tests for full DataStore testing
- Or use `TestDataStoreFactory` for unit tests

## Continuous Integration

Tests can be integrated into CI/CD pipeline:

```yaml
# Example GitHub Actions
- name: Run tests
  run: ./gradlew test
```

## Additional Test Coverage Opportunities

Areas that could benefit from additional testing:

1. **UI Components**: Compose UI tests (requires instrumented tests)
2. **Integration Tests**: End-to-end tests for critical flows
3. **TokenRepository**: Full DataStore testing (requires instrumented tests)
4. **Edge Cases**: More boundary condition testing
5. **Performance Tests**: Test for memory leaks, performance bottlenecks

## References

- [JUnit Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Kotlin Coroutines Testing](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/)
- [Android Testing Guide](https://developer.android.com/training/testing)
- [Hilt Testing](https://dagger.dev/hilt/testing.html)
