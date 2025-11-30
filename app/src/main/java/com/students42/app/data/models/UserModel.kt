package com.students42.app.data.models

import com.google.gson.annotations.SerializedName

data class UserModel(
    val id: Int,
    val login: String,
    val email: String?,
    val mobile: String?,
    val level: Double,
    val location: String?,
    val wallet: Int,
    val evaluations: Int,
    @SerializedName("image_url")
    val imageUrl: String?,
    val skills: List<SkillModel>?,
    @SerializedName("projects_users")
    val projectsUsers: List<ProjectModel>?
)
