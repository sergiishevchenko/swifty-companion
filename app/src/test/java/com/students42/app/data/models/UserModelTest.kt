package com.students42.app.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserModelTest {
    @Test
    fun `imageUrl returns image link when image exists`() {
        val image = ImageModel(
            link = "https://example.com/image.jpg",
            versions = null
        )
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = null,
            location = null,
            wallet = null,
            image = image,
            skills = null,
            projectsUsers = null,
            cursusUsers = null,
            campus = null,
            campusUsers = null,
            correctionPoint = null
        )

        assertEquals("https://example.com/image.jpg", user.imageUrl)
    }

    @Test
    fun `imageUrl returns null when image is null`() {
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = null,
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

        assertNull(user.imageUrl)
    }

    @Test
    fun `level returns highest level from active cursus`() {
        val cursusUsers = listOf(
            CursusUserModel(
                id = 1,
                level = 10.0,
                grade = null,
                blackholedAt = null,
                beginAt = null,
                endAt = null,
                cursus = null,
                skills = null
            ),
            CursusUserModel(
                id = 2,
                level = 15.0,
                grade = null,
                blackholedAt = null,
                beginAt = null,
                endAt = null,
                cursus = null,
                skills = null
            )
        )
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = null,
            location = null,
            wallet = null,
            image = null,
            skills = null,
            projectsUsers = null,
            cursusUsers = cursusUsers,
            campus = null,
            campusUsers = null,
            correctionPoint = null
        )

        assertEquals(15.0, user.level, 0.0)
    }

    @Test
    fun `level returns 0 when no cursus users`() {
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = null,
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

        assertEquals(0.0, user.level, 0.0)
    }

    @Test
    fun `level returns highest level from ended cursus when no active`() {
        val cursusUsers = listOf(
            CursusUserModel(
                id = 1,
                level = 10.0,
                grade = null,
                blackholedAt = null,
                beginAt = null,
                endAt = "2024-01-01",
                cursus = null,
                skills = null
            ),
            CursusUserModel(
                id = 2,
                level = 15.0,
                grade = null,
                blackholedAt = null,
                beginAt = null,
                endAt = "2024-02-01",
                cursus = null,
                skills = null
            )
        )
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = null,
            location = null,
            wallet = null,
            image = null,
            skills = null,
            projectsUsers = null,
            cursusUsers = cursusUsers,
            campus = null,
            campusUsers = null,
            correctionPoint = null
        )

        assertEquals(15.0, user.level, 0.0)
    }

    @Test
    fun `locationName returns location when location is not null and not empty`() {
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = null,
            location = "Paris",
            wallet = null,
            image = null,
            skills = null,
            projectsUsers = null,
            cursusUsers = null,
            campus = null,
            campusUsers = null,
            correctionPoint = null
        )

        assertEquals("Paris", user.locationName)
    }

    @Test
    fun `locationName returns null when location is null`() {
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = null,
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

        assertNull(user.locationName)
    }

    @Test
    fun `evaluations returns correctionPoint when not null`() {
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = null,
            location = null,
            wallet = null,
            image = null,
            skills = null,
            projectsUsers = null,
            cursusUsers = null,
            campus = null,
            campusUsers = null,
            correctionPoint = 42
        )

        assertEquals(42, user.evaluations)
    }

    @Test
    fun `evaluations returns 0 when correctionPoint is null`() {
        val user = UserModel(
            id = 1,
            login = "testuser",
            email = null,
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

        assertEquals(0, user.evaluations)
    }
}

