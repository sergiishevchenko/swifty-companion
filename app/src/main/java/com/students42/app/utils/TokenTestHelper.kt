package com.students42.app.utils

import android.content.Context
import com.students42.app.data.local.TokenRepository
import kotlinx.coroutines.runBlocking

object TokenTestHelper {
    fun setExpiredToken(context: Context) {
        val tokenRepository = TokenRepository(context)
        runBlocking {
            tokenRepository.saveToken(
                token = "expired_token_for_testing",
                expiresIn = -3600
            )
        }
    }

    fun setTokenExpiringSoon(context: Context, secondsUntilExpiration: Int = 5) {
        val tokenRepository = TokenRepository(context)
        runBlocking {
            tokenRepository.saveToken(
                token = "token_expiring_soon",
                expiresIn = secondsUntilExpiration
            )
        }
    }

    fun clearAllTokens(context: Context) {
        val tokenRepository = TokenRepository(context)
        runBlocking {
            tokenRepository.clearToken()
        }
    }

    fun isTokenExpired(context: Context): Boolean {
        val tokenRepository = TokenRepository(context)
        return runBlocking {
            tokenRepository.isTokenExpired()
        }
    }
}
