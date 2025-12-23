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

    val isAdvancedCore: Boolean
        get() {
            if (!cursus.isNullOrEmpty()) {
                val found = cursus.any { 
                    val slug = it.slug?.lowercase() ?: ""
                    val name = it.name?.lowercase() ?: ""
                    slug.contains("advanced", ignoreCase = true) ||
                    name.contains("advanced", ignoreCase = true)
                }
                if (found) return true
            }
            
            val projectSlug = project?.slug?.lowercase() ?: ""
            val projectName = project?.name?.lowercase() ?: ""
            return projectSlug.contains("advanced", ignoreCase = true) ||
                   projectName.contains("advanced", ignoreCase = true) ||
                   projectName.contains("hangout", ignoreCase = true) ||
                   projectName.contains("work experience", ignoreCase = true) ||
                   projectName.contains("workexperience", ignoreCase = true)
        }

    val isCommonCore: Boolean
        get() {
            if (isPiscine) return false
            
            val projectSlug = project?.slug?.lowercase() ?: ""
            val projectName = project?.name?.lowercase() ?: ""
            
            if (projectSlug.contains("piscine", ignoreCase = true) || 
                projectName.contains("piscine", ignoreCase = true) ||
                projectSlug.contains("advanced", ignoreCase = true) ||
                projectName.contains("advanced", ignoreCase = true) ||
                projectName.contains("hangout", ignoreCase = true) ||
                projectName.contains("work experience", ignoreCase = true) ||
                projectName.contains("workexperience", ignoreCase = true)) {
                return false
            }
            
            if (!cursus.isNullOrEmpty()) {
                val hasAdvancedCursus = cursus.any { 
                    val slug = it.slug?.lowercase() ?: ""
                    val name = it.name?.lowercase() ?: ""
                    slug.contains("advanced", ignoreCase = true) ||
                    name.contains("advanced", ignoreCase = true)
                }
                if (hasAdvancedCursus) return false
                
                val found = cursus.any { 
                    val slug = it.slug?.lowercase() ?: ""
                    val name = it.name?.lowercase() ?: ""
                    slug == "42cursus" || 
                    slug == "42" ||
                    name.contains("common core", ignoreCase = true) ||
                    name.contains("42 cursus", ignoreCase = true)
                }
                if (found) return true
            }
            
            return false
        }

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
