package com.dinesh.m3theme.di

import com.dinesh.m3theme.data.repository.ThemeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import androidx.compose.ui.graphics.Color
import com.dinesh.m3theme.model.PaletteStyle
import com.dinesh.m3theme.model.ThemeMode
import com.dinesh.m3theme.model.ThemeState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// --- Get Use Case ---
class GetThemeStateUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    operator fun invoke(): Flow<ThemeState> {
        return repository.getThemeState()
    }
}

// --- Save Use Cases ---
class SaveSeedColorUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(color: Color) {
        repository.saveSeedColor(color)
    }
}

class SaveThemeModeUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(mode: ThemeMode) {
        repository.saveThemeMode(mode)
    }
}

class SavePaletteStyleUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(style: PaletteStyle) {
        repository.savePaletteStyle(style)
    }
}

class SaveContrastLevelUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(level: Double) {
        repository.saveContrastLevel(level)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideGetThemeStateUseCase(themeRepository: ThemeRepository): GetThemeStateUseCase {
        return GetThemeStateUseCase(themeRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideSaveSeedColorUseCase(themeRepository: ThemeRepository): SaveSeedColorUseCase {
        return SaveSeedColorUseCase(themeRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideSaveThemeModeUseCase(themeRepository: ThemeRepository): SaveThemeModeUseCase {
        return SaveThemeModeUseCase(themeRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideSavePaletteStyleUseCase(themeRepository: ThemeRepository): SavePaletteStyleUseCase {
        return SavePaletteStyleUseCase(themeRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideSaveContrastLevelUseCase(themeRepository: ThemeRepository): SaveContrastLevelUseCase {
        return SaveContrastLevelUseCase(themeRepository)
    }
}