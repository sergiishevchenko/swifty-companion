package com.students42.app.utils

import android.content.Context
import com.google.gson.JsonSyntaxException
import com.students42.app.R
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorHandler {
    fun getErrorStringRes(throwable: Throwable): Int {
        return when (throwable) {
            is HttpException -> {
                when (throwable.code()) {
                    401 -> R.string.error_unauthorized
                    404 -> R.string.error_not_found
                    500 -> R.string.error_server
                    else -> R.string.error_unknown
                }
            }
            is IOException, is SocketTimeoutException -> {
                R.string.error_network
            }
            is UnknownHostException -> {
                R.string.error_network
            }
            is JsonSyntaxException -> {
                R.string.error_unknown
            }
            else -> {
                R.string.error_unknown
            }
        }
    }

    fun handleError(context: Context, throwable: Throwable): String {
        val stringRes = getErrorStringRes(throwable)
        return context.getString(stringRes)
    }
}
