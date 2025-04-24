package com.dinesh.m3theme.util

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.dinesh.m3theme.model.PaletteStyle
import com.dinesh.m3theme.model.ThemeMode

object ThemeDefaults {
    val SEED_COLOR: Color = Color(0xFF6750A4)
    val THEME_MODE: ThemeMode = ThemeMode.System
    val PALETTE_STYLE: PaletteStyle = PaletteStyle.TonalSpot
    const val CONTRAST_LEVEL: Double = 0.0
    fun lightColorScheme(): ColorScheme = COLOR_SCHEME
    fun darkColorScheme(): ColorScheme = COLOR_SCHEME

    val PRE_DEFINED_COLORS: List<Color> = listOf(
        SEED_COLOR, // Purple --> Color(0xFF6750A4)
        Color(0xFF3F51B5), // Indigo
        Color(0xFFF44336), // Red
        Color(0xFF2196F3), // Blue
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF4081), // Pink
        Color(0xFF795548), // Brown
        Color(0xFF607D8B), // Blue Grey
        Color(0xFFE91E63), // Deep Pink
        Color(0xFF3F51B5), // Indigo
        Color(0xFFCDDC39), // Lime
        Color(0xFF009688), // Teal
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF8BC34A), // Light Green
        Color(0xFFFFC107), // Amber
        Color(0xFF03A9F4), // Light Blue
        Color(0xFF9E9E9E), // Grey
        Color(0xFF795548), // Brown
        Color(0xFF00796B), // Dark Teal
        Color(0xFFC2185B), // Dark Pink
        Color(0xFF1976D2)  // Dark Blue
    )

    val WALLPAPER_COLORS: List<Color> = listOf(SEED_COLOR)

    val COLOR_SCHEME: ColorScheme = ColorScheme(
        primary = Color(0xFF65558F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE9DDFF),
        onPrimaryContainer = Color(0xFF201047),
        inversePrimary = Color(0xFFCFBDFE),
        secondary = Color(0xFF625B71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1E192B),
        tertiary = Color(0xFF7E5260),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9E3),
        onTertiaryContainer = Color(0xFF31101D),
        background = Color(0xFFFDF7FF),
        onBackground = Color(0xFF1D1B20),
        surface = Color(0xFFFDF7FF),
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFE7E0EB),
        onSurfaceVariant = Color(0xFF49454E),
        surfaceTint = Color(0xFF65558F),
        inverseSurface = Color(0xFF322F35),
        inverseOnSurface = Color(0xFFF5EFF7),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF7A757F),
        outlineVariant = Color(0xFFCAC4CF),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFDF7FF),
        surfaceDim = Color(0xFFDED8E0),
        surfaceContainer = Color(0xFFF2ECF4),
        surfaceContainerHigh = Color(0xFFECE6EE),
        surfaceContainerHighest = Color(0xFFE6E0E9),
        surfaceContainerLow = Color(0xFFF8F2FA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
    )

    fun generateDefaultsColorScheme(isDarkTheme: Boolean, context: Context? = null): ColorScheme {
        return if (isDarkTheme) darkColorScheme() else lightColorScheme()
        val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        return if (dynamicColorAvailable && context != null) {
             if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (isDarkTheme) darkColorScheme() else lightColorScheme()
        }
    }
}