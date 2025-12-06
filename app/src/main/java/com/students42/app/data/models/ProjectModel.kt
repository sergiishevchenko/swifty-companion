package com.students42.app.data.models

import com.google.gson.annotations.SerializedName

data class ProjectModel(
    val id: Int,
    val status: String?,
    @SerializedName("final_mark")
    val finalMark: Int?,
    val validated: Boolean?,
    @SerializedName("marked_at")
    val markedAt: String?,
    @SerializedName("marked")
    val marked: Boolean?,
    val project: ProjectInfoModel?
) {
    val name: String?
        get() = project?.name

    val isCompleted: Boolean
        get() {
            if (status == "finished" || status == "completed") return true
            if (validated == true && finalMark != null && finalMark >= 0) return true
            if (marked == true && finalMark != null && finalMark >= 0) return true
            return false
        }

    val isFailed: Boolean
        get() {
            if (status == "failed") return true
            if (validated == false) return true
            if (finalMark != null && finalMark < 0) return true
            if (marked == true && finalMark != null && finalMark < 0) return true
            return false
        }
}

data class ProjectInfoModel(
    val id: Int?,
    val name: String?,
    val slug: String?
)
