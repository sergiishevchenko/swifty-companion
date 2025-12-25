package com.students42.app.data.repositories

import com.students42.app.data.api.ApiService
import com.students42.app.data.models.ProjectModel
import com.students42.app.data.models.SkillModel
import com.students42.app.data.models.UserModel
import com.students42.app.utils.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UserRepositoryTest {
    private lateinit var apiService: ApiService
    private lateinit var userRepository: UserRepository

    @Before
    fun setup() {
        apiService = mock()
        userRepository = UserRepository(apiService)
    }

    @Test
    fun `getUserInfo emits Loading then Success when API call succeeds`() = runTest {
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = "test@example.com",
            location = null,
            wallet = null,
            image = null,
            skills = null,
            projectsUsers = null,
            cursusUsers = null,
            campus = null,
            campusUsers = null,
            correctionPoint = null
        )
        whenever(apiService.getUserInfo("testuser")).thenReturn(user)

        val result = userRepository.getUserInfo("testuser")

        val values = result.take(2).toList()
        assertEquals(2, values.size)
        assertTrue(values[0] is Result.Loading)
        assertTrue(values[1] is Result.Success)
        assertEquals(user, (values[1] as Result.Success).data)
    }

    @Test
    fun `getUserInfo emits Loading then Error when API call fails`() = runTest {
        val exception = RuntimeException("API error")
        whenever(apiService.getUserInfo("testuser")).thenThrow(exception)

        val result = userRepository.getUserInfo("testuser")

        val values = result.take(2).toList()
        assertEquals(2, values.size)
        assertTrue(values[0] is Result.Loading)
        assertTrue(values[1] is Result.Error)
        assertEquals(exception, (values[1] as Result.Error).exception)
    }

    @Test
    fun `getUserSkills emits Loading then Success when API call succeeds`() = runTest {
        val skills = listOf(
            SkillModel(id = 1, name = "C", level = 10.0, percentage = 50.0)
        )
        whenever(apiService.getUserSkills(1)).thenReturn(skills)

        val result = userRepository.getUserSkills(1)

        val values = result.take(2).toList()
        assertEquals(2, values.size)
        assertTrue(values[0] is Result.Loading)
        assertTrue(values[1] is Result.Success)
        assertEquals(skills, (values[1] as Result.Success).data)
    }

    @Test
    fun `getUserSkills emits Loading then Error when API call fails`() = runTest {
        val exception = RuntimeException("API error")
        whenever(apiService.getUserSkills(1)).thenThrow(exception)

        val result = userRepository.getUserSkills(1)

        val values = result.take(2).toList()
        assertEquals(2, values.size)
        assertTrue(values[0] is Result.Loading)
        assertTrue(values[1] is Result.Error)
        assertEquals(exception, (values[1] as Result.Error).exception)
    }

    @Test
    fun `getUserProjects emits Loading then Success when API call succeeds`() = runTest {
        val projects = listOf(
            ProjectModel(
                id = 1,
                status = "finished",
                finalMark = 100,
                validated = true,
                markedAt = null,
                marked = null,
                project = com.students42.app.data.models.ProjectInfoModel(id = 1, name = "Test Project", slug = "test-project"),
                cursus = null
            )
        )
        whenever(apiService.getUserProjects(1)).thenReturn(projects)

        val result = userRepository.getUserProjects(1)

        val values = result.take(2).toList()
        assertEquals(2, values.size)
        assertTrue(values[0] is Result.Loading)
        assertTrue(values[1] is Result.Success)
        assertEquals(projects, (values[1] as Result.Success).data)
    }

    @Test
    fun `getUserProjects emits Loading then Error when API call fails`() = runTest {
        val exception = RuntimeException("API error")
        whenever(apiService.getUserProjects(1)).thenThrow(exception)

        val result = userRepository.getUserProjects(1)

        val values = result.take(2).toList()
        assertEquals(2, values.size)
        assertTrue(values[0] is Result.Loading)
        assertTrue(values[1] is Result.Error)
        assertEquals(exception, (values[1] as Result.Error).exception)
    }
}
