package com.dinesh.m3theme.model

import androidx.annotation.FloatRange

data class ThemePreferencesData(
    val seedColorHex: String?,
    val themeModeName: String?,
    val paletteStyleName: String?,
    @FloatRange(from = 0.0, to = 1.0) val contrastLevelValue: Double? = null,
)