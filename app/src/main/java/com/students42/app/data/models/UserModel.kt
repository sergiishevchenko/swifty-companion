package com.students42.app.data.models

import com.google.gson.annotations.SerializedName

data class UserModel(
    val id: Int,
    val login: String,
    val email: String?,
    val location: String?,
    val wallet: Int?,
    val image: ImageModel?,
    val skills: List<SkillModel>?,
    @SerializedName("projects_users")
    val projectsUsers: List<ProjectModel>?,
    @SerializedName("cursus_users")
    val cursusUsers: List<CursusUserModel>?,
    val campus: List<CampusModel>?,
    @SerializedName("campus_users")
    val campusUsers: List<CampusUserModel>?,
    @SerializedName("correction_point")
    val correctionPoint: Int?
) {
    val imageUrl: String?
        get() = image?.link

    val level: Double
        get() {
            val cursusUsersList = cursusUsers ?: return 0.0
            
            val activeCursus = cursusUsersList
                .filter { it.endAt == null }
                .maxByOrNull { it.level ?: 0.0 }
            
            val currentCursus = activeCursus ?: cursusUsersList
                .filter { it.endAt != null }
                .maxByOrNull { it.endAt ?: "" }
            
            return currentCursus?.level ?: 0.0
        }

    val locationName: String?
        get() {
            if (location != null && location.isNotEmpty() && location != "null") return location
            
            val primaryCampus = campusUsers?.firstOrNull { it.isPrimary == true }
            val campusId = primaryCampus?.campusId
            val campusName = campus?.firstOrNull { it.id == campusId }?.name
            if (campusName != null && campusName.isNotEmpty()) return campusName
            
            return campus?.firstOrNull()?.name
        }

    val evaluations: Int
        get() = correctionPoint ?: 0
}

data class ImageModel(
    val link: String?,
    val versions: ImageVersionsModel?
)

data class ImageVersionsModel(
    val large: String?,
    val medium: String?,
    val small: String?,
    val micro: String?
)
