package com.dinesh.m3theme.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dinesh.m3theme.model.PaletteStyle
import com.dinesh.m3theme.model.ThemeMode
import com.dinesh.m3theme.model.ThemeState
import com.dinesh.m3theme.theme.AppTheme
import com.dinesh.m3theme.util.ThemeDefaults

@Composable
fun CustomizeThemeScreenContent(
    modifier: Modifier = Modifier,
    themeState: ThemeState,
    onSeedColorChange: (Color) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
    onContrastChange: (Double) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ColorSelector(
            seedColor = themeState.seedColor,
            onColorSelected = onSeedColorChange
        )

        ThemeModeSelector(
            selectedMode = themeState.themeMode,
            onModeSelected = onThemeModeChange
        )
        Spacer(modifier = Modifier.height(16.dp))

        PaletteStyleSelector(
            selectedStyle = themeState.paletteStyle,
            onStyleSelected = onPaletteStyleChange
        )
        Spacer(modifier = Modifier.height(16.dp))

        ContrastLevelSlider(
            contrastLevel = themeState.contrastLevel,
            onContrastLevelChanged = onContrastChange
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}


// --- Preview ---
@Preview(showBackground = true, name = "Customize Screen Light")
@Composable
fun CustomizeThemeScreenLightPreview() {
    val previewThemeState = ThemeState(
        seedColor = Color.Blue,
        themeMode = ThemeMode.System,
        paletteStyle = PaletteStyle.Vibrant,
        contrastLevel = 0.2
    )
    val previewColorScheme = ThemeDefaults.generateDefaultsColorScheme(false)

    AppTheme(colorScheme = previewColorScheme, isSystemInDarkTheme = false) {
        CustomizeThemeScreenContent(
            themeState = previewThemeState,
            onSeedColorChange = {},
            onThemeModeChange = {},
            onPaletteStyleChange = {},
            onContrastChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Customize Screen Dark")
@Composable
fun CustomizeThemeScreenDarkPreview() {
    val previewThemeState = ThemeState.DEFAULT
    val previewColorScheme = ThemeDefaults.generateDefaultsColorScheme(true)

    AppTheme(colorScheme = previewColorScheme, isSystemInDarkTheme = true) {
        CustomizeThemeScreenContent(
            themeState = previewThemeState,
            onSeedColorChange = {},
            onThemeModeChange = {},
            onPaletteStyleChange = {},
            onContrastChange = {}
        )
    }
}
