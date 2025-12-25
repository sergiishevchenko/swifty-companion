package com.students42.app.data.api

import com.students42.app.data.models.*
import retrofit2.http.*

interface ApiService {
    @GET("v2/users/{login}")
    suspend fun getUserInfo(@Path("login") login: String): UserModel

    @GET("v2/users/{userId}/skills")
    suspend fun getUserSkills(@Path("userId") userId: Int): List<SkillModel>

    @GET("v2/users/{userId}/projects_users")
    suspend fun getUserProjects(@Path("userId") userId: Int): List<ProjectModel>

    @GET("v2/users/{userId}/locations")
    suspend fun getUserLocations(@Path("userId") userId: Int): List<LocationModel>

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun getToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String
    ): TokenResponse

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String
    ): TokenResponse
}
