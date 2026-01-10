package com.students42.app.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.students42.app.utils.Constants
import com.students42.app.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

class TokenRepository(private val context: Context) {
    private val accessTokenKey = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)
    private val tokenExpiresAtKey = longPreferencesKey(Constants.KEY_TOKEN_EXPIRES_AT)
    private val refreshTokenKey = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)

    suspend fun saveToken(token: String, expiresIn: Int? = null) {
        val expiresAt = if (expiresIn != null) {
            System.currentTimeMillis() + (expiresIn * 1000L)
        } else {
            null
        }
        context.dataStore.edit { preferences ->
            preferences[accessTokenKey] = token
            expiresAt?.let {
                preferences[tokenExpiresAtKey] = it
            }
        }
        Log.d("TokenRepository", "=== ACCESS TOKEN SAVED ===")
        Log.d("TokenRepository", "Access Token: $token")
        if (expiresIn != null && expiresAt != null) {
            Log.d("TokenRepository", "Expires In: $expiresIn seconds")
            Log.d("TokenRepository", "Expires At: $expiresAt (${java.util.Date(expiresAt)})")
        }
    }

    suspend fun saveRefreshToken(refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[refreshTokenKey] = refreshToken
        }
        Log.d("TokenRepository", "=== REFRESH TOKEN SAVED ===")
        Log.d("TokenRepository", "Refresh Token: $refreshToken")
    }

    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[accessTokenKey]
        }
    }

    suspend fun getTokenSync(): String? {
        val token = context.dataStore.data.map { preferences ->
            preferences[accessTokenKey]
        }.first()
        Log.d("TokenRepository", "=== ACCESS TOKEN RETRIEVED ===")
        Log.d("TokenRepository", "Access Token: ${token ?: "null"}")
        return token
    }

    fun getTokenExpiresAt(): Flow<Long?> {
        return context.dataStore.data.map { preferences ->
            preferences[tokenExpiresAtKey]
        }
    }

    suspend fun isTokenExpired(): Boolean {
        val expiresAt = context.dataStore.data.map { preferences ->
            preferences[tokenExpiresAtKey]
        }.first()

        val expired = expiresAt != null && expiresAt < System.currentTimeMillis()
        Log.d("TokenRepository", "=== TOKEN EXPIRATION CHECK ===")
        Log.d("TokenRepository", "Expires At: ${expiresAt ?: "null"} ${if (expiresAt != null) "(${java.util.Date(expiresAt)})" else ""}")
        Log.d("TokenRepository", "Current Time: ${System.currentTimeMillis()} (${java.util.Date()})")
        Log.d("TokenRepository", "Is Expired: $expired")
        return expired
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(tokenExpiresAtKey)
            preferences.remove(refreshTokenKey)
        }
        Log.d("TokenRepository", "=== TOKENS CLEARED ===")
    }

    suspend fun getRefreshToken(): String? {
        val refreshToken = context.dataStore.data.map { preferences ->
            preferences[refreshTokenKey]
        }.first()
        Log.d("TokenRepository", "=== REFRESH TOKEN RETRIEVED ===")
        Log.d("TokenRepository", "Refresh Token: ${refreshToken ?: "null"}")
        return refreshToken
    }

    suspend fun getValidToken(): String? {
        val token = getTokenSync()
        if (token != null && !isTokenExpired()) {
            return token
        }
        return null
    }

    suspend fun refreshTokenIfNeeded(
        refreshToken: String?,
        onRefresh: suspend (String) -> Result<Pair<String, Int?>>
    ): Boolean {
        if (!isTokenExpired()) {
            return true
        }

        val refreshTokenValue = refreshToken ?: getRefreshToken() ?: return false

        return try {
            val result = onRefresh(refreshTokenValue)
            when (result) {
                is Result.Success<Pair<String, Int?>> -> {
                    val (newToken, expiresIn) = result.data
                    saveToken(newToken, expiresIn)
                    true
                }
                is Result.Error -> {
                    false
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
}
