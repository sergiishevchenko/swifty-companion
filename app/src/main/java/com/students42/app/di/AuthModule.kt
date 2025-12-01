package com.students42.app.di

import android.content.Context
import com.students42.app.auth.AuthService
import com.students42.app.data.api.ApiService
import com.students42.app.data.local.TokenRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideClientId(): String {
        return "your_client_id"
    }

    @Provides
    @Singleton
    fun provideClientSecret(): String {
        return "your_client_secret"
    }

    @Provides
    @Singleton
    fun provideRedirectUri(): String {
        return "students42://oauth/callback"
    }

    @Provides
    @Singleton
    fun provideAuthService(
        @ApplicationContext context: Context,
        apiService: ApiService,
        tokenRepository: TokenRepository,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): AuthService {
        return AuthService(
            context = context,
            apiService = apiService,
            tokenRepository = tokenRepository,
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = redirectUri
        )
    }
}
