package com.students42.app.data.api

import com.students42.app.data.local.TokenRepository
import com.students42.app.data.models.TokenResponse
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit
import javax.inject.Provider

class AuthInterceptorTest {
    private lateinit var tokenRepository: TokenRepository
    private lateinit var apiServiceProvider: Provider<ApiService>
    private lateinit var mockWebServer: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var authInterceptor: AuthInterceptor

    private val clientId = "test_client_id"
    private val clientSecret = "test_client_secret"

    @Before
    fun setup() {
        tokenRepository = mock()
        apiServiceProvider = mock()
        mockWebServer = MockWebServer()
        mockWebServer.start()

        authInterceptor = AuthInterceptor(
            tokenRepository = tokenRepository,
            apiServiceProvider = apiServiceProvider,
            clientId = clientId,
            clientSecret = clientSecret
        )

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `intercept bypasses OAuth token endpoint`() = runTest {
        val url = mockWebServer.url("/oauth/token")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        whenever(tokenRepository.getTokenSync()).thenReturn(null)
        whenever(tokenRepository.isTokenExpired()).thenReturn(false)

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        assertEquals(200, response.code)
    }

    @Test
    fun `intercept adds Authorization header when token exists`() = runTest {
        val url = mockWebServer.url("/v2/users/testuser")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        whenever(tokenRepository.getTokenSync()).thenReturn("test_token")
        whenever(tokenRepository.isTokenExpired()).thenReturn(false)

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        assertEquals(200, response.code)
        val requestHeader = response.request.header("Authorization")
        assertEquals("Bearer test_token", requestHeader)
    }

    @Test
    fun `intercept does not add Authorization header when token is null`() = runTest {
        val url = mockWebServer.url("/v2/users/testuser")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        whenever(tokenRepository.getTokenSync()).thenReturn(null)
        whenever(tokenRepository.isTokenExpired()).thenReturn(false)

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        assertEquals(200, response.code)
    }

    @Test
    fun `intercept refreshes token when expired before request`() = runTest {
        val url = mockWebServer.url("/v2/users/testuser")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val apiService = mock<ApiService>()
        val tokenResponse = TokenResponse(
            accessToken = "new_token",
            expiresIn = 7200,
            refreshToken = "new_refresh_token"
        )

        whenever(tokenRepository.getTokenSync()).thenReturn("expired_token", "new_token")
        whenever(tokenRepository.isTokenExpired()).thenReturn(true)
        whenever(tokenRepository.getRefreshToken()).thenReturn("refresh_token")
        whenever(apiServiceProvider.get()).thenReturn(apiService)
        whenever(apiService.refreshToken(any(), any(), any(), any())).thenReturn(tokenResponse)

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        assertEquals(200, response.code)
        verify(tokenRepository).saveToken("new_token", 7200)
        verify(tokenRepository).saveRefreshToken("new_refresh_token")
    }

    @Test
    fun `intercept clears tokens when refresh fails`() = runTest {
        val url = mockWebServer.url("/v2/users/testuser")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val apiService = mock<ApiService>()

        whenever(tokenRepository.getTokenSync()).thenReturn("expired_token")
        whenever(tokenRepository.isTokenExpired()).thenReturn(true)
        whenever(tokenRepository.getRefreshToken()).thenReturn("refresh_token")
        whenever(apiServiceProvider.get()).thenReturn(apiService)
        whenever(apiService.refreshToken(any(), any(), any(), any())).thenThrow(RuntimeException("Refresh failed"))

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        assertEquals(200, response.code)
        verify(tokenRepository).clearToken()
    }

    @Test
    fun `intercept retries request with new token on 401`() = runTest {
        val url = mockWebServer.url("/v2/users/testuser")
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val apiService = mock<ApiService>()
        val tokenResponse = TokenResponse(
            accessToken = "new_token",
            expiresIn = 7200,
            refreshToken = "new_refresh_token"
        )

        whenever(tokenRepository.getTokenSync()).thenReturn("old_token")
        whenever(tokenRepository.isTokenExpired()).thenReturn(false)
        whenever(tokenRepository.getRefreshToken()).thenReturn("refresh_token")
        whenever(tokenRepository.getValidToken()).thenReturn("new_token")
        whenever(apiServiceProvider.get()).thenReturn(apiService)
        whenever(apiService.refreshToken(any(), any(), any(), any())).thenReturn(tokenResponse)

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `intercept clears tokens when refresh fails on 401`() = runTest {
        val url = mockWebServer.url("/v2/users/testuser")
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val apiService = mock<ApiService>()

        whenever(tokenRepository.getTokenSync()).thenReturn("old_token")
        whenever(tokenRepository.isTokenExpired()).thenReturn(false)
        whenever(tokenRepository.getRefreshToken()).thenReturn("refresh_token")
        whenever(apiServiceProvider.get()).thenReturn(apiService)
        whenever(apiService.refreshToken(any(), any(), any(), any())).thenThrow(RuntimeException("Refresh failed"))

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        assertEquals(401, response.code)
        verify(tokenRepository).clearToken()
    }
}

