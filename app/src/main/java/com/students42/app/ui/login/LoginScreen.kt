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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.students42.app.R

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel(),
    initialUri: Uri? = null
) {
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current
    var loginText by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var lastNavigatedLogin by remember { mutableStateOf<String?>(null) }
    var lastShownError by remember { mutableStateOf<String?>(null) }
    var processedInitialUri by remember { mutableStateOf<Uri?>(null) }

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

    LaunchedEffect(initialUri, loginState) {
        initialUri?.let { uri ->
            if (uri.scheme == "students42" && uri.host == "oauth" && uri.path == "/callback") {
                if (processedInitialUri != uri && loginState is LoginState.NoToken) {
                    processedInitialUri = uri
                    val code = uri.getQueryParameter("code")
                    code?.let { authCode ->
                        viewModel.handleOAuthCallback(authCode)
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                val route = navController.currentDestination?.route
                if (route == "login") {
                    if (lastNavigatedLogin != state.user.login) {
                        lastNavigatedLogin = state.user.login
                        navController.navigate("profile/${state.user.login}") {
                            popUpTo("login") { inclusive = false }
                        }
                    }
                }
            }
            is LoginState.Error -> {
                if (lastShownError != state.message) {
                    lastShownError = state.message
                    val result = snackbarHostState.showSnackbar(
                        message = state.message,
                        actionLabel = if (state.retryAction != null) context.getString(R.string.retry) else null
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        state.retryAction?.invoke()
                    }
                }
            }
            else -> {
                lastNavigatedLogin = null
                if (loginState !is LoginState.Error) {
                    lastShownError = null
                }
            }
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
            val scrollState = rememberScrollState()
            val logoSize = if (isLandscape) 120.dp else 200.dp
            val spacing = if (isLandscape) 16.dp else 24.dp
            Column(
                modifier = Modifier
                    .then(if (isLandscape) Modifier.fillMaxWidth(0.6f) else Modifier.fillMaxWidth())
                    .verticalScroll(scrollState)
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .padding(bottom = if (isLandscape) 8.dp else 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(logoSize)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                            .padding(4.dp)
                    )
                }
                Text(
                    text = "Enter login to view profile",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = loginText,
                    onValueChange = { loginText = it },
                    label = { Text(stringResource(R.string.login_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = loginState !is LoginState.Loading,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )

                when (loginState) {
                    is LoginState.Loading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is LoginState.NoToken -> {
                        Button(
                            onClick = {
                                val intent = viewModel.startOAuthFlow()
                                oauthLauncher.launch(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.authorize_button),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = { viewModel.searchUser(loginText) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = loginText.isNotBlank(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.search_button),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
