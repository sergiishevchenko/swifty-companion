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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) : AndroidViewModel(context as Application) {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()


    fun loadUserProfileByLogin(login: String) {
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
                        _profileState.value = ProfileState.Error(errorMessage)
                    }
                }
            }
        }
    }

    private fun loadUserData(userId: Int, user: com.students42.app.data.models.UserModel) {
        viewModelScope.launch {
            var skills: List<SkillModel>? = null
            var projects: List<ProjectModel>? = null

            launch {
                userRepository.getUserSkills(userId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            skills = result.data
                            if (skills != null && projects != null) {
                                _profileState.value = ProfileState.Success(
                                    user = user,
                                    skills = skills!!,
                                    projects = projects!!
                                )
                            }
                        }
                        is Result.Error -> {
                            if (projects != null) {
                                _profileState.value = ProfileState.Success(
                                    user = user,
                                    skills = emptyList(),
                                    projects = projects!!
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }

            launch {
                userRepository.getUserProjects(userId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            projects = result.data
                            if (skills != null && projects != null) {
                                _profileState.value = ProfileState.Success(
                                    user = user,
                                    skills = skills!!,
                                    projects = projects!!
                                )
                            }
                        }
                        is Result.Error -> {
                            if (skills != null) {
                                _profileState.value = ProfileState.Success(
                                    user = user,
                                    skills = skills!!,
                                    projects = emptyList()
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
