package com.students42.app.utils

import android.content.Context
import com.students42.app.R
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.google.gson.JsonSyntaxException

object ErrorHandler {
    fun handleError(context: Context, throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> {
                when (throwable.code()) {
                    401 -> context.getString(R.string.error_unauthorized)
                    404 -> context.getString(R.string.error_not_found)
                    500 -> context.getString(R.string.error_server)
                    else -> context.getString(R.string.error_unknown)
                }
            }
            is IOException, is SocketTimeoutException -> {
                context.getString(R.string.error_network)
            }
            is UnknownHostException -> {
                context.getString(R.string.error_network)
            }
            is JsonSyntaxException -> {
                context.getString(R.string.error_unknown)
            }
            else -> {
                context.getString(R.string.error_unknown)
            }
        }
    }
}
