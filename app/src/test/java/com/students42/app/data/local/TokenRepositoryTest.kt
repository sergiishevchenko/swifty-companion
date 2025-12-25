package com.students42.app.data.local

import android.content.Context
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class TokenRepositoryTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = mock()
    }

    @Test
    fun `TokenRepository can be instantiated`() {
        val repository = TokenRepository(context)
        assertNotNull(repository)
    }
}
