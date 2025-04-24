package com.dinesh.m3theme.domain

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dinesh.m3theme.model.PaletteStyle
import com.dinesh.m3theme.model.ThemeMode
import com.dinesh.m3theme.model.ThemeState
import com.dinesh.m3theme.util.ThemeDefaults
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

//class GetThemeStateUseCase @Inject constructor(
//    private val repository: ThemeRepositoryImpl
//) {
//    operator fun invoke() = repository.themeStateFlow
//}
//
//class UpdateSeedColorUseCase @Inject constructor(
//    private val repository: ThemeRepositoryImpl
//) {
//    suspend operator fun invoke(color: Color) {
//
//    }
//}
//
//class UpdateThemeModeUseCase @Inject constructor(
//    private val repository: ThemeRepositoryImpl
//) {
//    suspend operator fun invoke(mode: ThemeMode) {
//
//    }
//}
//
//class UpdatePaletteStyleUseCase @Inject constructor(
//    private val repository: ThemeRepositoryImpl
//) {
//    suspend operator fun invoke(style: PaletteStyle) {
//
//    }
//}
//
//class UpdateContrastLevelUseCase @Inject constructor(
//    private val repository: ThemeRepositoryImpl
//) {
//    suspend operator fun invoke(level: Double) {
//
//    }
//}
//
//class GenerateColorSchemeUseCase @Inject constructor() {
//    operator fun invoke(
//        seedColor: Color,
//        isDarkTheme: Boolean,
//        paletteStyle: PaletteStyle,
//        contrastLevel: Double
//    ): ColorScheme {
//        return ThemeDefaults.COLOR_SCHEME
//    }
//}
