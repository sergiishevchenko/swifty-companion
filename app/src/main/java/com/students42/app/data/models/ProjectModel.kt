package com.students42.app.data.models

import com.google.gson.annotations.SerializedName

data class ProjectModel(
    val id: Int,
    val name: String,
    val status: String,
    @SerializedName("final_mark")
    val finalMark: Int?,
    val validated: Boolean?
) {
    val isCompleted: Boolean
        get() = status == "finished" || status == "completed" || (validated == true && finalMark != null && finalMark >= 0)

    val isFailed: Boolean
        get() = status == "failed" || (validated == false) || (finalMark != null && finalMark < 0)
}
