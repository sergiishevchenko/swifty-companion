package com.students42.app.ui.profile

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.students42.app.R
import com.students42.app.data.models.ProjectModel
import com.students42.app.data.models.SkillModel
import com.students42.app.data.repositories.UserRepository
import com.students42.app.utils.ErrorHandler
import com.students42.app.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) : AndroidViewModel(context as Application) {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private var lastLogin: String? = null

    fun loadUserProfileByLogin(login: String) {
        lastLogin = login
        performLoadUserProfile(login)
    }

    private fun performLoadUserProfile(login: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            userRepository.getUserInfo(login).collect { userResult ->
                when (userResult) {
                    is Result.Loading -> {
                        _profileState.value = ProfileState.Loading
                    }
                    is Result.Success -> {
                        val userId = userResult.data.id
                        loadUserData(userId, userResult.data)
                    }
                    is Result.Error -> {
                        val errorMessage = ErrorHandler.handleError(context, userResult.exception)
                        _profileState.value = ProfileState.Error(
                            errorMessage,
                            retryAction = { performLoadUserProfile(login) }
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        lastLogin?.let { login ->
            performLoadUserProfile(login)
        }
    }

    private fun loadUserData(userId: Int, user: com.students42.app.data.models.UserModel) {
        viewModelScope.launch {
            val skillsResult = async {
                try {
                    userRepository.getUserSkills(userId).first { it is Result.Success || it is Result.Error }
                } catch (e: Exception) {
                    Result.Error(e) as Result<List<SkillModel>>
                }
            }

            val projectsResult = async {
                try {
                    userRepository.getUserProjects(userId).first { it is Result.Success || it is Result.Error }
                } catch (e: Exception) {
                    Result.Error(e) as Result<List<ProjectModel>>
                }
            }

            val skillsFromApi = when (val result = skillsResult.await()) {
                is Result.Success -> result.data
                else -> emptyList()
            }

            val projectsFromApi = when (val result = projectsResult.await()) {
                is Result.Success -> result.data
                else -> emptyList()
            }

            val skillsFromCursus = user.cursusUsers
                ?.flatMap { it.skills ?: emptyList() }
                ?: emptyList()

            val projectsFromUser = user.projectsUsers ?: emptyList()

            val skills = if (skillsFromApi.isNotEmpty()) {
                skillsFromApi
            } else {
                skillsFromCursus
            }

            val allProjects = if (projectsFromApi.isNotEmpty()) {
                projectsFromApi
            } else {
                projectsFromUser
            }

            val projects = allProjects.filter { project ->
                project.isCompleted || project.isFailed
            }

            _profileState.value = ProfileState.Success(
                user = user,
                skills = skills,
                projects = projects
            )
        }
    }
}
