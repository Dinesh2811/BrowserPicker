package com.dinesh.m3theme.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.dinesh.m3theme.presentation.ui.screen.CustomizeThemeScreen
import com.dinesh.m3theme.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
                    CustomizeThemeScreen(themeViewModel = themeViewModel)
                }
            }
        }
    }
}
