package com.students42.app.utils

object Constants {
    const val API_BASE_URL = "https://api.intra.42.fr"
    const val OAUTH_AUTHORIZE_URL = "$API_BASE_URL/oauth/authorize"
    const val OAUTH_TOKEN_URL = "$API_BASE_URL/oauth/token"
    const val DATASTORE_NAME = "students42_preferences"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val ARG_USER_LOGIN = "user_login"
    const val ARG_USER_ID = "user_id"
}
