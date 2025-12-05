package com.students42.app.data.models

import com.google.gson.annotations.SerializedName

data class CursusUserModel(
    val id: Int?,
    val level: Double?,
    @SerializedName("grade")
    val grade: String?,
    @SerializedName("blackholed_at")
    val blackholedAt: String?,
    @SerializedName("begin_at")
    val beginAt: String?,
    @SerializedName("end_at")
    val endAt: String?,
    val cursus: CursusModel?
)

data class CursusModel(
    val id: Int?,
    val name: String?,
    val slug: String?
)
