package com.students42.app.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.students42.app.R

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current
    var loginText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val oauthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                val code = it.getQueryParameter("code")
                code?.let { authCode ->
                    viewModel.handleOAuthCallback(authCode)
                }
            }
        }
    }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                navController.navigate("profile/${state.user.login}") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is LoginState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    actionLabel = if (state.retryAction != null) stringResource(R.string.retry) else null
                ) {
                    state.retryAction?.invoke()
                }
            }
            else -> {}
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            val maxWidth = this.maxWidth
            val padding = if (maxWidth < 600.dp) 32.dp else 48.dp
            Column(
                modifier = Modifier
                    .then(if (isLandscape) Modifier.fillMaxWidth(0.6f) else Modifier.fillMaxWidth())
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = loginText,
                    onValueChange = { loginText = it },
                    label = { Text(stringResource(R.string.login_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = loginState !is LoginState.Loading && loginState !is LoginState.NoToken
                )

                when (loginState) {
                    is LoginState.Loading -> {
                        CircularProgressIndicator()
                    }
                    is LoginState.NoToken -> {
                        Button(
                            onClick = {
                                val intent = viewModel.startOAuthFlow()
                                oauthLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.authorize_button))
                        }
                    }
                    else -> {
                        Button(
                            onClick = { viewModel.searchUser(loginText) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = loginText.isNotBlank()
                        ) {
                            Text(stringResource(R.string.search_button))
                        }
                    }
                }
            }
        }
    }
}
