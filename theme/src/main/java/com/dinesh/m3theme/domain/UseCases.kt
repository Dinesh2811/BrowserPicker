package com.dinesh.m3theme.domain

import androidx.compose.ui.graphics.Color
import com.dinesh.m3theme.data.repository.ThemeRepository
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
