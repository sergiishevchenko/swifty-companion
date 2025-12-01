package com.students42.app.data.api

import com.students42.app.data.models.ProjectModel
import com.students42.app.data.models.SkillModel
import com.students42.app.data.models.TokenResponse
import com.students42.app.data.models.UserModel
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("v2/users/{login}")
    suspend fun getUserInfo(@Path("login") login: String): UserModel

    @GET("v2/users/{userId}/skills")
    suspend fun getUserSkills(@Path("userId") userId: Int): List<SkillModel>

    @GET("v2/users/{userId}/projects_users")
    suspend fun getUserProjects(@Path("userId") userId: Int): List<ProjectModel>

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun getToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String
    ): TokenResponse
}
