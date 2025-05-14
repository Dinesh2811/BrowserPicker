package com.dinesh.browserpicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinesh.m3theme.presentation.ThemeViewModel
import com.dinesh.m3theme.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class BrowserPickerActivity: ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        splashScreen.setKeepOnScreenCondition {
            themeViewModel.themeUiState.value.isLoading
        }
        enableEdgeToEdge()

        setContent {
            val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()

            val systemInDarkTheme = isSystemInDarkTheme()
            LaunchedEffect(systemInDarkTheme) {
                themeViewModel.updateSystemDarkTheme(systemInDarkTheme)
            }

            if (!themeUiState.isLoading) {
                AppTheme(
                    colorScheme = themeUiState.colorScheme,
                    isSystemInDarkTheme = systemInDarkTheme,
                ) {
//                    browserpicker.presentation.test.main.MainScreen()
                    browserpicker.presentation.features.main.MainScreen()
//                    browserpicker.presentation.features.browserpicker.BrowserPickerScreen()
                    com.dinesh.browserpicker.mock.DebugScreen()
                }
            }
        }
    }
}
