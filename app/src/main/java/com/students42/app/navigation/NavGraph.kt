package com.students42.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.students42.app.ui.login.LoginScreen
import com.students42.app.ui.profile.ProfileScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    initialUri: Uri? = null
) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController = navController, initialUri = initialUri)
        }
        composable("profile/{login}") { backStackEntry ->
            val login = backStackEntry.arguments?.getString("login") ?: ""
            ProfileScreen(login = login)
        }
    }
}
