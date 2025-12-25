package com.students42.app.auth

import android.content.Context
import android.net.Uri
import com.students42.app.data.api.ApiService
import com.students42.app.data.local.TokenRepository
import com.students42.app.data.models.TokenResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthServiceTest {
    private lateinit var context: Context
    private lateinit var apiService: ApiService
    private lateinit var tokenRepository: TokenRepository
    private lateinit var authService: AuthService

    private val clientId = "test_client_id"
    private val clientSecret = "test_client_secret"
    private val redirectUri = "students42://oauth/callback"

    @Before
    fun setup() {
        context = mock()
        apiService = mock()
        tokenRepository = mock()
        authService = AuthService(
            context = context,
            apiService = apiService,
            tokenRepository = tokenRepository,
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = redirectUri
        )
    }

    @Test
    fun `handleOAuthCallback extracts code from URI`() {
        val uri = Uri.parse("students42://oauth/callback?code=test_code_123")
        val code = authService.handleOAuthCallback(uri)
        assertEquals("test_code_123", code)
    }

    @Test
    fun `handleOAuthCallback returns null when no code in URI`() {
        val uri = Uri.parse("students42://oauth/callback")
        val code = authService.handleOAuthCallback(uri)
        assertNull(code)
    }

    @Test
    fun `startOAuthFlow creates Intent with correct action`() {
        val intent = authService.startOAuthFlow()
        assertEquals(android.content.Intent.ACTION_VIEW, intent.action)
        assertNotNull(intent.data)
    }

    @Test
    fun `startOAuthFlow creates Intent with correct URL scheme`() {
        val intent = authService.startOAuthFlow()
        val uri = intent.data
        assertNotNull(uri)
        val uriString = uri.toString()
        assertTrue(uriString.contains("https://api.intra.42.fr/oauth/authorize"))
        assertTrue(uriString.contains("client_id=$clientId"))
        assertTrue(uriString.contains("redirect_uri") && (uriString.contains(android.net.Uri.encode(redirectUri)) || uriString.contains(redirectUri)))
        assertTrue(uriString.contains("response_type=code"))
        assertTrue(uriString.contains("scope=public"))
    }

    @Test
    fun `getToken returns Success when API call succeeds`() = runBlocking {
        val tokenResponse = TokenResponse(
            accessToken = "test_token",
            expiresIn = 7200,
            refreshToken = "test_refresh_token"
        )
        whenever(apiService.getToken(any(), any(), any(), any(), any())).thenReturn(tokenResponse)

        val result = authService.getToken("test_code")

        assertTrue(result is com.students42.app.utils.Result.Success)
        assertEquals(tokenResponse, (result as com.students42.app.utils.Result.Success).data)
    }

    @Test
    fun `getToken returns Error when API call fails`() = runBlocking {
        val exception = RuntimeException("API error")
        whenever(apiService.getToken(any(), any(), any(), any(), any())).thenThrow(exception)

        val result = authService.getToken("test_code")

        assertTrue(result is com.students42.app.utils.Result.Error)
        assertEquals(exception, (result as com.students42.app.utils.Result.Error).exception)
    }

    @Test
    fun `saveTokenResponse saves token and refresh token`() = runBlocking {
        val tokenResponse = TokenResponse(
            accessToken = "test_token",
            expiresIn = 7200,
            refreshToken = "test_refresh_token"
        )

        authService.saveTokenResponse(tokenResponse)

        verify(tokenRepository).saveToken("test_token", 7200)
        verify(tokenRepository).saveRefreshToken("test_refresh_token")
    }

    @Test
    fun `saveTokenResponse saves token without refresh token when null`() = runBlocking {
        val tokenResponse = TokenResponse(
            accessToken = "test_token",
            expiresIn = 7200,
            refreshToken = null
        )

        authService.saveTokenResponse(tokenResponse)

        verify(tokenRepository).saveToken("test_token", 7200)
    }
}
