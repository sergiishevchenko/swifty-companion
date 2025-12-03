package com.students42.app.data.api

import com.students42.app.data.local.TokenRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenRepository: TokenRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = runBlocking {
            tokenRepository.getToken().first()
        }

        val requestBuilder = originalRequest.newBuilder()

        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            runBlocking {
                tokenRepository.clearToken()
            }
        }

        return response
    }
}
