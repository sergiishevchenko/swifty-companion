package com.students42.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.students42.app.R
import com.students42.app.ui.components.ProjectsList
import com.students42.app.ui.components.SkillsList
import com.students42.app.ui.components.UserInfoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    login: String,
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileState by viewModel.profileState.collectAsState()
    val context = LocalContext.current
    var showErrorDialog by remember { mutableStateOf(false) }
    var lastLoadedLogin by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(login) {
        if (lastLoadedLogin != login) {
            lastLoadedLogin = login
            viewModel.loadUserProfileByLogin(login)
        }
    }

    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Error) {
            showErrorDialog = true
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val isTablet = configuration.screenWidthDp >= 600

    BackHandler(enabled = true) {
        navController.navigate("login") {
            popUpTo("login") { inclusive = true }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(paddingValues)
        ) {
            when (val state = profileState) {
                is ProfileState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProfileState.Success -> {
                    if (isLandscape && isTablet) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp)
                            ) {
                                state.user.imageUrl?.let { imageUrl ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.4f)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    CircleShape
                                                )
                                                .padding(4.dp)
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(imageUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Profile picture",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                                UserInfoCard(user = state.user)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp)
                            ) {
                                if (state.skills.isNotEmpty()) {
                                    SkillsList(skills = state.skills)
                                }
                                if (state.projects.isNotEmpty()) {
                                    ProjectsList(projects = state.projects)
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            state.user.imageUrl?.let { imageUrl ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(if (isTablet) 0.25f else 0.3f)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape
                                            )
                                            .padding(4.dp)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Profile picture",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            UserInfoCard(user = state.user)

                            if (state.skills.isNotEmpty()) {
                                SkillsList(skills = state.skills)
                            }

                            if (state.projects.isNotEmpty()) {
                                ProjectsList(projects = state.projects)
                            }
                        }
                    }
                }
                is ProfileState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (state.retryAction != null) {
                            Button(
                                onClick = {
                                    showErrorDialog = false
                                    viewModel.retry()
                                }
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showErrorDialog && profileState is ProfileState.Error) {
        val errorState = profileState as ProfileState.Error
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Text(stringResource(R.string.error_unknown))
            },
            text = {
                Text(errorState.message)
            },
            confirmButton = {
                if (errorState.retryAction != null) {
                    Button(
                        onClick = {
                            showErrorDialog = false
                            viewModel.retry()
                        }
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showErrorDialog = false }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}
