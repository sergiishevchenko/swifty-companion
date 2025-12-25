package com.students42.app.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectModelTest {
    private fun createProject(
        name: String? = "Test Project",
        slug: String? = "test-project",
        cursus: List<CursusModel>? = null,
        validated: Boolean? = null,
        status: String? = null,
        finalMark: Int? = null,
        marked: Boolean? = null
    ): ProjectModel {
        return ProjectModel(
            id = 1,
            status = status,
            finalMark = finalMark,
            validated = validated,
            markedAt = null,
            marked = marked,
            project = ProjectInfoModel(id = 1, name = name, slug = slug),
            cursus = cursus
        )
    }

    @Test
    fun `isPiscine returns true when cursus contains piscine`() {
        val cursus = listOf(CursusModel(id = 1, name = "Piscine", slug = "piscine"))
        val project = createProject(cursus = cursus)
        assertTrue(project.isPiscine)
    }

    @Test
    fun `isPiscine returns true when project slug contains piscine`() {
        val project = createProject(slug = "piscine-c-00")
        assertTrue(project.isPiscine)
    }

    @Test
    fun `isPiscine returns false for non-piscine project`() {
        val cursus = listOf(CursusModel(id = 1, name = "42 Cursus", slug = "42cursus"))
        val project = createProject(cursus = cursus)
        assertFalse(project.isPiscine)
    }

    @Test
    fun `isAdvancedCore returns true when cursus contains advanced`() {
        val cursus = listOf(CursusModel(id = 1, name = "Advanced", slug = "advanced"))
        val project = createProject(cursus = cursus)
        assertTrue(project.isAdvancedCore)
    }

    @Test
    fun `isAdvancedCore returns true when project name contains hangout`() {
        val project = createProject(name = "Hangout Project")
        assertTrue(project.isAdvancedCore)
    }

    @Test
    fun `isAdvancedCore returns true when project name contains work experience`() {
        val project = createProject(name = "Work Experience Project")
        assertTrue(project.isAdvancedCore)
    }

    @Test
    fun `isCommonCore returns false when isPiscine is true`() {
        val cursus = listOf(CursusModel(id = 1, name = "Piscine", slug = "piscine"))
        val project = createProject(cursus = cursus)
        assertFalse(project.isCommonCore)
    }

    @Test
    fun `isCommonCore returns true when cursus is 42cursus`() {
        val cursus = listOf(CursusModel(id = 1, name = "42 Cursus", slug = "42cursus"))
        val project = createProject(cursus = cursus)
        assertTrue(project.isCommonCore)
    }

    @Test
    fun `isCommonOrAdvanced returns false for piscine`() {
        val cursus = listOf(CursusModel(id = 1, name = "Piscine", slug = "piscine"))
        val project = createProject(cursus = cursus)
        assertFalse(project.isCommonOrAdvanced)
    }

    @Test
    fun `isCommonOrAdvanced returns true for non-piscine`() {
        val cursus = listOf(CursusModel(id = 1, name = "42 Cursus", slug = "42cursus"))
        val project = createProject(cursus = cursus)
        assertTrue(project.isCommonOrAdvanced)
    }

    @Test
    fun `isFailed returns true when validated is false`() {
        val project = createProject(validated = false)
        assertTrue(project.isFailed)
    }

    @Test
    fun `isFailed returns true when status is failed`() {
        val project = createProject(status = "failed")
        assertTrue(project.isFailed)
    }

    @Test
    fun `isFailed returns true when finalMark is negative`() {
        val project = createProject(finalMark = -42)
        assertTrue(project.isFailed)
    }

    @Test
    fun `isFailed returns true when marked is true and finalMark is negative`() {
        val project = createProject(marked = true, finalMark = -1)
        assertTrue(project.isFailed)
    }

    @Test
    fun `isFailed returns false when project is valid`() {
        val project = createProject(validated = true, finalMark = 100)
        assertFalse(project.isFailed)
    }

    @Test
    fun `isCompleted returns false when isFailed is true`() {
        val project = createProject(validated = false, finalMark = 0)
        assertFalse(project.isCompleted)
    }

    @Test
    fun `isCompleted returns true when validated is true and finalMark is non-negative`() {
        val project = createProject(validated = true, finalMark = 100)
        assertTrue(project.isCompleted)
    }

    @Test
    fun `isCompleted returns true when status is finished and validated is true`() {
        val project = createProject(status = "finished", validated = true)
        assertTrue(project.isCompleted)
    }

    @Test
    fun `isCompleted returns true when status is completed and validated is true`() {
        val project = createProject(status = "completed", validated = true)
        assertTrue(project.isCompleted)
    }

    @Test
    fun `isCompleted returns true when status is finished, validated is null and finalMark is non-negative`() {
        val project = createProject(status = "finished", validated = null, finalMark = 100)
        assertTrue(project.isCompleted)
    }

    @Test
    fun `isCompleted returns true when marked is true and finalMark is non-negative`() {
        val project = createProject(marked = true, finalMark = 100)
        assertTrue(project.isCompleted)
    }

    @Test
    fun `isCompleted returns false when no conditions are met`() {
        val project = createProject(status = "in_progress", validated = null, finalMark = null)
        assertFalse(project.isCompleted)
    }

    @Test
    fun `name returns project name`() {
        val project = createProject(name = "Test Project")
        assertEquals("Test Project", project.name)
    }

    @Test
    fun `name returns null when project is null`() {
        val project = ProjectModel(
            id = 1,
            status = null,
            finalMark = null,
            validated = null,
            markedAt = null,
            marked = null,
            project = null,
            cursus = null
        )
        assertEquals(null, project.name)
    }
}
