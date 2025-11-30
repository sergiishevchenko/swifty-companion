package com.students42.app.data.models

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String = "Bearer",
    @SerializedName("expires_in")
    val expiresIn: Int? = null,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    val scope: String? = null
)
