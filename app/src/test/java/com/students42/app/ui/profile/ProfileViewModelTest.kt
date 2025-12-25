package com.students42.app.ui.profile

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.students42.app.R
import com.students42.app.data.models.CursusUserModel
import com.students42.app.data.models.ProjectModel
import com.students42.app.data.models.SkillModel
import com.students42.app.data.models.UserModel
import com.students42.app.data.repositories.UserRepository
import com.students42.app.utils.Result
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: ProfileViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext() as Application
        context = application
        userRepository = mock()
        viewModel = ProfileViewModel(
            context = application,
            userRepository = userRepository
        )
    }

    @Test
    fun `loadUserProfileByLogin sets Loading then Success when user data loaded`() = runTest(testDispatcher) {
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
        val skills = listOf(
            SkillModel(id = 1, name = "C", level = 10.0, percentage = 50.0)
        )
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

        whenever(userRepository.getUserInfo("testuser")).thenReturn(
            flowOf(Result.Loading, Result.Success(user))
        )
        whenever(userRepository.getUserSkills(1)).thenReturn(
            flowOf(Result.Loading, Result.Success(skills))
        )
        whenever(userRepository.getUserProjects(1)).thenReturn(
            flowOf(Result.Loading, Result.Success(projects))
        )

        viewModel.loadUserProfileByLogin("testuser")
        advanceUntilIdle()

        assertTrue(viewModel.profileState.value is ProfileState.Success)
        val successState = viewModel.profileState.value as ProfileState.Success
        assertEquals(user, successState.user)
        assertEquals(skills, successState.skills)
        assertEquals(projects, successState.projects)
    }

    @Test
    fun `loadUserProfileByLogin sets Loading then Error when user fetch fails`() = runTest(testDispatcher) {
        val exception = RuntimeException("API error")
        whenever(userRepository.getUserInfo("testuser")).thenReturn(
            flowOf(Result.Loading, Result.Error(exception))
        )

        viewModel.loadUserProfileByLogin("testuser")
        advanceUntilIdle()

        assertTrue(viewModel.profileState.value is ProfileState.Error)
    }

    @Test
    fun `loadUserProfileByLogin merges skills from API and cursus`() = runTest(testDispatcher) {
        val cursusUser = CursusUserModel(
            id = 1,
            level = 10.0,
            grade = null,
            blackholedAt = null,
            beginAt = null,
            endAt = null,
            cursus = com.students42.app.data.models.CursusModel(id = 1, name = "42 Cursus", slug = "42cursus"),
            skills = listOf(
                SkillModel(id = 2, name = "Unix", level = 5.0, percentage = 30.0)
            )
        )
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = "test@example.com",
            location = null,
            wallet = null,
            image = null,
            skills = null,
            projectsUsers = null,
            cursusUsers = listOf(cursusUser),
            campus = null,
            campusUsers = null,
            correctionPoint = null
        )
        val apiSkills = listOf(
            SkillModel(id = 1, name = "C", level = 10.0, percentage = 50.0)
        )

        whenever(userRepository.getUserInfo("testuser")).thenReturn(
            flowOf(Result.Loading, Result.Success(user))
        )
        whenever(userRepository.getUserSkills(1)).thenReturn(
            flowOf(Result.Loading, Result.Success(apiSkills))
        )
        whenever(userRepository.getUserProjects(1)).thenReturn(
            flowOf(Result.Loading, Result.Success(emptyList()))
        )

        viewModel.loadUserProfileByLogin("testuser")
        advanceUntilIdle()

        assertTrue(viewModel.profileState.value is ProfileState.Success)
        val successState = viewModel.profileState.value as ProfileState.Success
        assertEquals(2, successState.skills.size)
    }

    @Test
    fun `loadUserProfileByLogin filters only completed and failed projects`() = runTest(testDispatcher) {
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
        val projects = listOf(
            ProjectModel(
                id = 1,
                status = "finished",
                finalMark = 100,
                validated = true,
                markedAt = null,
                marked = null,
                project = com.students42.app.data.models.ProjectInfoModel(id = 1, name = "Completed Project", slug = "completed"),
                cursus = null
            ),
            ProjectModel(
                id = 2,
                status = "in_progress",
                finalMark = null,
                validated = null,
                markedAt = null,
                marked = null,
                project = com.students42.app.data.models.ProjectInfoModel(id = 2, name = "In Progress Project", slug = "in-progress"),
                cursus = null
            ),
            ProjectModel(
                id = 3,
                status = "finished",
                finalMark = 0,
                validated = false,
                markedAt = null,
                marked = null,
                project = com.students42.app.data.models.ProjectInfoModel(id = 3, name = "Failed Project", slug = "failed"),
                cursus = null
            )
        )

        whenever(userRepository.getUserInfo("testuser")).thenReturn(
            flowOf(Result.Loading, Result.Success(user))
        )
        whenever(userRepository.getUserSkills(1)).thenReturn(
            flowOf(Result.Loading, Result.Success(emptyList()))
        )
        whenever(userRepository.getUserProjects(1)).thenReturn(
            flowOf(Result.Loading, Result.Success(projects))
        )

        viewModel.loadUserProfileByLogin("testuser")
        advanceUntilIdle()

        assertTrue(viewModel.profileState.value is ProfileState.Success)
        val successState = viewModel.profileState.value as ProfileState.Success
        assertEquals(2, successState.projects.size)
        assertTrue(successState.projects.any { it.id == 1 })
        assertTrue(successState.projects.any { it.id == 3 })
    }

    @Test
    fun `retry calls performLoadUserProfile with lastLogin`() = runTest(testDispatcher) {
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

        whenever(userRepository.getUserInfo("testuser")).thenReturn(
            flowOf(Result.Loading, Result.Success(user))
        )
        whenever(userRepository.getUserSkills(1)).thenReturn(
            flowOf(Result.Loading, Result.Success(emptyList()))
        )
        whenever(userRepository.getUserProjects(1)).thenReturn(
            flowOf(Result.Loading, Result.Success(emptyList()))
        )

        viewModel.loadUserProfileByLogin("testuser")
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        verify(userRepository, org.mockito.kotlin.times(2)).getUserInfo("testuser")
    }
}
