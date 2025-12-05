package com.students42.app.data.models

import com.google.gson.annotations.SerializedName

data class LocationModel(
    val id: Int?,
    val host: String?,
    @SerializedName("end_at")
    val endAt: String?,
    @SerializedName("begin_at")
    val beginAt: String?
)
