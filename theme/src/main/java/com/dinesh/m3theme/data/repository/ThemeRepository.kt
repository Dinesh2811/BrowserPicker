package com.dinesh.m3theme.data.repository

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dinesh.m3theme.data.datasource.local.ThemeLocalDataSource
import com.dinesh.m3theme.model.PaletteStyle
import com.dinesh.m3theme.model.ThemeMode
import com.dinesh.m3theme.model.ThemePreferencesData
import com.dinesh.m3theme.model.ThemeState
import com.dinesh.m3theme.util.ThemeDefaults
import com.dinesh.m3theme.util.ThemeUtils.toColorOrNull
import com.dinesh.m3theme.util.ThemeUtils.toHexString
import com.dinesh.m3theme.util.ThemeUtils.toPaletteStyle
import com.dinesh.m3theme.util.ThemeUtils.toThemeMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface ThemeRepository {
    fun getThemeState(): Flow<ThemeState>
    suspend fun saveSeedColor(color: Color)
    suspend fun saveThemeMode(themeMode: ThemeMode)
    suspend fun savePaletteStyle(paletteStyle: PaletteStyle)
    suspend fun saveContrastLevel(contrastLevel: Double)
}

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val localDataSource: ThemeLocalDataSource
): ThemeRepository {
    companion object {
        private const val TAG = "log_ThemeRepositoryImpl"
    }

    override fun getThemeState(): Flow<ThemeState> {
        return localDataSource.getThemePreferences()
            .map { prefsData ->
                ThemeState(
                    seedColor = prefsData.seedColorHex.toColorOrNull()?: ThemeDefaults.SEED_COLOR,
                    themeMode = prefsData.themeModeName.toThemeMode(),
                    paletteStyle = prefsData.paletteStyleName.toPaletteStyle(),
                    contrastLevel = prefsData.contrastLevelValue?: ThemeDefaults.CONTRAST_LEVEL
                )
            }
    }

    override suspend fun saveSeedColor(color: Color) {
        localDataSource.saveSeedColor(color.toHexString())
    }

    override suspend fun saveThemeMode(themeMode: ThemeMode) {
        localDataSource.saveThemeMode(themeMode.name)
    }

    override suspend fun savePaletteStyle(paletteStyle: PaletteStyle) {
        localDataSource.savePaletteStyle(paletteStyle.name)
    }

    override suspend fun saveContrastLevel(contrastLevel: Double) {
        val clampedLevel = contrastLevel.coerceIn(0.0, 1.0)
        localDataSource.saveContrastLevel(clampedLevel)
        if (clampedLevel != contrastLevel) {
            Log.w(TAG, "Contrast level $contrastLevel coerced to $clampedLevel")
        }
    }

}
