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

    val isCommonOrAdvanced: Boolean
        get() {
            return !isPiscine
        }

    val isFailed: Boolean
        get() {
            val result = when {
                validated == false -> {
                    try {
                        android.util.Log.d("ProjectModel", "Project ${name}: validated=false -> FAILED")
                    } catch (e: Exception) {
                    }
                    true
                }
                status == "failed" -> {
                    try {
                        android.util.Log.d("ProjectModel", "Project ${name}: status=failed -> FAILED")
                    } catch (e: Exception) {
                    }
                    true
                }
                finalMark != null && finalMark < 0 -> {
                    try {
                        android.util.Log.d("ProjectModel", "Project ${name}: finalMark=$finalMark < 0 -> FAILED")
                    } catch (e: Exception) {
                    }
                    true
                }
                marked == true && finalMark != null && finalMark < 0 -> {
                    try {
                        android.util.Log.d("ProjectModel", "Project ${name}: marked=true, finalMark=$finalMark < 0 -> FAILED")
                    } catch (e: Exception) {
                    }
                    true
                }
                else -> false
            }
            return result
        }

    val isCompleted: Boolean
        get() {
            if (isFailed) {
                try {
                    android.util.Log.d("ProjectModel", "Project ${name}: isFailed=true -> NOT COMPLETED")
                } catch (e: Exception) {
                }
                return false
            }
            if (validated == true && finalMark != null && finalMark >= 0) {
                try {
                    android.util.Log.d("ProjectModel", "Project ${name}: validated=true, finalMark=$finalMark >= 0 -> COMPLETED")
                } catch (e: Exception) {
                }
                return true
            }
            if (status == "finished" || status == "completed") {
                if (validated == true) {
                    try {
                        android.util.Log.d("ProjectModel", "Project ${name}: status=$status, validated=true -> COMPLETED")
                    } catch (e: Exception) {
                    }
                    return true
                }
                if (validated == null && finalMark != null && finalMark >= 0) {
                    try {
                        android.util.Log.d("ProjectModel", "Project ${name}: status=$status, validated=null, finalMark=$finalMark >= 0 -> COMPLETED")
                    } catch (e: Exception) {
                    }
                    return true
                }
            }
            if (marked == true && finalMark != null && finalMark >= 0) {
                try {
                    android.util.Log.d("ProjectModel", "Project ${name}: marked=true, finalMark=$finalMark >= 0 -> COMPLETED")
                } catch (e: Exception) {
                }
                return true
            }
            try {
                android.util.Log.d("ProjectModel", "Project ${name}: no conditions met -> NOT COMPLETED")
            } catch (e: Exception) {
            }
            return false
        }
}

data class ProjectInfoModel(
    val id: Int?,
    val name: String?,
    val slug: String?
)
