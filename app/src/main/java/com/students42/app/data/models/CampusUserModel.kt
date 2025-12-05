package com.students42.app.data.models

import com.google.gson.annotations.SerializedName

data class CampusUserModel(
    val id: Int?,
    @SerializedName("user_id")
    val userId: Int?,
    @SerializedName("campus_id")
    val campusId: Int?,
    @SerializedName("is_primary")
    val isPrimary: Boolean?
)

data class CampusModel(
    val id: Int?,
    val name: String?,
    val city: String?,
    val country: String?
)
