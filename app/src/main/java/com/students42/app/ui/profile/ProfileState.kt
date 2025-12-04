package com.students42.app.ui.profile

import com.students42.app.data.models.ProjectModel
import com.students42.app.data.models.SkillModel
import com.students42.app.data.models.UserModel

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(
        val user: UserModel,
        val skills: List<SkillModel>,
        val projects: List<ProjectModel>
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
