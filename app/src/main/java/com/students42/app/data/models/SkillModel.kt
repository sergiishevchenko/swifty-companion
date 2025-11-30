package com.students42.app.data.models

import com.google.gson.annotations.SerializedName

data class SkillModel(
    val id: Int,
    val name: String,
    val level: Double,
    val percentage: Double
)
