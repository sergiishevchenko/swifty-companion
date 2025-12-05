package com.students42.app.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.students42.app.data.api.ApiService
import com.students42.app.data.local.TokenRepository
import com.students42.app.data.models.TokenResponse
import com.students42.app.utils.Constants
import com.students42.app.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthService(
    private val context: Context,
    private val apiService: ApiService,
    private val tokenRepository: TokenRepository,
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String
) {
    fun startOAuthFlow(): Intent {
        val authUrl = buildAuthUrl()
        return Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
    }

    private fun buildAuthUrl(): String {
        val params = mapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to "public"
        )
        val queryString = params.entries.joinToString("&") { "${it.key}=${Uri.encode(it.value)}" }
        return "${Constants.OAUTH_AUTHORIZE_URL}?$queryString"
    }

    fun handleOAuthCallback(uri: Uri): String? {
        return uri.getQueryParameter("code")
    }

    suspend fun getToken(code: String): Result<TokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getToken(
                    grantType = "authorization_code",
                    clientId = clientId,
                    clientSecret = clientSecret,
                    code = code,
                    redirectUri = redirectUri
                )
                Result.Success(response)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    suspend fun saveTokenResponse(tokenResponse: TokenResponse) {
        tokenRepository.saveToken(
            token = tokenResponse.accessToken,
            expiresIn = tokenResponse.expiresIn
        )
        tokenResponse.refreshToken?.let {
            tokenRepository.saveRefreshToken(it)
        }
    }

    suspend fun isTokenValid(): Boolean {
        return !tokenRepository.isTokenExpired()
    }

    suspend fun refreshToken(refreshToken: String): Result<TokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.refreshToken(
                    grantType = "refresh_token",
                    clientId = clientId,
                    clientSecret = clientSecret,
                    refreshToken = refreshToken
                )
                Result.Success(response)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    suspend fun refreshTokenIfNeeded(): Boolean {
        val refreshToken = tokenRepository.getRefreshToken() ?: return false
        val result = refreshToken(refreshToken)
        return when (result) {
            is Result.Success -> {
                saveTokenResponse(result.data)
                true
            }
            is Result.Error -> {
                false
            }
            else -> false
        }
    }
}
