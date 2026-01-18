package com.students42.app.ui.profile

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.students42.app.data.models.ProjectModel
import com.students42.app.data.repositories.UserRepository
import com.students42.app.utils.ErrorHandler
import com.students42.app.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.drop
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
    private var loadUserDataJob: Job? = null

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
        loadUserDataJob?.cancel()
        Log.d("ProfileViewModel", "=== loadUserData called for userId=$userId ===")
        loadUserDataJob = viewModelScope.launch {
            val projectsResult = async {
                try {
                    Log.d("ProfileViewModel", "Starting getUserProjects for userId=$userId")
                    val result = userRepository.getUserProjects(userId).drop(1).first()
                    Log.d("ProfileViewModel", "getUserProjects completed: ${result.javaClass.simpleName}")
                    result
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "getUserProjects exception: ${e.message}", e)
                    Result.Error(e) as Result<List<ProjectModel>>
                }
            }

            val projectsFromApi = when (val result = projectsResult.await()) {
                is Result.Success -> result.data
                else -> emptyList()
            }

            val projectsFromUser = user.projectsUsers ?: emptyList()

            val currentCursus = user.cursusUsers?.let { cursusList ->
                val activeCursus = cursusList
                    .filter { it.endAt == null }
                    .maxByOrNull { it.level ?: 0.0 }

                activeCursus ?: cursusList
                    .filter { it.endAt != null }
                    .maxByOrNull { it.endAt ?: "" }
            }

            val skills = currentCursus?.skills ?: emptyList()

            val allProjects = (projectsFromApi + projectsFromUser)
                .distinctBy { it.id }
                .groupBy { it.name?.lowercase()?.trim() }
                .values
                .mapNotNull { projectsWithSameName ->
                    projectsWithSameName.firstOrNull()
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
