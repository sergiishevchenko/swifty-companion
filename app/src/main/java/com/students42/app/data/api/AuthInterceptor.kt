package com.students42.app.data.api

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

        val token = runBlocking {
            tokenRepository.getValidToken()
        }

        val requestBuilder = originalRequest.newBuilder()

        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        var response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            lock.withLock {
                val refreshed = runBlocking {
                    refreshTokenIfNeeded()
                }

                if (refreshed) {
                    val newToken = runBlocking {
                        tokenRepository.getValidToken()
                    }

                    if (newToken != null) {
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        response.close()
                        response = chain.proceed(newRequest)
                    } else {
                        runBlocking {
                            tokenRepository.clearToken()
                        }
                    }
                } else {
                    runBlocking {
                        tokenRepository.clearToken()
                    }
                }
            }
        }

        return response
    }

    private suspend fun refreshTokenIfNeeded(): Boolean {
        val refreshToken = tokenRepository.getRefreshToken() ?: return false
        return try {
            val apiService = apiServiceProvider.get()
            val response = apiService.refreshToken(
                grantType = "refresh_token",
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = refreshToken
            )
            tokenRepository.saveToken(
                token = response.accessToken,
                expiresIn = response.expiresIn
            )
            response.refreshToken?.let {
                tokenRepository.saveRefreshToken(it)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
