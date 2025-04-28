package com.dinesh.browserpicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinesh.browserpicker.ui.theme.BrowserPickerTheme
import com.dinesh.m3theme.presentation.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinesh.m3theme.presentation.ui.screen.CustomizeThemeScreen

@AndroidEntryPoint
class BrowserPickerActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()

            val systemInDarkTheme = isSystemInDarkTheme()
            LaunchedEffect(systemInDarkTheme) {
                themeViewModel.updateSystemDarkTheme(systemInDarkTheme)
            }

            MaterialTheme(colorScheme = themeUiState.colorScheme) {
//                browserpicker.presentation.main.MainScreen()
                CustomizeThemeScreen()
            }
        }
    }
}
