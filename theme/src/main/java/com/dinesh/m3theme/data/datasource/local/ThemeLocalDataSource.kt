package com.dinesh.m3theme.data.datasource.local

import android.util.Log
import com.dinesh.m3theme.model.ThemePreferencesData
import kotlinx.coroutines.flow.Flow
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface ThemeLocalDataSource {
    fun getThemePreferences(): Flow<ThemePreferencesData>

    suspend fun saveSeedColor(seedColorHex: String)
    suspend fun saveThemeMode(themeModeName: String)
    suspend fun savePaletteStyle(paletteStyleName: String)
    suspend fun saveContrastLevel(contrastLevel: Double)
}


@Singleton
class ThemeLocalDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
): ThemeLocalDataSource {
    private companion object {
        private const val TAG = "log_ThemeLocalDataSourceImpl"
        val SEED_COLOR_KEY = stringPreferencesKey("pref_seed_color_hex")
        val THEME_MODE_KEY = stringPreferencesKey("pref_theme_mode")
        val PALETTE_STYLE_KEY = stringPreferencesKey("pref_palette_style")
        val CONTRAST_LEVEL_KEY = doublePreferencesKey("pref_contrast_level")
    }

    override fun getThemePreferences(): Flow<ThemePreferencesData> {
        return dataStore.data
            .catch { exception ->
                Log.e(TAG, "Error getting theme state: ${exception.message}")
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                mapPreferencesToData(preferences)
            }
    }

    private fun mapPreferencesToData(preferences: Preferences): ThemePreferencesData {
        val seedColor = preferences[SEED_COLOR_KEY]
        val themeMode = preferences[THEME_MODE_KEY]
        val paletteStyle = preferences[PALETTE_STYLE_KEY]
        val contrastLevel = preferences[CONTRAST_LEVEL_KEY]
        return ThemePreferencesData(seedColor, themeMode, paletteStyle, contrastLevel)
    }

    override suspend fun saveSeedColor(seedColorHex: String) {
        try {
            dataStore.edit { preferences ->
                preferences[SEED_COLOR_KEY] = seedColorHex
                Log.d(TAG, "Saved seed color: $seedColorHex")
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Error saving seed color.", exception)
        } catch (exception: Exception) {
            Log.e(TAG, "Unexpected error saving seed color.", exception)
        }
    }

    override suspend fun saveThemeMode(themeModeName: String) {
        try {
            dataStore.edit { preferences ->
                preferences[THEME_MODE_KEY] = themeModeName
                Log.d(TAG, "Saved theme mode: $themeModeName")
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Error saving theme mode.", exception)
        } catch (exception: Exception) {
            Log.e(TAG, "Unexpected error saving theme mode.", exception)
        }
    }

    override suspend fun savePaletteStyle(paletteStyleName: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PALETTE_STYLE_KEY] = paletteStyleName
                Log.d(TAG, "Saved palette style: $paletteStyleName")
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Error saving palette style.", exception)
        } catch (exception: Exception) {
            Log.e(TAG, "Unexpected error saving palette style.", exception)
        }
    }

    override suspend fun saveContrastLevel(contrastLevel: Double) {
        try {
            dataStore.edit { preferences ->
                preferences[CONTRAST_LEVEL_KEY] = contrastLevel
                Log.d(TAG, "Saved contrast level: $contrastLevel")
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Error saving contrast level.", exception)
        } catch (exception: Exception) {
            Log.e(TAG, "Unexpected error saving contrast level.", exception)
        }
    }

}