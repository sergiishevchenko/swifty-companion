package com.students42.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {
    @Test
    fun `Success contains data`() {
        val result = Result.Success("test data")
        assertTrue(result is Result.Success)
        assertEquals("test data", (result as Result.Success).data)
    }

    @Test
    fun `Error contains exception`() {
        val exception = RuntimeException("Test error")
        val result = Result.Error(exception)
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `Loading is singleton object`() {
        val result1 = Result.Loading
        val result2 = Result.Loading
        assertTrue(result1 is Result.Loading)
        assertTrue(result2 is Result.Loading)
        assertEquals(result1, result2)
    }
}
