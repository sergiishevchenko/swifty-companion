package com.students42.app.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.students42.app.data.api.ApiService
import com.students42.app.data.api.AuthInterceptor
import com.students42.app.data.local.TokenRepository
import com.students42.app.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.students42.app.data.api.SimpleLoggingInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideSimpleLoggingInterceptor(): SimpleLoggingInterceptor {
        return SimpleLoggingInterceptor()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenRepository: TokenRepository,
        apiServiceProvider: javax.inject.Provider<ApiService>,
        @ClientId clientId: String,
        @ClientSecret clientSecret: String
    ): AuthInterceptor {
        return AuthInterceptor(
            tokenRepository = tokenRepository,
            apiServiceProvider = apiServiceProvider,
            clientId = clientId,
            clientSecret = clientSecret
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: SimpleLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }
}
