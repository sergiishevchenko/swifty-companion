package com.students42.app.data.api

import android.util.Log
import com.students42.app.data.local.TokenRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Provider
import kotlin.concurrent.withLock

class AuthInterceptor(
    private val tokenRepository: TokenRepository,
    private val apiServiceProvider: Provider<ApiService>,
    private val clientId: String,
    private val clientSecret: String
) : Interceptor {
    private val lock = ReentrantLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        if (url.contains("/oauth/token")) {
            return chain.proceed(originalRequest)
        }

        val token = try {
            runBlocking {
                val tokenValue = tokenRepository.getTokenSync()
                val isExpired = tokenRepository.isTokenExpired()
                
                if (isExpired && tokenValue != null) {
                    Log.d("AuthInterceptor", "=== TOKEN EXPIRED BEFORE REQUEST ===")
                    Log.d("AuthInterceptor", "Attempting to refresh token...")
                    val refreshed = refreshTokenIfNeeded()
                    if (refreshed) {
                        Log.d("AuthInterceptor", "Token refresh successful")
                        tokenRepository.getTokenSync()
                    } else {
                        Log.d("AuthInterceptor", "Token refresh failed, clearing tokens")
                        tokenRepository.clearToken()
                        null
                    }
                } else {
                    tokenValue
                }
            }
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "Error getting token: ${e.message}", e)
            null
        }

        val requestBuilder = originalRequest.newBuilder()

        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        var response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            Log.d("AuthInterceptor", "=== RECEIVED 401 UNAUTHORIZED ===")
            Log.d("AuthInterceptor", "Request URL: ${originalRequest.url}")
            lock.withLock {
                val refreshed = runBlocking {
                    refreshTokenIfNeeded()
                }

                if (refreshed) {
                    Log.d("AuthInterceptor", "Token refresh successful after 401")
                    val newToken = runBlocking {
                        tokenRepository.getValidToken()
                    }

                    if (newToken != null) {
                        Log.d("AuthInterceptor", "Retrying request with new token")
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        response.close()
                        response = chain.proceed(newRequest)
                        Log.d("AuthInterceptor", "Retry response code: ${response.code}")
                    } else {
                        Log.d("AuthInterceptor", "New token is null, clearing tokens")
                        runBlocking {
                            tokenRepository.clearToken()
                        }
                    }
                } else {
                    Log.d("AuthInterceptor", "Token refresh failed after 401, clearing tokens")
                    runBlocking {
                        tokenRepository.clearToken()
                    }
                }
            }
        }

        return response
    }

    private suspend fun refreshTokenIfNeeded(): Boolean {
        val refreshToken = tokenRepository.getRefreshToken() ?: run {
            Log.d("AuthInterceptor", "No refresh token available")
            return false
        }
        return try {
            Log.d("AuthInterceptor", "=== STARTING TOKEN REFRESH ===")
            Log.d("AuthInterceptor", "Using refresh token: $refreshToken")
            val apiService = apiServiceProvider.get()
            val response = apiService.refreshToken(
                grantType = "refresh_token",
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = refreshToken
            )
            Log.d("AuthInterceptor", "Refresh API response received")
            Log.d("AuthInterceptor", "New access token: ${response.accessToken}")
            Log.d("AuthInterceptor", "New refresh token: ${response.refreshToken ?: "not provided"}")
            Log.d("AuthInterceptor", "Expires in: ${response.expiresIn ?: "not provided"} seconds")
            tokenRepository.saveToken(
                token = response.accessToken,
                expiresIn = response.expiresIn
            )
            response.refreshToken?.let {
                tokenRepository.saveRefreshToken(it)
            }
            Log.d("AuthInterceptor", "=== TOKEN REFRESH COMPLETED SUCCESSFULLY ===")
            true
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "Token refresh failed: ${e.message}", e)
            false
        }
    }
}
