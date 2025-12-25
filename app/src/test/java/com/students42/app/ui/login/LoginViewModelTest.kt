package com.students42.app.ui.login

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.students42.app.R
import com.students42.app.auth.AuthService
import com.students42.app.data.local.TokenRepository
import com.students42.app.data.models.TokenResponse
import com.students42.app.data.models.UserModel
import com.students42.app.data.repositories.UserRepository
import com.students42.app.utils.Result
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LoginViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var userRepository: UserRepository
    private lateinit var tokenRepository: TokenRepository
    private lateinit var authService: AuthService
    private lateinit var viewModel: LoginViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext() as Application
        context = application
        userRepository = mock()
        tokenRepository = mock()
        authService = mock()
        whenever(tokenRepository.getToken()).thenReturn(flowOf(null))
        viewModel = LoginViewModel(
            context = application,
            userRepository = userRepository,
            tokenRepository = tokenRepository,
            authService = authService
        )
    }

    @Test
    fun `checkToken sets NoToken when token is null`() = runTest(testDispatcher) {
        whenever(tokenRepository.getToken()).thenReturn(flowOf(null))

        viewModel.checkToken()
        advanceUntilIdle()

        assertEquals(LoginState.NoToken, viewModel.loginState.value)
    }

    @Test
    fun `checkToken sets Idle when token exists`() = runTest(testDispatcher) {
        whenever(tokenRepository.getToken()).thenReturn(flowOf("valid_token"))

        viewModel.checkToken()
        advanceUntilIdle()

        assertEquals(LoginState.Idle, viewModel.loginState.value)
    }

    @Test
    fun `searchUser sets Error when login is blank`() = runTest(testDispatcher) {
        viewModel.searchUser("")
        advanceUntilIdle()

        assertTrue(viewModel.loginState.value is LoginState.Error)
    }

    @Test
    fun `searchUser sets Loading then Success when user found`() = runTest(testDispatcher) {
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = "test@example.com",
            location = null,
            wallet = null,
            image = null,
            skills = null,
            projectsUsers = null,
            cursusUsers = null,
            campus = null,
            campusUsers = null,
            correctionPoint = null
        )
        whenever(userRepository.getUserInfo("testuser")).thenReturn(
            flowOf(Result.Loading, Result.Success(user))
        )

        viewModel.searchUser("testuser")
        advanceUntilIdle()

        assertTrue(viewModel.loginState.value is LoginState.Success)
        assertEquals(user, (viewModel.loginState.value as LoginState.Success).user)
    }

    @Test
    fun `searchUser sets Loading then Error when API call fails`() = runTest(testDispatcher) {
        val exception = RuntimeException("API error")
        whenever(userRepository.getUserInfo("testuser")).thenReturn(
            flowOf(Result.Loading, Result.Error(exception))
        )

        viewModel.searchUser("testuser")
        advanceUntilIdle()

        assertTrue(viewModel.loginState.value is LoginState.Error)
    }

    @Test
    fun `startOAuthFlow returns Intent from AuthService`() {
        val intent = Intent()
        whenever(authService.startOAuthFlow()).thenReturn(intent)

        val result = viewModel.startOAuthFlow()

        assertEquals(intent, result)
    }

    @Test
    fun `handleOAuthCallback saves token and checks token on success`() = runTest(testDispatcher) {
        val tokenResponse = TokenResponse(
            accessToken = "test_token",
            expiresIn = 7200,
            refreshToken = "test_refresh_token"
        )
        whenever(authService.getToken("test_code")).thenReturn(Result.Success(tokenResponse))
        whenever(tokenRepository.getToken()).thenReturn(flowOf("test_token"))

        viewModel.handleOAuthCallback("test_code")
        advanceUntilIdle()

        verify(authService).saveTokenResponse(tokenResponse)
    }

    @Test
    fun `handleOAuthCallback sets Error when token exchange fails`() = runTest(testDispatcher) {
        val exception = RuntimeException("Token exchange failed")
        whenever(authService.getToken("test_code")).thenReturn(Result.Error(exception))

        viewModel.handleOAuthCallback("test_code")
        advanceUntilIdle()

        assertTrue(viewModel.loginState.value is LoginState.Error)
    }

    @Test
    fun `retry calls performSearch with lastSearchLogin`() = runTest(testDispatcher) {
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = "test@example.com",
            location = null,
            wallet = null,
            image = null,
            skills = null,
            projectsUsers = null,
            cursusUsers = null,
            campus = null,
            campusUsers = null,
            correctionPoint = null
        )
        whenever(userRepository.getUserInfo("testuser")).thenReturn(
            flowOf(Result.Loading, Result.Success(user))
        )

        viewModel.searchUser("testuser")
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        verify(userRepository, org.mockito.kotlin.times(2)).getUserInfo("testuser")
    }

    @Test
    fun `clearError sets Idle when state is Error`() = runTest(testDispatcher) {
        viewModel.searchUser("")
        advanceUntilIdle()

        viewModel.clearError()
        advanceUntilIdle()

        assertEquals(LoginState.Idle, viewModel.loginState.value)
    }

    @Test
    fun `clearError does nothing when state is not Error`() {
        whenever(tokenRepository.getToken()).thenReturn(flowOf("token"))
        viewModel.checkToken()

        val currentState = viewModel.loginState.value
        viewModel.clearError()

        assertEquals(currentState, viewModel.loginState.value)
    }
}
