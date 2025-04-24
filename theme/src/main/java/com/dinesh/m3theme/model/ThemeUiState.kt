package com.dinesh.m3theme.model

import androidx.annotation.FloatRange
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.dinesh.m3theme.util.ThemeDefaults
import dynamiccolor.DynamicScheme

data class ThemeUiState(
    val themeState: ThemeState = ThemeState.DEFAULT,
    val colorLists: ColorLists = ColorLists.Default,
    val isWallpaperSegmentSelected: Boolean = false,
    val isSystemInDarkTheme: Boolean? = null,
    val dynamicScheme: DynamicScheme? = null,
    val colorScheme: ColorScheme = ThemeDefaults.COLOR_SCHEME,
//    val dialogState: DialogState = DialogState.IsColorPickerDialogShown(false),
    val isLoading: Boolean = true,
    val isDarkEffective: Boolean = false,
)

data class ThemeState(
    val seedColor: Color = ThemeDefaults.SEED_COLOR,
    val themeMode: ThemeMode = ThemeDefaults.THEME_MODE,
    val paletteStyle: PaletteStyle = ThemeDefaults.PALETTE_STYLE,
    @FloatRange(from = 0.0, to = 1.0) val contrastLevel: Double = ThemeDefaults.CONTRAST_LEVEL,
) {
    companion object {
        val DEFAULT: ThemeState = ThemeState()
    }
}

/*

data class ThemeState(
    val seedColor: Color? = null,
    val themeMode: ThemeMode? = null,
    val paletteStyle: PaletteStyle? = null,
    @FloatRange(from = 0.0, to = 1.0) val contrastLevel: Double? = null,
) {
    internal fun updateWith(themeState: ThemeState?): ThemeState = ThemeState(
        seedColor = themeState?.seedColor?: this.seedColor?: ThemeDefaults.SEED_COLOR,
        themeMode = themeState?.themeMode?: this.themeMode?: ThemeDefaults.THEME_MODE,
        paletteStyle = themeState?.paletteStyle?: this.paletteStyle?: ThemeDefaults.PALETTE_STYLE,
        contrastLevel = themeState?.contrastLevel?: this.contrastLevel?: ThemeDefaults.CONTRAST_LEVEL
    )

    companion object {
        val DEFAULT: ThemeState = ThemeState(
            seedColor = ThemeDefaults.SEED_COLOR,
            themeMode = ThemeDefaults.THEME_MODE,
            paletteStyle = ThemeDefaults.PALETTE_STYLE,
            contrastLevel = ThemeDefaults.CONTRAST_LEVEL,
        )
    }
}
*/

data class ColorLists(
    val predefinedColors: List<Color>? = ThemeDefaults.PRE_DEFINED_COLORS,
    val wallpaperColors: List<Color>? = listOf(ThemeDefaults.SEED_COLOR)
) {
    internal fun updateWith(colorLists: ColorLists?): ColorLists = ColorLists(
        predefinedColors = colorLists?.predefinedColors?: predefinedColors?: ThemeDefaults.PRE_DEFINED_COLORS,
        wallpaperColors = colorLists?.wallpaperColors?: wallpaperColors?: listOf(ThemeDefaults.SEED_COLOR)
    )
    companion object {
        val Default: ColorLists = ColorLists(
            predefinedColors = ThemeDefaults.PRE_DEFINED_COLORS,
            wallpaperColors = listOf(ThemeDefaults.SEED_COLOR)
        )
    }
}

enum class ThemeMode {
    Light, Dark, System
}

enum class PaletteStyle {
    TonalSpot, Neutral, Vibrant, Rainbow, Expressive, FruitSalad, Monochrome, Fidelity, Content,
}
