package com.students42.app.utils

import com.google.gson.JsonSyntaxException
import com.students42.app.R
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorHandlerTest {
    @Test
    fun `getErrorStringRes returns error_unauthorized for 401 HttpException`() {
        val exception = HttpException(retrofit2.Response.error<Any>(401, okhttp3.ResponseBody.create(null, "")))
        val result = ErrorHandler.getErrorStringRes(exception)
        assertEquals(R.string.error_unauthorized, result)
    }

    @Test
    fun `getErrorStringRes returns error_not_found for 404 HttpException`() {
        val exception = HttpException(retrofit2.Response.error<Any>(404, okhttp3.ResponseBody.create(null, "")))
        val result = ErrorHandler.getErrorStringRes(exception)
        assertEquals(R.string.error_not_found, result)
    }

    @Test
    fun `getErrorStringRes returns error_server for 500 HttpException`() {
        val exception = HttpException(retrofit2.Response.error<Any>(500, okhttp3.ResponseBody.create(null, "")))
        val result = ErrorHandler.getErrorStringRes(exception)
        assertEquals(R.string.error_server, result)
    }

    @Test
    fun `getErrorStringRes returns error_unknown for other HttpException codes`() {
        val exception = HttpException(retrofit2.Response.error<Any>(403, okhttp3.ResponseBody.create(null, "")))
        val result = ErrorHandler.getErrorStringRes(exception)
        assertEquals(R.string.error_unknown, result)
    }

    @Test
    fun `getErrorStringRes returns error_network for IOException`() {
        val exception = IOException("Network error")
        val result = ErrorHandler.getErrorStringRes(exception)
        assertEquals(R.string.error_network, result)
    }

    @Test
    fun `getErrorStringRes returns error_network for SocketTimeoutException`() {
        val exception = SocketTimeoutException("Timeout")
        val result = ErrorHandler.getErrorStringRes(exception)
        assertEquals(R.string.error_network, result)
    }

    @Test
    fun `getErrorStringRes returns error_network for UnknownHostException`() {
        val exception = UnknownHostException("Unknown host")
        val result = ErrorHandler.getErrorStringRes(exception)
        assertEquals(R.string.error_network, result)
    }

    @Test
    fun `getErrorStringRes returns error_unknown for JsonSyntaxException`() {
        val exception = JsonSyntaxException("Invalid JSON")
        val result = ErrorHandler.getErrorStringRes(exception)
        assertEquals(R.string.error_unknown, result)
    }

    @Test
    fun `getErrorStringRes returns error_unknown for unknown exception`() {
        val exception = RuntimeException("Unknown error")
        val result = ErrorHandler.getErrorStringRes(exception)
        assertEquals(R.string.error_unknown, result)
    }
}
