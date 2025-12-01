package com.students42.app.data.repositories

import com.students42.app.data.api.ApiService
import com.students42.app.data.models.ProjectModel
import com.students42.app.data.models.SkillModel
import com.students42.app.data.models.UserModel
import com.students42.app.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(
    private val apiService: ApiService
) {
    fun getUserInfo(login: String): Flow<Result<UserModel>> = flow {
        emit(Result.Loading)
        try {
            val user = withContext(Dispatchers.IO) {
                apiService.getUserInfo(login)
            }
            emit(Result.Success(user))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    fun getUserSkills(userId: Int): Flow<Result<List<SkillModel>>> = flow {
        emit(Result.Loading)
        try {
            val skills = withContext(Dispatchers.IO) {
                apiService.getUserSkills(userId)
            }
            emit(Result.Success(skills))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    fun getUserProjects(userId: Int): Flow<Result<List<ProjectModel>>> = flow {
        emit(Result.Loading)
        try {
            val projects = withContext(Dispatchers.IO) {
                apiService.getUserProjects(userId)
            }
            emit(Result.Success(projects))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
}
