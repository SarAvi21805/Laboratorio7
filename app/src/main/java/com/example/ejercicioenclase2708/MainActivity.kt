package com.example.ejercicioenclase2708

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.ejercicioenclase2708.ui.theme.Ejercicioenclase2708Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Estado para manejar el tema (claro/oscuro)
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }

            Ejercicioenclase2708Theme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { isDark -> isDarkTheme = isDark }
                )
            }
        }
    }
}