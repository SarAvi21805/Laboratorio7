package com.example.ejercicioenclase2708

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onPhotoClick = { photoId ->
                    navController.navigate("details/$photoId")
                },
                onProfileClick = {
                    navController.navigate("profile")
                }
            )
        }
        composable("details/{photoId}") { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId")
            requireNotNull(photoId) { "photoId no puede ser null" }
            DetailsScreen(photoId = photoId, onNavigateBack = { navController.popBackStack() })
        }
        composable("profile") {
            ProfileScreen(
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}