package com.students42.app.data.models

import com.google.gson.annotations.SerializedName

data class ProjectModel(
    val id: Int,
    val status: String?,
    @SerializedName("final_mark")
    val finalMark: Int?,
    @SerializedName("validated?")
    val validated: Boolean?,
    @SerializedName("marked_at")
    val markedAt: String?,
    @SerializedName("marked")
    val marked: Boolean?,
    val project: ProjectInfoModel?,
    val cursus: List<CursusModel>?
) {
    val name: String?
        get() = project?.name

    val isPiscine: Boolean
        get() {
            if (!cursus.isNullOrEmpty()) {
                return cursus.any {
                    val slug = it.slug?.lowercase() ?: ""
                    slug.contains("piscine", ignoreCase = true)
                }
            }
            val projectSlug = project?.slug?.lowercase() ?: ""
            return projectSlug.contains("piscine", ignoreCase = true)
        }

    val isCommonOrAdvanced: Boolean
        get() {
            return !isPiscine
        }

    val isFailed: Boolean
        get() {
            return when {
                validated == false -> true
                status == "failed" -> true
                finalMark != null && finalMark < 0 -> true
                marked == true && finalMark != null && finalMark < 0 -> true
                else -> false
            }
        }

    val isCompleted: Boolean
        get() {
            if (isFailed) {
                return false
            }
            if (validated == true && finalMark != null && finalMark >= 0) {
                return true
            }
            if (status == "finished" || status == "completed") {
                if (validated == true) {
                    return true
                }
                if (validated == null && finalMark != null && finalMark >= 0) {
                    return true
                }
            }
            if (marked == true && finalMark != null && finalMark >= 0) {
                return true
            }
            return false
        }
}

data class ProjectInfoModel(
    val id: Int?,
    val name: String?,
    val slug: String?
)
