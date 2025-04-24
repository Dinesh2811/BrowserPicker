package com.dinesh.m3theme.presentation

import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.dinesh.m3theme.domain.*
import com.dinesh.m3theme.model.PaletteStyle
import com.dinesh.m3theme.model.ThemeMode
import com.dinesh.m3theme.model.ThemeState
import com.dinesh.m3theme.model.ThemeUiState
import com.dinesh.m3theme.theme.ColorSchemeGenerator
import com.dinesh.m3theme.util.ThemeDefaults
import dynamiccolor.DynamicScheme
import hct.Hct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val getThemeStateUseCase: GetThemeStateUseCase,
    private val saveSeedColorUseCase: SaveSeedColorUseCase,
    private val saveThemeModeUseCase: SaveThemeModeUseCase,
    private val savePaletteStyleUseCase: SavePaletteStyleUseCase,
    private val saveContrastLevelUseCase: SaveContrastLevelUseCase,
    private val colorSchemeGenerator: ColorSchemeGenerator,
) : ViewModel() {
    private val TAG = "log_ThemeViewModel"

    // --- Internal Mutable State Flows for Inputs ---
    private val _isSystemInDarkTheme = MutableStateFlow<Boolean?>(null)

    // Holds the state reflecting the latest persisted value or user interaction.
    // Initialized from the repository first, then updated by UI actions.
    private val _currentThemeState = MutableStateFlow<ThemeState?>(null) // Start as null

    init {
        // 1. Observe persisted state: Split initial load/sync observation for clarity
        viewModelScope.launch {
            // Task 1: Fetch the *first* state and mark initial load complete
            try {
                val initialState = getThemeStateUseCase().first() // Suspends until first emission
                _currentThemeState.value = initialState
//                  _isLoading.value = false
                Log.d(TAG, "Initial load complete. State: $initialState")

                // Task 2: Continuously collect subsequent updates (for sync)
                getThemeStateUseCase()
                    .drop(1) // Skip the initial value we already processed
                    .distinctUntilChanged()
                    .collect { subsequentPersistedState ->
                        // Only update internal state if it differs from the current one.
                        // This prevents sync events from overwriting user edits that are currently debouncing before save.
                        if (_currentThemeState.value != subsequentPersistedState) {
                            _currentThemeState.value = subsequentPersistedState
                            Log.d(TAG, "Sync update applied: $subsequentPersistedState")
                        } else {
                            Log.d(TAG, "Sync update ignored (matches current state).")
                        }
//                          _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error during initial theme load or sync collection ${e.message}", e)
            }
        }

        // 2. Debounced Save Logic: Observe internal state changes for saving (debounced)
        viewModelScope.launch {
            _currentThemeState
                .filterNotNull()
                .debounce(500L)
                .distinctUntilChanged()
                // Use collectLatest: If a new state arrives while saving the previous one, cancel the previous save and start saving the new one.
                .collectLatest { stateToSave ->
                    // logDebug("Debounced change detected. Saving: $stateToSave")
                    launch { saveSeedColorUseCase(stateToSave.seedColor) }
                    launch { saveThemeModeUseCase(stateToSave.themeMode) }
                    launch { savePaletteStyleUseCase(stateToSave.paletteStyle) }
                    launch { saveContrastLevelUseCase(stateToSave.contrastLevel) }
                }
        }
    }

    // 3. Derive the final UI State using combine + stateIn
    val themeUiState: StateFlow<ThemeUiState> = combine(
        _currentThemeState,
        _isSystemInDarkTheme,
    ) { currentInternalState, isSystemDark ->

        // Use default if state hasn't been loaded yet
        val themeState = currentInternalState?: ThemeState.DEFAULT

        val isDarkEffective = determineDarkness(themeMode = themeState.themeMode, isSystemDark == true)

        //  _isLoading.value = false
        if (currentInternalState == null) {
            ThemeUiState(
                themeState = themeState,
                isSystemInDarkTheme = isSystemDark,
                // Use a sensible default color scheme during load
                colorScheme = if (isDarkEffective) ThemeDefaults.COLOR_SCHEME else ThemeDefaults.lightColorScheme(),
                isLoading = true,
                isDarkEffective = isDarkEffective
            )
        } else {
            val colorScheme: ColorScheme = withContext(Dispatchers.Default) {
                generateColorSchemeInternal(
                    seedColor = themeState.seedColor,
                    isDark = isDarkEffective,
                    paletteStyle = themeState.paletteStyle,
                    contrastLevel = themeState.contrastLevel
                )
            }
            ThemeUiState(
                themeState = themeState,
                isSystemInDarkTheme = isSystemDark,
                colorScheme = colorScheme,
                isDarkEffective = isDarkEffective,
                isLoading = false,
            )
        }
    }
        .distinctUntilChanged() // Only emit when the calculated ThemeUiState actually changes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ThemeUiState(
                themeState = ThemeState.DEFAULT,
                isSystemInDarkTheme = null,
                colorScheme = ThemeDefaults.lightColorScheme(),
                isDarkEffective = false,
                isLoading = true,
            )
        )


    // --- Public Functions for UI Interaction ---
    // These functions now update _currentThemeState, triggering combine and debounce flows.
    fun updateSeedColor(seedColor: Color) {
        _currentThemeState.update { it?.copy(seedColor = seedColor) }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        _currentThemeState.update { it?.copy(themeMode = themeMode) }
    }

    fun updatePaletteStyle(style: PaletteStyle) {
        _currentThemeState.update { it?.copy(paletteStyle = style) }
    }

    fun updateContrastLevel(level: Double) {
        val clampedLevel = level.coerceIn(0.0, 1.0)
        _currentThemeState.update { it?.copy(contrastLevel = clampedLevel) }
    }

    /**
     * Call this from your Composable when the system's dark theme status changes.
     */
    fun updateSystemDarkTheme(isSystemDark: Boolean) {
        // Update the dedicated flow for system theme status
        _isSystemInDarkTheme.value = isSystemDark
        // The 'combine' operator handles the rest.
    }

    // --- Private Helper Functions ---

    private fun generateColorSchemeInternal(seedColor: Color, isDark: Boolean, paletteStyle: PaletteStyle, contrastLevel: Double): ColorScheme {
        val hct: Hct? = Hct.fromInt(seedColor.toArgb())
        val dynamicScheme: DynamicScheme = colorSchemeGenerator.createDynamicScheme(paletteStyle, hct, isDark, contrastLevel)
        return colorSchemeGenerator.createColorScheme(dynamicScheme)
    }

    private fun determineDarkness(themeMode: ThemeMode, isSystemInDark: Boolean): Boolean {
        return when (themeMode) {
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
            ThemeMode.System -> isSystemInDark
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared called.")
    }
}



/*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ThemeViewModel @Inject constructor(
    application: Application,
    private val getThemeStateUseCase: GetThemeStateUseCase,
    private val saveSeedColorUseCase: SaveSeedColorUseCase,
    private val saveThemeModeUseCase: SaveThemeModeUseCase,
    private val savePaletteStyleUseCase: SavePaletteStyleUseCase,
    private val saveContrastLevelUseCase: SaveContrastLevelUseCase,
    private val colorSchemeGenerator: ColorSchemeGenerator,
) : ViewModel() {
    private var _isSystemInDarkTheme = isSystemInitiallyDark(application)
    private fun isSystemInitiallyDark(app: Application): Boolean {
        return (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
    fun updateSystemDarkMode(isSystemDark: Boolean) {
        _isSystemInDarkTheme = isSystemDark
    }

    private val _themeUiState = MutableStateFlow<ThemeUiState>(ThemeUiState(isLoading = true))
    fun updateThemeUiState(themeUiState: (ThemeUiState) -> ThemeUiState) {
        _themeUiState.update(themeUiState)
    }
    fun updateThemeState(update: (ThemeState) -> ThemeState) {
        _themeUiState.update { it.copy(themeState = update(it.themeState)) }
    }
    val themeUiState: StateFlow<ThemeUiState> = _themeUiState
        .debounce(10)
        .mapLatest { themeUiState ->
            val themeState: ThemeState = themeUiState.themeState
            val useDarkTheme: Boolean = when (themeState.themeMode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> _isSystemInDarkTheme
            }

            val colorScheme = colorSchemeGenerator.generateColorScheme(
                seedColor = themeState.seedColor,
                isDark = useDarkTheme,
                paletteStyle = themeState.paletteStyle,
                contrastLevel = themeState.contrastLevel
            )

            ThemeUiState(
                themeState = themeState,
                isSystemInDarkTheme = _isSystemInDarkTheme,
                colorScheme = colorScheme,
                isLoading = false
            )
        }
        .catch { e ->
            emit(
                ThemeUiState(
                    themeState = ThemeState.DEFAULT,
                    isSystemInDarkTheme = _isSystemInDarkTheme,
                    colorScheme = ThemeDefaults.COLOR_SCHEME,
                    isLoading = false
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeUiState(isLoading = true)
        )

    init {
        observePersistedState()
        observeAndPersistChanges()
    }

    private fun observePersistedState() {
        viewModelScope.launch(Dispatchers.IO) {
            getThemeStateUseCase()
                .distinctUntilChanged()
                .collect { persistedState ->
                    updateThemeState { persistedState }
                }
        }
    }

    private fun observeAndPersistChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            _themeUiState
                .filterNotNull()
                .distinctUntilChanged()
                .debounce(500)
                .collectLatest { stateToSave ->
                    saveSeedColorUseCase(stateToSave.themeState.seedColor)
                    saveThemeModeUseCase(stateToSave.themeState.themeMode)
                    savePaletteStyleUseCase(stateToSave.themeState.paletteStyle)
                    saveContrastLevelUseCase(stateToSave.themeState.contrastLevel.coerceIn(0.0, 1.0))
                }
        }
    }

}


 */

/*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ThemeViewModel @Inject constructor(
    application: Application,
    private val getThemeStateUseCase: GetThemeStateUseCase,
    private val saveSeedColorUseCase: SaveSeedColorUseCase,
    private val saveThemeModeUseCase: SaveThemeModeUseCase,
    private val savePaletteStyleUseCase: SavePaletteStyleUseCase,
    private val saveContrastLevelUseCase: SaveContrastLevelUseCase,
    private val colorSchemeGenerator: ColorSchemeGenerator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
): ViewModel() {
    private val _themeUiState: MutableStateFlow<ThemeUiState> = MutableStateFlow(ThemeUiState())
    val themeUiState: StateFlow<ThemeUiState> = _themeUiState.asStateFlow()
    fun updateThemeUiState(themeUiState: (ThemeUiState) -> ThemeUiState) {
        _themeUiState.update(themeUiState)
    }

    fun updateThemeState(update: (ThemeState) -> ThemeState) {
        _themeUiState.update { it.copy(themeState = it.themeState?.let { it1 -> update(it1) }) }
    }

    private val _isSystemDark = MutableStateFlow(isSystemInitiallyDark(application))

    init {
        observePersistedState()
        viewModelScope.launch {
            var lastSavedThemeState: ThemeState? = null
            combine(_themeUiState.map { it.themeState }.filterNotNull(), _isSystemDark) { themeState: ThemeState, isSysDark: Boolean ->
                themeState to isSysDark
            }.distinctUntilChanged().debounce(10).conflate().map { (themeState: ThemeState, isSysDark: Boolean) ->
                    val useDarkTheme: Boolean = when (themeState.themeMode) {
                        ThemeMode.Light -> false
                        ThemeMode.Dark -> true
                        ThemeMode.System, null -> isSysDark
                    }
                    val seedColor: Color = themeState.seedColor ?: ThemeDefaults.SEED_COLOR
                    val paletteStyle: PaletteStyle = themeState.paletteStyle ?: ThemeDefaults.PALETTE_STYLE
                    val contrastLevel: Double = themeState.contrastLevel?.coerceIn(0.0, 1.0) ?: ThemeDefaults.CONTRAST_LEVEL

                    val colorScheme: ColorScheme = withContext(Dispatchers.Default) {
                        colorSchemeGenerator.generateColorScheme(
                            seedColor = seedColor, isDark = useDarkTheme, paletteStyle = paletteStyle, contrastLevel = contrastLevel
                        )
                    }
                    _themeUiState.update { it.copy(colorScheme = colorScheme) }
                    themeState
                }.distinctUntilChanged().debounce(500).conflate().collectLatest { currentThemeState ->
                    var changed = false

                    if (currentThemeState.seedColor != lastSavedThemeState?.seedColor && currentThemeState.seedColor != null) {
                        saveSeedColorUseCase(currentThemeState.seedColor)
                        changed = true
                    }
                    if (currentThemeState.themeMode != lastSavedThemeState?.themeMode && currentThemeState.themeMode != null) {
                        saveThemeModeUseCase(currentThemeState.themeMode)
                        changed = true
                    }
                    if (currentThemeState.paletteStyle != lastSavedThemeState?.paletteStyle && currentThemeState.paletteStyle != null) {
                        savePaletteStyleUseCase(currentThemeState.paletteStyle)
                        changed = true
                    }
                    val coercedContrast = currentThemeState.contrastLevel?.coerceIn(0.0, 1.0) ?: ThemeDefaults.CONTRAST_LEVEL
                    val lastCoercedContrast = lastSavedThemeState?.contrastLevel?.coerceIn(0.0, 1.0) ?: ThemeDefaults.CONTRAST_LEVEL
                    if (coercedContrast != lastCoercedContrast) {
                        saveContrastLevelUseCase(coercedContrast)
                        changed = true
                    }
                    if (changed) {
                        lastSavedThemeState = currentThemeState
                    }
                }
        }
    }

    private fun observePersistedState() {
        viewModelScope.launch(Dispatchers.IO) {
            getThemeStateUseCase().distinctUntilChanged().collectLatest { persistedState ->
                    if (_themeUiState.value.themeState == null || _themeUiState.value.themeState != persistedState) {
                        updateThemeState { persistedState }
                    }
                }
        }
    }

    fun updateSystemDarkMode(isSystemDark: Boolean) {
        if (_isSystemDark.value != isSystemDark) {
            _isSystemDark.value = isSystemDark
        }
    }

    private fun isSystemInitiallyDark(app: Application): Boolean {
        return (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

*/
