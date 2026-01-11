package com.students42.app.data.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class SimpleLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method
        
        val authHeader = request.header("Authorization")
        val token = authHeader?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }
        
        Log.d("OkHttp", "--> $method $url")
        token?.let {
            Log.d("OkHttp", "Authorization: Bearer $it")
        }
        Log.d("OkHttp", "--> END $method")
        
        val startTime = System.currentTimeMillis()
        try {
            val response = chain.proceed(request)
            val duration = System.currentTimeMillis() - startTime
            val code = response.code
            val message = response.message
            
            if (code >= 400) {
                val errorMessage = message.ifBlank { 
                    when (code) {
                        400 -> "Bad Request"
                        401 -> "Unauthorized"
                        403 -> "Forbidden"
                        404 -> "Not Found"
                        429 -> "Too Many Requests"
                        500 -> "Internal Server Error"
                        502 -> "Bad Gateway"
                        503 -> "Service Unavailable"
                        else -> "Unknown error"
                    }
                }
                Log.e("OkHttp", "<-- $code $errorMessage $url (${duration}ms) [ERROR]")
            } else {
                val statusMessage = message.ifBlank { "OK" }
                Log.d("OkHttp", "<-- $code $statusMessage $url (${duration}ms)")
            }
            
            return response
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e("OkHttp", "<-- FAILED $url (${duration}ms) [NETWORK ERROR: ${e.message}]")
            throw e
        }
    }
}
