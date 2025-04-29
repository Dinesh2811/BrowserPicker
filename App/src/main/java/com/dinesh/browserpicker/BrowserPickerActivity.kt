package com.dinesh.browserpicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinesh.m3theme.presentation.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

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
                browserpicker.presentation.main.MainScreen()
                com.dinesh.browserpicker.mock.DebugScreen()
//                CustomizeThemeScreen()
            }
        }
    }
}
