package com.dinesh.m3theme.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AppTheme(
    colorScheme: ColorScheme,
    isSystemInDarkTheme: Boolean = isSystemInDarkTheme(),
    // dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                // Set status bar color and icons
//                it.statusBarColor = colorScheme.primary.toArgb() // Or use surface, primaryContainer etc.
//                it.navigationBarColor = colorScheme.background.toArgb() // Or use surface, surfaceVariant etc.

                // Make system bars transparent to allow content to draw behind
                it.statusBarColor = Color.Transparent.toArgb()
                it.navigationBarColor = Color.Transparent.toArgb()

                // Tell the system whether the content behind the bars is light or dark
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !isSystemInDarkTheme
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = !isSystemInDarkTheme

                // Ensure content draws behind system bars
                WindowCompat.setDecorFitsSystemWindows(it, false)
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background,
            contentColor = colorScheme.onBackground
        ) {
            content()
        }
    }
}
