# Dagger Hilt

## Overview

**Dagger Hilt** is a dependency injection library built on top of Dagger, specifically designed for Android applications. It simplifies dependency injection by reducing boilerplate code and providing a standard way to integrate DI into Android apps.

## What is Dagger Hilt?

Dagger Hilt is Google's recommended solution for dependency injection in Android. It provides:

- **Simplified setup** - Less boilerplate compared to vanilla Dagger
- **Android-aware** - Built specifically for Android lifecycle and components
- **Compile-time safety** - Catches dependency errors at compile time
- **Standard components** - Pre-defined Android component scopes
- **Kotlin-first** - Designed with Kotlin in mind, using `KClass` for type information

## Architecture

### Core Concepts

```
┌─────────────────────────────────────────────────────────┐
│                    Application                          │
│              (@HiltAndroidApp)                          │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ generates
                     ▼
┌─────────────────────────────────────────────────────────┐
│              SingletonComponent                         │
│         (Application-level scope)                       │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│  Module  │  │  Module  │  │  Module  │
│  (DI)    │  │  (DI)    │  │  (DI)    │
└──────────┘  └──────────┘  └──────────┘
        │            │            │
        └────────────┼────────────┘
                     │
                     │ provides
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Android Components                         │
│  (Activity, Fragment, ViewModel, Service, etc.)         │
│              (@AndroidEntryPoint, @HiltViewModel)       │
└─────────────────────────────────────────────────────────┘
```

### Key Components

1. **Application Component** - `@HiltAndroidApp`
2. **Modules** - `@Module` with `@InstallIn`
3. **Provides** - `@Provides` methods
4. **Inject** - `@Inject` constructor
5. **Components** - Pre-defined scopes (`SingletonComponent`, `ActivityComponent`, etc.)

## KClass in Hilt

### What is KClass?

`KClass` is Kotlin's reflection representation of a class. It's the Kotlin equivalent of Java's `Class<T>`, but with better support for Kotlin-specific features like nullable types, type parameters, and extension functions.

### How Hilt Uses KClass

Hilt uses `KClass` internally to:

1. **Type Information** - Store and retrieve type metadata at compile time
2. **Code Generation** - Generate dependency injection code based on class information
3. **Type Safety** - Ensure type correctness during compilation
4. **Reflection** - Access class metadata for dependency resolution

### KClass vs Class

| Feature | Java `Class<T>` | Kotlin `KClass<T>` |
|---------|----------------|-------------------|
| **Nullability** | No null safety | Supports nullable types |
| **Type Parameters** | Limited support | Full reified generics support |
| **Extension Functions** | Not supported | Supported |
| **Properties** | Field-based | Property-based |
| **Primary Constructor** | Not distinguished | Distinguished |

### Example: KClass Usage in Hilt

When Hilt processes annotations, it uses `KClass` internally:

```kotlin
// Hilt internally uses KClass for type information
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // Hilt uses KClass<Gson> to identify the return type
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }
    
    // Hilt uses KClass<Retrofit> and KClass<Gson> for dependency resolution
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,  // KClass<OkHttpClient>
        gson: Gson                   // KClass<Gson>
    ): Retrofit {                    // KClass<Retrofit>
        return Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
```

### KClass in ViewModel Injection

Hilt uses `KClass` to identify ViewModel types:

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    // Hilt uses KClass<Context>, KClass<UserRepository>, etc.
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val authService: AuthService
) : AndroidViewModel(context as Application)
```

When you request a ViewModel in Compose:

```kotlin
val viewModel: LoginViewModel = hiltViewModel()
// Hilt uses KClass<LoginViewModel> to find the correct provider
```

### Accessing KClass in Code

While Hilt uses `KClass` internally, you can also use it explicitly:

```kotlin
// Get KClass from a type
val gsonClass: KClass<Gson> = Gson::class

// Get KClass from an instance
val instance = Gson()
val instanceClass: KClass<out Gson> = instance::class

// Using KClass for type checking
fun <T : Any> createInstance(kClass: KClass<T>): T {
    return kClass.createInstance()
}
```

## Setup in This Project

### Application Setup

The application class is annotated with `@HiltAndroidApp`:

```6:7:app/src/main/java/com/students42/app/Students42Application.kt
@HiltAndroidApp
class Students42Application : Application()
```

This annotation:
- Generates the base DI component
- Sets up Hilt for the entire application
- Must be applied to the Application class

### Gradle Configuration

```kotlin
plugins {
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
}
```

## Dependency Injection Modules

### NetworkModule

Provides network-related dependencies:

```19:84:app/src/main/java/com/students42/app/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenRepository: TokenRepository,
        apiServiceProvider: javax.inject.Provider<ApiService>,
        @ClientId clientId: String,
        @ClientSecret clientSecret: String
    ): AuthInterceptor {
        return AuthInterceptor(
            tokenRepository = tokenRepository,
            apiServiceProvider = apiServiceProvider,
            clientId = clientId,
            clientSecret = clientSecret
        )
    }

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
}
```

**Key Points:**
- `@Module` - Marks the class as a DI module
- `@InstallIn(SingletonComponent::class)` - Installs the module in the application scope
- `@Provides` - Marks methods that provide dependencies
- `@Singleton` - Ensures only one instance exists
- Dependency chain: `OkHttpClient` → `Retrofit` → `ApiService`

### AuthModule

Provides authentication-related dependencies:

```15:58:app/src/main/java/com/students42/app/di/AuthModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    @ClientId
    fun provideClientId(): String {
        return BuildConfig.API_CLIENT_ID
    }

    @Provides
    @Singleton
    @ClientSecret
    fun provideClientSecret(): String {
        return BuildConfig.API_CLIENT_SECRET
    }

    @Provides
    @Singleton
    @RedirectUri
    fun provideRedirectUri(): String {
        return BuildConfig.API_REDIRECT_URI
    }

    @Provides
    @Singleton
    fun provideAuthService(
        @ApplicationContext context: Context,
        apiService: ApiService,
        tokenRepository: TokenRepository,
        @ClientId clientId: String,
        @ClientSecret clientSecret: String,
        @RedirectUri redirectUri: String
    ): AuthService {
        return AuthService(
            context = context,
            apiService = apiService,
            tokenRepository = tokenRepository,
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = redirectUri
        )
    }
}
```

**Key Points:**
- Uses **Qualifiers** (`@ClientId`, `@ClientSecret`, `@RedirectUri`) to distinguish String types
- `@ApplicationContext` - Hilt-provided qualifier for Application Context
- Multiple String dependencies are differentiated using custom qualifiers

### AppModule

Provides application-level repositories:

```14:28:app/src/main/java/com/students42/app/di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideTokenRepository(@ApplicationContext context: Context): TokenRepository {
        return TokenRepository(context)
    }

    @Provides
    @Singleton
    fun provideUserRepository(apiService: ApiService): UserRepository {
        return UserRepository(apiService)
    }
}
```

## Qualifiers

Qualifiers are used to distinguish between multiple dependencies of the same type:

```5:16:app/src/main/java/com/students42/app/di/Qualifiers.kt
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClientId

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClientSecret

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RedirectUri
```

**Usage:**
- `@Qualifier` - Marks the annotation as a qualifier
- `@Retention(AnnotationRetention.BINARY)` - Retains annotation in compiled bytecode
- Used to differentiate multiple `String` dependencies

## Android Component Injection

### Activity Injection

Activities are injected using `@AndroidEntryPoint`:

```15:16:app/src/main/java/com/students42/app/MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
```

**Requirements:**
- Activity must be annotated with `@AndroidEntryPoint`
- Must call `super.onCreate()` before using injected dependencies
- Hilt generates code to inject dependencies automatically

### ViewModel Injection

ViewModels are injected using `@HiltViewModel`:

```25:31:app/src/main/java/com/students42/app/ui/login/LoginViewModel.kt
@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val authService: AuthService
) : AndroidViewModel(context as Application) {
```

**Key Points:**
- `@HiltViewModel` - Marks the ViewModel for Hilt injection
- `@Inject constructor` - Injects dependencies via constructor
- `@ApplicationContext` - Provides Application Context (not Activity Context)
- ViewModel must extend `AndroidViewModel` or `ViewModel`

**Usage in Compose:**

```kotlin
@Composable
fun LoginScreen() {
    val viewModel: LoginViewModel = hiltViewModel()
    // Use viewModel...
}
```

## Hilt Components (Scopes)

Hilt provides pre-defined Android component scopes:

| Component | Scope | Lifetime |
|-----------|-------|----------|
| `SingletonComponent` | `@Singleton` | Application lifetime |
| `ActivityRetainedComponent` | `@ActivityRetainedScoped` | Survives configuration changes |
| `ActivityComponent` | `@ActivityScoped` | Activity lifetime |
| `ViewModelComponent` | `@ViewModelScoped` | ViewModel lifetime |
| `FragmentComponent` | `@FragmentScoped` | Fragment lifetime |
| `ViewComponent` | `@ViewScoped` | View lifetime |
| `ServiceComponent` | `@ServiceScoped` | Service lifetime |

### Component Hierarchy

```
SingletonComponent (Application)
    ├── ActivityRetainedComponent (ViewModel)
    │       └── ViewModelComponent
    ├── ActivityComponent (Activity)
    │       ├── FragmentComponent (Fragment)
    │       │       └── ViewComponent (View)
    │       └── ViewComponent (View)
    └── ServiceComponent (Service)
```

**Rules:**
- Child components can access parent component dependencies
- Parent components cannot access child component dependencies
- Each component has its own scope annotation

## Dependency Graph

The project's dependency graph:

```
Application
    │
    ├── SingletonComponent
    │       │
    │       ├── NetworkModule
    │       │       ├── Gson
    │       │       ├── HttpLoggingInterceptor
    │       │       ├── OkHttpClient (depends on AuthInterceptor, HttpLoggingInterceptor)
    │       │       ├── Retrofit (depends on OkHttpClient, Gson)
    │       │       └── ApiService (depends on Retrofit)
    │       │
    │       ├── AuthModule
    │       │       ├── @ClientId String
    │       │       ├── @ClientSecret String
    │       │       ├── @RedirectUri String
    │       │       └── AuthService (depends on Context, ApiService, TokenRepository, qualifiers)
    │       │
    │       └── AppModule
    │               ├── TokenRepository (depends on Context)
    │               └── UserRepository (depends on ApiService)
    │
    └── ViewModelComponent
            ├── LoginViewModel (depends on Context, UserRepository, TokenRepository, AuthService)
            └── ProfileViewModel (depends on Context, UserRepository)
```

## How Hilt Works

### Compile-Time Processing

1. **Annotation Processing** - KAPT processes Hilt annotations
2. **Code Generation** - Hilt generates DI code based on `KClass` information
3. **Component Creation** - Creates component interfaces and implementations
4. **Dependency Resolution** - Resolves dependency graph using type information

### Runtime Injection

1. **Component Initialization** - Hilt initializes components when app starts
2. **Dependency Lookup** - Uses `KClass` to find appropriate providers
3. **Instance Creation** - Creates or retrieves instances from scopes
4. **Injection** - Injects dependencies into constructors or fields

### Example: Injection Flow

```kotlin
// 1. Request ViewModel
val viewModel: LoginViewModel = hiltViewModel()

// 2. Hilt looks up KClass<LoginViewModel>
// 3. Finds @HiltViewModel annotation
// 4. Analyzes constructor parameters (KClass<Context>, KClass<UserRepository>, etc.)
// 5. Resolves each dependency:
//    - Context → @ApplicationContext qualifier → Application context
//    - UserRepository → SingletonComponent → AppModule.provideUserRepository()
//    - TokenRepository → SingletonComponent → AppModule.provideTokenRepository()
//    - AuthService → SingletonComponent → AuthModule.provideAuthService()
// 6. Creates ViewModel instance with resolved dependencies
```

## Provider Pattern

Hilt supports lazy injection using `Provider<T>`:

```kotlin
@Provides
fun provideAuthInterceptor(
    tokenRepository: TokenRepository,
    apiServiceProvider: javax.inject.Provider<ApiService>,  // Lazy provider
    @ClientId clientId: String,
    @ClientSecret clientSecret: String
): AuthInterceptor {
    return AuthInterceptor(
        tokenRepository = tokenRepository,
        apiServiceProvider = apiServiceProvider,  // Pass provider, not instance
        clientId = clientId,
        clientSecret = clientSecret
    )
}
```

**Benefits:**
- Prevents circular dependencies
- Lazy initialization
- Can create multiple instances if needed

**Usage:**

```kotlin
class AuthInterceptor(
    private val apiServiceProvider: Provider<ApiService>
) {
    fun someMethod() {
        val apiService = apiServiceProvider.get()  // Get instance when needed
    }
}
```

## Best Practices

### 1. Use Singleton for Expensive Objects

```kotlin
@Provides
@Singleton  // ✅ Reuse instance
fun provideRetrofit(): Retrofit {
    return Retrofit.Builder().build()
}
```

### 2. Use Qualifiers for Same-Type Dependencies

```kotlin
@Provides
@ClientId
fun provideClientId(): String = BuildConfig.API_CLIENT_ID

@Provides
@ClientSecret
fun provideClientSecret(): String = BuildConfig.API_CLIENT_SECRET
```

### 3. Keep Modules Focused

```kotlin
// ✅ Good: Single responsibility
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // Only network-related dependencies
}

// ❌ Bad: Mixed concerns
@Module
@InstallIn(SingletonComponent::class)
object EverythingModule {
    // Network, auth, database, UI - too much!
}
```

### 4. Use @ApplicationContext for Context

```kotlin
@Provides
fun provideTokenRepository(
    @ApplicationContext context: Context  // ✅ Application context
): TokenRepository {
    return TokenRepository(context)
}
```

### 5. Avoid Field Injection

```kotlin
// ❌ Bad: Field injection
class MyClass {
    @Inject lateinit var dependency: SomeDependency
}

// ✅ Good: Constructor injection
class MyClass @Inject constructor(
    private val dependency: SomeDependency
)
```

### 6. Test with Hilt

```kotlin
@HiltAndroidTest
class MyTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var repository: UserRepository
    
    @Before
    fun init() {
        hiltRule.inject()
    }
}
```

## Common Issues and Solutions

### Issue: "Cannot find binding"

**Cause:** Missing `@Provides` method or wrong scope

**Solution:** Ensure all dependencies have providers in the correct component scope

### Issue: Circular Dependency

**Cause:** Two classes depend on each other

**Solution:** Use `Provider<T>` for lazy injection

### Issue: "Hilt must be initialized"

**Cause:** Missing `@HiltAndroidApp` on Application class

**Solution:** Add `@HiltAndroidApp` to Application class

### Issue: "Cannot inject into non-AndroidEntryPoint"

**Cause:** Trying to inject into class without `@AndroidEntryPoint` or `@HiltViewModel`

**Solution:** Add appropriate annotation

## Advantages Over Manual DI

| Aspect | Manual DI | Dagger Hilt |
|--------|-----------|-------------|
| **Boilerplate** | High | Low |
| **Type Safety** | Runtime errors | Compile-time errors |
| **Code Generation** | Manual | Automatic |
| **Testing** | Difficult | Easy with test modules |
| **Scope Management** | Manual | Automatic |
| **Android Integration** | Manual setup | Built-in support |

## Dependencies

```kotlin
implementation("com.google.dagger:hilt-android:2.51.1")
kapt("com.google.dagger:hilt-compiler:2.51.1")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
```

## References

- [Dagger Hilt Documentation](https://dagger.dev/hilt/)
- [Android Hilt Guide](https://developer.android.com/training/dependency-injection/hilt-android)
- [Kotlin KClass Documentation](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/)
- [Dagger Hilt Migration Guide](https://developer.android.com/training/dependency-injection/hilt-migration)
