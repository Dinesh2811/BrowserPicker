package com.dinesh.m3theme.di

import com.dinesh.m3theme.data.repository.ThemeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import com.dinesh.m3theme.domain.*

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