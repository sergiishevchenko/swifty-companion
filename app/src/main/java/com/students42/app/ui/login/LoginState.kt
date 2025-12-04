package com.students42.app.ui.login

import com.students42.app.data.models.UserModel

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: UserModel) : LoginState()
    data class Error(val message: String, val retryAction: (() -> Unit)? = null) : LoginState()
    object NoToken : LoginState()
}
