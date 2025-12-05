package com.students42.app.di

import android.content.Context
import com.students42.app.BuildConfig
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
    @ClientId
    fun provideClientId(): String {
        return BuildConfig.API_CLIENT_ID
    }

    @Provides
    @Singleton
    @ClientSecret
    fun provideClientSecret(): String {
        return BuildConfig.API_CLIENT_SECRET
    }

    @Provides
    @Singleton
    @RedirectUri
    fun provideRedirectUri(): String {
        return BuildConfig.API_REDIRECT_URI
    }

    @Provides
    @Singleton
    fun provideAuthService(
        @ApplicationContext context: Context,
        apiService: ApiService,
        tokenRepository: TokenRepository,
        @ClientId clientId: String,
        @ClientSecret clientSecret: String,
        @RedirectUri redirectUri: String
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
