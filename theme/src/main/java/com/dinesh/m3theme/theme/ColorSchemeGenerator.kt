package com.dinesh.m3theme.theme

import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dinesh.m3theme.model.PaletteStyle
import com.dinesh.m3theme.util.ThemeDefaults
import com.dinesh.m3theme.util.ThemeUtils.toHexString
import dynamiccolor.DynamicScheme
import hct.Hct
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import scheme.SchemeContent
import scheme.SchemeExpressive
import scheme.SchemeFidelity
import scheme.SchemeFruitSalad
import scheme.SchemeMonochrome
import scheme.SchemeNeutral
import scheme.SchemeRainbow
import scheme.SchemeTonalSpot
import scheme.SchemeVibrant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorSchemeGenerator @Inject constructor() {
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default    //  withContext(defaultDispatcher)

    @Stable
    fun generateSchemes(seedColor: Color, isDark: Boolean, style: PaletteStyle, contrastLevel: Double): Pair<DynamicScheme, ColorScheme> {
        val hct: Hct? = Hct.fromInt(seedColor.toArgb())
        val dynamicScheme: DynamicScheme = createDynamicScheme(style, hct, isDark, contrastLevel)
        val colorScheme: ColorScheme = createColorScheme(dynamicScheme)
        return dynamicScheme to colorScheme
    }

    @Stable
    suspend fun generateColorScheme(
        seedColor: Color,
        isDark: Boolean,
        paletteStyle: PaletteStyle,
        contrastLevel: Double
    ): ColorScheme = coroutineScope {
        // Consider adding try-catch here if Hct/DynamicScheme creation can fail
        try {
            val hct: Hct = Hct.fromInt(seedColor.toArgb())
            // Ensure contrast is within valid Material range if necessary (often -1.0 to 1.0, check lib docs)
            val dynamicScheme: DynamicScheme = createDynamicScheme(paletteStyle, hct, isDark, contrastLevel.coerceIn(0.0, 1.0))
            createColorScheme(dynamicScheme)
        } catch (e: Exception) {
            // Log error, return a fallback default scheme
            // Log.e("ColorSchemeGenerator", "Error generating color scheme", e)
            ThemeDefaults.COLOR_SCHEME // Return a safe default
        }
    }

//    @Stable
//    fun generateColorScheme(seedColor: Color, isDark: Boolean, paletteStyle: PaletteStyle, contrastLevel: Double): ColorScheme {
//        val hct: Hct? = Hct.fromInt(seedColor.toArgb())
//        val dynamicScheme: DynamicScheme = createDynamicScheme(paletteStyle, hct, isDark, contrastLevel.coerceIn(0.0, 1.0))
//        val colorScheme: ColorScheme = createColorScheme(dynamicScheme)
//        return colorScheme
//    }

    fun logSeedColor(dynamicScheme: DynamicScheme, TAG: String = "log_ColorSchemeGenerator") {
        Log.i(TAG, "seedColor: ${Color(dynamicScheme.sourceColorArgb).toHexString()} --> ${Color.Green.toHexString()}")
    }

    @Stable
    fun createDynamicScheme(style: PaletteStyle, hct: Hct?, isDark: Boolean, contrastLevel: Double): DynamicScheme = when (style) {
        PaletteStyle.TonalSpot -> SchemeTonalSpot(hct, isDark, contrastLevel)
        PaletteStyle.Neutral -> SchemeNeutral(hct, isDark, contrastLevel)
        PaletteStyle.Vibrant -> SchemeVibrant(hct, isDark, contrastLevel)
        PaletteStyle.Expressive -> SchemeExpressive(hct, isDark, contrastLevel)
        PaletteStyle.Rainbow -> SchemeRainbow(hct, isDark, contrastLevel)
        PaletteStyle.FruitSalad -> SchemeFruitSalad(hct, isDark, contrastLevel)
        PaletteStyle.Monochrome -> SchemeMonochrome(hct, isDark, contrastLevel)
        PaletteStyle.Fidelity -> SchemeFidelity(hct, isDark, contrastLevel)
        PaletteStyle.Content -> SchemeContent(hct, isDark, contrastLevel)
    }

    @Stable
    fun createColorScheme(dynamicScheme: DynamicScheme): ColorScheme = ColorScheme(
        background = Color(dynamicScheme.background),
        error = Color(dynamicScheme.error),
        errorContainer = Color(dynamicScheme.errorContainer),
        inverseOnSurface = Color(dynamicScheme.inverseOnSurface),
        inversePrimary = Color(dynamicScheme.inversePrimary),
        inverseSurface = Color(dynamicScheme.inverseSurface),
        onBackground = Color(dynamicScheme.onBackground),
        onError = Color(dynamicScheme.onError),
        onErrorContainer = Color(dynamicScheme.onErrorContainer),
        onPrimary = Color(dynamicScheme.onPrimary),
        onPrimaryContainer = Color(dynamicScheme.onPrimaryContainer),
        onSecondary = Color(dynamicScheme.onSecondary),
        onSecondaryContainer = Color(dynamicScheme.onSecondaryContainer),
        onSurface = Color(dynamicScheme.onSurface),
        onSurfaceVariant = Color(dynamicScheme.onSurfaceVariant),
        onTertiary = Color(dynamicScheme.onTertiary),
        onTertiaryContainer = Color(dynamicScheme.onTertiaryContainer),
        outline = Color(dynamicScheme.outline),
        outlineVariant = Color(dynamicScheme.outlineVariant),
        primary = Color(dynamicScheme.primary),
        primaryContainer = Color(dynamicScheme.primaryContainer),
        scrim = Color(dynamicScheme.scrim),
        secondary = Color(dynamicScheme.secondary),
        secondaryContainer = Color(dynamicScheme.secondaryContainer),
        surface = Color(dynamicScheme.surface),
        surfaceBright = Color(dynamicScheme.surfaceBright),
        surfaceContainer = Color(dynamicScheme.surfaceContainer),
        surfaceContainerLow = Color(dynamicScheme.surfaceContainerLow),
        surfaceContainerLowest = Color(dynamicScheme.surfaceContainerLowest),
        surfaceContainerHigh = Color(dynamicScheme.surfaceContainerHigh),
        surfaceContainerHighest = Color(dynamicScheme.surfaceContainerHighest),
        surfaceDim = Color(dynamicScheme.surfaceDim),
        surfaceTint = Color(dynamicScheme.surfaceTint),
        surfaceVariant = Color(dynamicScheme.surfaceVariant),
        tertiary = Color(dynamicScheme.tertiary),
        tertiaryContainer = Color(dynamicScheme.tertiaryContainer),
    )

//    fun generateColorScheme(isDarkTheme: Boolean,
////                            context: Context
//    ): ColorScheme {
//        return if (isDarkTheme) darkColorScheme() else lightColorScheme()
////        val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
////        return if (dynamicColorAvailable) {
////             if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
////        } else {
////            if (isDarkTheme) darkColorScheme() else lightColorScheme()
////        }
//    }
}