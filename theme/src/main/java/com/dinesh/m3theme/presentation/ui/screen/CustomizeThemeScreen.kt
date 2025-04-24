package com.dinesh.m3theme.presentation.ui.screen

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinesh.m3theme.model.ThemeState
import com.dinesh.m3theme.presentation.ThemeViewModel
import com.dinesh.m3theme.presentation.ui.components.CustomizeThemeScreenContent


@Composable
fun CustomizeThemeScreen(
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsState()
//    val isSystemInDarkTheme = isSystemInDarkTheme()
//    LaunchedEffect(isSystemInDarkTheme) {
//        themeViewModel.updateSystemDarkMode(isSystemInDarkTheme)
//    }

    CustomizeThemeScreenContent(
        modifier = modifier,
        themeState = themeUiState.themeState,
        onSeedColorChange = themeViewModel::updateSeedColor,
        onThemeModeChange = themeViewModel::updateThemeMode,
        onPaletteStyleChange = themeViewModel::updatePaletteStyle,
        onContrastChange = themeViewModel::updateContrastLevel
    )
}


/*

@Composable
fun CustomizeThemeScreen(modifier: Modifier = Modifier, themeViewModel: ThemeViewModel = hiltViewModel()) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val isSystemInDarkTheme: Boolean = isSystemInDarkTheme()

    LaunchedEffect(isSystemInDarkTheme) {
        themeViewModel.updateSystemDarkMode(isSystemInDarkTheme)
    }
    if (themeUiState.themeState == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        CustomizeThemeScreenContent(
            modifier = modifier,
            themeState = themeUiState.themeState?: ThemeState.DEFAULT,
            onSeedColorChange = { seedColor ->
                themeViewModel.updateThemeState { it.copy(seedColor = seedColor) }
            },
            onThemeModeChange = { themeMode ->
                themeViewModel.updateThemeState { it.copy(themeMode = themeMode) }
            },
            onPaletteStyleChange = { paletteStyle ->
                themeViewModel.updateThemeState { it.copy(paletteStyle = paletteStyle) }
            },
            onContrastChange = { contrastLevel ->
                themeViewModel.updateThemeState { it.copy(contrastLevel = contrastLevel) }
            }
        )
    }
}

*/
