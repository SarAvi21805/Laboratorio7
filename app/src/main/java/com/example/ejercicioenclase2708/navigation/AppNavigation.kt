package com.example.ejercicioenclase2708.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ejercicioenclase2708.ui.screens.DetailsScreen
import com.example.ejercicioenclase2708.ui.screens.HomeScreen
import com.example.ejercicioenclase2708.ui.screens.ProfileScreen

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable(
            route = "details/{photoId}",
            arguments = listOf(navArgument("photoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
            DetailsScreen(navController = navController, photoId = photoId)
        }
        composable("profile") {
            ProfileScreen(
                navController = navController,
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange
            )
        }
    }
}