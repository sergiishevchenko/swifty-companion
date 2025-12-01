package com.students42.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.students42.app.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

class TokenRepository(private val context: Context) {
    private val accessTokenKey = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)
    private val tokenExpiresAtKey = longPreferencesKey(Constants.KEY_TOKEN_EXPIRES_AT)
    private val refreshTokenKey = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)

    suspend fun saveToken(token: String, expiresIn: Int? = null) {
        context.dataStore.edit { preferences ->
            preferences[accessTokenKey] = token
            if (expiresIn != null) {
                val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)
                preferences[tokenExpiresAtKey] = expiresAt
            }
        }
    }

    suspend fun saveRefreshToken(refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[refreshTokenKey] = refreshToken
        }
    }

    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[accessTokenKey]
        }
    }

    suspend fun getTokenSync(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[accessTokenKey]
        }.first()
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
        
        return expiresAt != null && expiresAt < System.currentTimeMillis()
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(tokenExpiresAtKey)
            preferences.remove(refreshTokenKey)
        }
    }
}
