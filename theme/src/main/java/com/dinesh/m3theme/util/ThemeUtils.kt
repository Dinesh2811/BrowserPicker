package com.dinesh.m3theme.util

import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dinesh.m3theme.model.PaletteStyle
import com.dinesh.m3theme.model.ThemeMode

object ThemeUtils {
    fun String?.toThemeMode(): ThemeMode = try {
        this?.let { ThemeMode.valueOf(it) }?: ThemeDefaults.THEME_MODE
    } catch (e: IllegalArgumentException) {
        ThemeDefaults.THEME_MODE
    } catch (e: Exception) {
        ThemeDefaults.THEME_MODE
    }

    fun String?.toPaletteStyle(): PaletteStyle = try {
        this?.let { PaletteStyle.valueOf(it) }?: ThemeDefaults.PALETTE_STYLE
    } catch (e: IllegalArgumentException) {
        ThemeDefaults.PALETTE_STYLE
    } catch (e: Exception) {
        ThemeDefaults.PALETTE_STYLE
    }

    fun Color.toHexString(includeAlpha: Boolean = true): String {
        val argb: Int = this.toArgb()

        return if (includeAlpha) {
            String.format("#%08X", argb)
        } else {
            //  String.format("#%02X%02X%02X", (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
            String.format("#%06X", argb and 0xFFFFFF)
        }
    }

    fun String?.toColorOrNull(): Color? {
        // 1. Handle null or blank input
        if (this.isNullOrBlank()) return null

        // 2. Clean the input string (remove '#', trim, uppercase)
        val hexString = this.trim().removePrefix("#").uppercase()

        // 3. Validate length (must be 6 for RGB or 8 for ARGB)
        if (hexString.length != 6 && hexString.length != 8) return null

        // 4. Validate characters (must be 0-9 or A-F)
        if (!hexString.all { it in '0'..'9' || it in 'A'..'F' }) return null

        return try {
            // Prepend "FF" for full opacity if only RGB is provided (AARRGGBB format)
            val finalHexString = if (hexString.length == 6) "FF$hexString" else hexString
            Color(color = finalHexString.toLong(16).toInt())
        } catch (e: NumberFormatException) {
            null
        } catch (e: Exception) {
            null
        }
    }


    fun logColorInfo(selectedColor: Color, scheme: ColorScheme) {
        Log.d("log_ColorInfo", """
        Selected Color: ${selectedColor.toHexString()}
        Primary Color: ${scheme.primary.toHexString()}
        OnPrimary Color: ${scheme.onPrimary.toHexString()}
        PrimaryContainer Color: ${scheme.primaryContainer.toHexString()}
        OnPrimaryContainer Color: ${scheme.onPrimaryContainer.toHexString()}
        InversePrimary Color: ${scheme.inversePrimary.toHexString()}
        Secondary Color: ${scheme.secondary.toHexString()}
        OnSecondary Color: ${scheme.onSecondary.toHexString()}
        SecondaryContainer Color: ${scheme.secondaryContainer.toHexString()}
        OnSecondaryContainer Color: ${scheme.onSecondaryContainer.toHexString()}
        Tertiary Color: ${scheme.tertiary.toHexString()}
        OnTertiary Color: ${scheme.onTertiary.toHexString()}
        TertiaryContainer Color: ${scheme.tertiaryContainer.toHexString()}
        OnTertiaryContainer Color: ${scheme.onTertiaryContainer.toHexString()}
        Background Color: ${scheme.background.toHexString()}
        OnBackground Color: ${scheme.onBackground.toHexString()}
        Surface Color: ${scheme.surface.toHexString()}
        OnSurface Color: ${scheme.onSurface.toHexString()}
        SurfaceVariant Color: ${scheme.surfaceVariant.toHexString()}
        OnSurfaceVariant Color: ${scheme.onSurfaceVariant.toHexString()}
        SurfaceTint Color: ${scheme.surfaceTint.toHexString()}
        InverseSurface Color: ${scheme.inverseSurface.toHexString()}
        InverseOnSurface Color: ${scheme.inverseOnSurface.toHexString()}
        Error Color: ${scheme.error.toHexString()}
        OnError Color: ${scheme.onError.toHexString()}
        ErrorContainer Color: ${scheme.errorContainer.toHexString()}
        OnErrorContainer Color: ${scheme.onErrorContainer.toHexString()}
        Outline Color: ${scheme.outline.toHexString()}
        OutlineVariant Color: ${scheme.outlineVariant.toHexString()}
        Scrim Color: ${scheme.scrim.toHexString()}
        SurfaceBright Color: ${scheme.surfaceBright.toHexString()}
        SurfaceDim Color: ${scheme.surfaceDim.toHexString()}
        SurfaceContainer Color: ${scheme.surfaceContainer.toHexString()}
        SurfaceContainerHigh Color: ${scheme.surfaceContainerHigh.toHexString()}
        SurfaceContainerHighest Color: ${scheme.surfaceContainerHighest.toHexString()}
        SurfaceContainerLow Color: ${scheme.surfaceContainerLow.toHexString()}
        SurfaceContainerLowest Color: ${scheme.surfaceContainerLowest.toHexString()}
    """.trimIndent())
    }
}