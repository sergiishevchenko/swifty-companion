package com.students42.app.ui.login

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.students42.app.R
import com.students42.app.auth.AuthService
import com.students42.app.data.local.TokenRepository
import com.students42.app.data.repositories.UserRepository
import com.students42.app.utils.ErrorHandler
import com.students42.app.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val authService: AuthService
) : AndroidViewModel(context as Application) {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private var lastSearchLogin: String? = null

    init {
        checkToken()
    }

    fun checkToken() {
        viewModelScope.launch {
            val token = tokenRepository.getToken().first()
            if (token == null) {
                _loginState.value = LoginState.NoToken
            }
        }
    }

    fun searchUser(login: String) {
        if (login.isBlank()) {
            _loginState.value = LoginState.Error(
                context.getString(R.string.error_unknown),
                null
            )
            return
        }

        lastSearchLogin = login.trim()
        performSearch(lastSearchLogin!!)
    }

    private fun performSearch(login: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            userRepository.getUserInfo(login).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _loginState.value = LoginState.Loading
                    }
                    is Result.Success -> {
                        _loginState.value = LoginState.Success(result.data)
                    }
                    is Result.Error -> {
                        val errorMessage = ErrorHandler.handleError(context, result.exception)
                        _loginState.value = LoginState.Error(
                            errorMessage,
                            retryAction = { performSearch(login) }
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        lastSearchLogin?.let { login ->
            performSearch(login)
        }
    }

    fun startOAuthFlow(): Intent {
        return authService.startOAuthFlow()
    }

    fun handleOAuthCallback(code: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            when (val result = authService.getToken(code)) {
                is Result.Success -> {
                    authService.saveTokenResponse(result.data)
                    _loginState.value = LoginState.Idle
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.handleError(context, result.exception)
                    _loginState.value = LoginState.Error(
                        errorMessage,
                        retryAction = { handleOAuthCallback(code) }
                    )
                }
                else -> {}
            }
        }
    }

    fun clearError() {
        if (_loginState.value is LoginState.Error) {
            _loginState.value = LoginState.Idle
        }
    }
}
