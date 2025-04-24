package com.dinesh.m3theme

import com.dinesh.m3theme.model.PaletteStyle

/*
com.example.yourapp
│
├── data
│   ├── local
│   │   ├── ColorPreferences.kt
│   │   └── ColorDataSource.kt
│   ├── model
│   │   └── ColorSchemeModel.kt
│   └── repository
│       └── ColorRepositoryImpl.kt
│
├── domain
│   ├── model
│   │   └── ColorScheme.kt
│   ├── repository
│   │   └── ColorRepository.kt
│   └── usecase
│       └── GenerateColorSchemeUseCase.kt
│
├── presentation
│   ├── viewmodel
│   │   └── ColorViewModel.kt
│   └── ui
│       └── ColorSchemeScreen.kt
│
└── di
    └── AppModule.kt
 */

/*
class ColorPreferences {
    suspend fun saveSeedColor(color: String)
    suspend fun getSeedColor(): String
    suspend fun saveIsDarkTheme(isDark: Boolean)
    suspend fun getIsDarkTheme(): Boolean
    suspend fun savePaletteStyle(style: String)
    suspend fun getPaletteStyle(): String
    suspend fun saveContrastLevel(level: Int)
    suspend fun getContrastLevel(): Int
}
interface ColorDataSource {
    suspend fun saveColorScheme(colorScheme: ColorSchemeModel)
    suspend fun getColorScheme(): ColorSchemeModel
}
class ColorRepositoryImpl(private val colorDataSource: ColorDataSource) : ColorRepository {
    override suspend fun saveColorScheme(colorScheme: ColorSchemeModel)
    override suspend fun getColorScheme(): ColorSchemeModel
}
data class ColorScheme(
    val seedColor: Color,
    val isDarkTheme: Boolean,
    val paletteStyle: PaletteStyle,
    val contrastLevel: Int
)
interface ColorRepository {
    suspend fun saveColorScheme(colorScheme: ColorScheme)
    suspend fun getColorScheme(): ColorScheme
}
class GenerateColorSchemeUseCase(private val colorRepository: ColorRepository) {
    suspend fun execute(seedColor: Color, isDarkTheme: Boolean, paletteStyle: PaletteStyle, contrastLevel: Int): ColorScheme
}
class ColorViewModel(private val generateColorSchemeUseCase: GenerateColorSchemeUseCase) : ViewModel() {
    fun generateColorScheme(seedColor: Color, isDarkTheme: Boolean, paletteStyle: PaletteStyle, contrastLevel: Int)
}
@Composable
fun ColorSchemeScreen(viewModel: ColorViewModel) {
    // UI implementation
}
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideColorRepository(dataSource: ColorDataSource): ColorRepository = ColorRepositoryImpl(dataSource)

    @Provides
    @Singleton
    fun provideGenerateColorSchemeUseCase(repository: ColorRepository): GenerateColorSchemeUseCase = GenerateColorSchemeUseCase(repository)
}
 */

/*
class GenerateColorSchemeUseCase @Inject constructor() {
    fun execute(seedColor: Long, isDarkTheme: Boolean, paletteStyle: PaletteStyle, contrastLevel: Float): ColorScheme
}
data class ColorSchemeParams(
    val seedColor: Long,
    val isDarkTheme: Boolean,
    val paletteStyle: PaletteStyle,
    val contrastLevel: Float
)
@Singleton
class ColorSchemeRepositoryImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ColorSchemeRepository {
    override suspend fun saveParams(params: ColorSchemeParams)
    override suspend fun getParams(): ColorSchemeParams?
}
@Singleton
class DataStoreManager @Inject constructor(@ApplicationContext context: Context) {
    suspend fun saveSeedColor(color: Long)
    suspend fun getSeedColor(): Long?
    suspend fun saveIsDarkTheme(isDarkTheme: Boolean)
    suspend fun getIsDarkTheme(): Boolean?
    suspend fun savePaletteStyle(paletteStyle: PaletteStyle)
    suspend fun getPaletteStyle(): PaletteStyle?
    suspend fun saveContrastLevel(contrastLevel: Float)
    suspend fun getContrastLevel(): Float?
}
fun Color.toLong(): Long { /* Conversion logic */ }
fun Long.toColor(): Color { /* Conversion logic */ }
interface ColorSchemeRepository {
    suspend fun saveParams(params: ColorSchemeParams)
    suspend fun getParams(): ColorSchemeParams?
}
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val generateColorSchemeUseCase: GenerateColorSchemeUseCase,
    private val repository: ColorSchemeRepository
) : ViewModel() {
    val colorSchemeFlow: StateFlow<ColorScheme>

    init {
        loadTheme()
    }

    private fun loadTheme()

    fun updateTheme(seedColor: Long, isDarkTheme: Boolean, paletteStyle: PaletteStyle, contrastLevel: Float)

    private suspend fun saveTheme(params: ColorSchemeParams)

    private suspend fun generateColorScheme(params: ColorSchemeParams): ColorScheme
}
@Composable
fun ThemeScreen(viewModel: ThemeViewModel) {
    // Observe color scheme and render UI.
}

@Composable
private fun ThemeSettings(/* Parameters */) {
    // UI for modifying theme settings.
}

@Composable
private fun PreviewColors(/* Parameters */) {
    // UI for previewing generated colors.
}
fun Long.toComposeColor(): androidx.compose.ui.graphics.Color { /* Conversion logic */ }
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideColorSchemeRepository(
        dataStoreManager: DataStoreManager
    ): ColorSchemeRepository = ColorSchemeRepositoryImpl(dataStoreManager)
}
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideGenerateColorSchemeUseCase(): GenerateColorSchemeUseCase = GenerateColorSchemeUseCase()
}
fun Color.toLong(): Long = (alpha * 255).toInt().toLong().shl(24) or
                          (red * 255).toInt().toLong().shl(16) or
                          (green * 255).toInt().toLong().shl(8) or
                          (blue * 255).toInt().toLong()

fun Long.toColor(): Color = Color(
    alpha = ((this shr 24) and 0xFF) / 255f,
    red = ((this shr 16) and 0xFF) / 255f,
    green = ((this shr 8) and 0xFF) / 255f,
    blue = (this and 0xFF) / 255f
)

 */

/* you
com.example.app
├── data
│   ├── local
│   │   ├── PreferencesDataSource.kt
│   │   └── PreferencesDataSourceImpl.kt
│   ├── model
│   │   └── ColourScheme.kt
│   └── repository
│       └── ColourSchemeRepository.kt
├── domain
│   ├── model
│   │   └── PaletteStyle.kt
│   ├── repository
│   │   └── ColourSchemeRepository.kt
│   └── usecase
│       └── GenerateColourSchemeUseCase.kt
└── presentation
    ├── viewmodel
    │   └── ColourSchemeViewModel.kt
    └── ui
        └── ColourSchemeScreen.kt

interface PreferencesDataSource {
    suspend fun saveSeedColor(color: Long)
    suspend fun getSeedColor(): Long
    suspend fun saveIsDarkTheme(isDark: Boolean)
    suspend fun getIsDarkTheme(): Boolean
    suspend fun savePaletteStyle(style: PaletteStyle)
    suspend fun getPaletteStyle(): PaletteStyle
    suspend fun saveContrastLevel(level: Int)
    suspend fun getContrastLevel(): Int
}
class PreferencesDataSourceImpl : PreferencesDataSource {
    override suspend fun saveSeedColor(color: Long) { /* Implementation */ }
    override suspend fun getSeedColor(): Long { /* Implementation */ }
    override suspend fun saveIsDarkTheme(isDark: Boolean) { /* Implementation */ }
    override suspend fun getIsDarkTheme(): Boolean { /* Implementation */ }
    override suspend fun savePaletteStyle(style: PaletteStyle) { /* Implementation */ }
    override suspend fun getPaletteStyle(): PaletteStyle { /* Implementation */ }
    override suspend fun saveContrastLevel(level: Int) { /* Implementation */ }
    override suspend fun getContrastLevel(): Int { /* Implementation */ }
}
data class ColourScheme(
    val seedColor: Long,
    val isDarkTheme: Boolean,
    val paletteStyle: PaletteStyle,
    val contrastLevel: Int
)
interface ColourSchemeRepository {
    suspend fun getColourScheme(): ColourScheme
    suspend fun saveColourScheme(colourScheme: ColourScheme)
}
interface ColourSchemeRepository {
    suspend fun getColourScheme(): ColourScheme
    suspend fun saveColourScheme(colourScheme: ColourScheme)
}
class GenerateColourSchemeUseCase(private val repository: ColourSchemeRepository) {
    suspend fun execute(seedColor: Long, isDarkTheme: Boolean, paletteStyle: PaletteStyle, contrastLevel: Int): ColourScheme {
        // Implementation to generate ColourScheme
    }
}
class ColourSchemeViewModel(private val generateColourSchemeUseCase: GenerateColourSchemeUseCase) : ViewModel() {
    fun generateColourScheme(seedColor: Long, isDarkTheme: Boolean, paletteStyle: PaletteStyle, contrastLevel: Int) {
        // Implementation
    }
}
@Composable
fun ColourSchemeScreen(viewModel: ColourSchemeViewModel) {
    // Implementation of UI
}
 */


/*
https://www.phind.com/search/cm9478r4i0000356n4zc4atz0

classDiagram
    class ColorSchemeGenerator {
        +generateColorScheme(seedColor: Long, isDarkTheme: Boolean, paletteStyle: PaletteStyle, contrastLevel: Float): ColorScheme
    }

    class ThemeRepository {
        <<interface>>
        +getColorPreferences(): Flow<ColorPreferences>
        +updateColorPreferences(ColorPreferences): Flow<Unit>
    }

    class DataStoreImpl {
        -dataStore: DataStore<Preferences>
        +getColorPreferences()
        +updateColorPreferences(ColorPreferences)
    }

    class ColorPreferences {
        +seedColor: Long
        +isDarkTheme: Boolean
        +paletteStyle: String
        +contrastLevel: Float
    }

    class PaletteStyle {
        <<enumeration>>
        TonalSpot
        Neutral
        Vibrant
        Rainbow
        Expressive
        FruitSalad
        Monochrome
        Fidelity
        Content
    }

    class ColorScheme {
        <<interface>>
        +primary: Color
        +onPrimary: Color
        +secondary: Color
        +onSecondary: Color
        +background: Color
        +onBackground: Color
        +surface: Color
        +onSurface: Color
    }

    ThemeRepository <|.. DataStoreImpl : implements
    ColorSchemeGenerator ..> ColorScheme : generates
    ColorPreferences --> PaletteStyle : uses
    ColorPreferences --> ColorScheme : produces
    DataStoreImpl ..> ColorPreferences : manages













fun Color.toLong(): Long {
    return ((alpha.toInt() shl 24) or
            (red.toInt() shl 16) or
            (green.toInt() shl 8) or
             blue.toInt())
}

fun Long.toColor(): Color {
    val alpha = (this shr 24) and 0xFF
    val red = (this shr 16) and 0xFF
    val green = (this shr 8) and 0xFF
    val blue = this and 0xFF

    return Color(
        alpha = alpha.toFloat() / 255f,
        red = red.toFloat() / 255f,
        green = green.toFloat() / 255f,
        blue = blue.toFloat() / 255f
    )
}
import kotlinx.serialization.Serializable

@Serializable
enum class PaletteStyle {
    TonalSpot, Neutral, Vibrant, Rainbow, Expressive, FruitSalad, Monochrome, Fidelity, Content
}

// In your preferences proto:
preferences {
    string("palette_style") { defaultValue = "" }
}

// Usage:
suspend fun Preferences.paletteStyle(): PaletteStyle {
    val styleString = stringPreferencesKey("palette_style").get(this)
    return styleString?.let { PaletteStyle.valueOf(it) } ?: PaletteStyle.TonalSpot
}

suspend fun Preferences.setPaletteStyle(style: PaletteStyle) {
    stringPreferencesKey("palette_style").set(this, style.name)
}
// domain/theme/
data class ColorPreferences(
    val seedColor: Long,
    val isDarkTheme: Boolean,
    val paletteStyle: PaletteStyle,
    val contrastLevel: Float
)

interface ThemeRepository {
    fun getColorPreferences(): Flow<ColorPreferences>
    suspend fun updateColorPreferences(preferences: ColorPreferences)
}

class ColorSchemeGenerator @Inject constructor() {
    fun generateColorScheme(
        seedColor: Long,
        isDarkTheme: Boolean,
        paletteStyle: PaletteStyle,
        contrastLevel: Float
    ): ColorScheme
}

// data/local/
class ThemeLocalDataSource @Inject constructor(private val context: Context) : ThemeRepository {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("theme_preferences")

    override fun getColorPreferences(): Flow<ColorPreferences> =
        dataStore.data.map { prefs ->
            ColorPreferences(
                seedColor = prefs.longPreferencesKey("seed_color").getOrDefault(Color.Default.toLong()),
                isDarkTheme = prefs.booleanPreferencesKey("is_dark_theme").getOrDefault(false),
                paletteStyle = prefs.paletteStyle(),
                contrastLevel = prefs.floatPreferencesKey("contrast_level").getOrDefault(1f)
            )
        }

    override suspend fun updateColorPreferences(preferences: ColorPreferences) {
        dataStore.edit { prefs ->
            prefs.longPreferencesKey("seed_color").set(preferences.seedColor)
            prefs.booleanPreferencesKey("is_dark_theme").set(preferences.isDarkTheme)
            prefs.setPaletteStyle(preferences.paletteStyle)
            prefs.floatPreferencesKey("contrast_level").set(preferences.contrastLevel)
        }
    }
}

// presentation/
class ThemeViewModel @Inject constructor(
    private val repository: ThemeRepository,
    private val colorSchemeGenerator: ColorSchemeGenerator
) : ViewModel() {
    private val _colorScheme = MutableStateFlow<ColorScheme>(MaterialTheme.colorScheme)
    val colorScheme: StateFlow<ColorScheme> = _colorScheme

    fun updateTheme(preferences: ColorPreferences) {
        viewModelScope.launch {
            repository.updateColorPreferences(preferences)
            val newScheme = colorSchemeGenerator.generateColorScheme(
                preferences.seedColor,
                preferences.isDarkTheme,
                preferences.paletteStyle,
                preferences.contrastLevel
            )
            _colorScheme.value = newScheme
        }
    }
}
 */

/*
// Root package: com.yourcompany.yourapp

// data layer
package com.yourcompany.yourapp.data {

    // DataStore
    package datastore {
        import androidx.datastore.core.DataStore
        import androidx.datastore.preferences.core.Preferences
        import kotlinx.coroutines.flow.Flow

        interface PreferencesDataSource {
            suspend fun saveSeedColor(color: Long)
            fun getSeedColor(): Flow<Long?>
            suspend fun saveIsDarkTheme(isDark: Boolean)
            fun getIsDarkTheme(): Flow<Boolean?>
            suspend fun savePaletteStyle(paletteStyle: PaletteStyle)
            fun getPaletteStyle(): Flow<PaletteStyle?>
            suspend fun saveContrastLevel(contrastLevel: Double)
            fun getContrastLevel(): Flow<Double?>
        }

        class PreferencesDataSourceImpl(
            private val dataStore: DataStore<Preferences>
        ) : PreferencesDataSource {
            // Implement the functions here using dataStore.edit() and Preferences.keys
            override suspend fun saveSeedColor(color: Long) {}
            override fun getSeedColor(): Flow<Long?> = flowOf(null) {}
            override suspend fun saveIsDarkTheme(isDark: Boolean) {}
            override fun getIsDarkTheme(): Flow<Boolean?> = flowOf(null) {}
            override suspend fun savePaletteStyle(paletteStyle: PaletteStyle) {}
            override fun getPaletteStyle(): Flow<PaletteStyle?> = flowOf(null) {}
            override suspend fun saveContrastLevel(contrastLevel: Double) {}
            override fun getContrastLevel(): Flow<Double?> = flowOf(null) {}
        }
    }

    // Repository
    package repository {
        import com.yourcompany.yourapp.domain.model.CustomColorSchemeParams
        import kotlinx.coroutines.flow.Flow

        interface ColorSchemeRepository {
            suspend fun saveColorSchemeParams(params: CustomColorSchemeParams)
            fun getColorSchemeParams(): Flow<CustomColorSchemeParams>
        }

        class ColorSchemeRepositoryImpl(
            private val preferencesDataSource: com.yourcompany.yourapp.data.datastore.PreferencesDataSource
        ) : ColorSchemeRepository {
            override suspend fun saveColorSchemeParams(params: CustomColorSchemeParams) {}
            override fun getColorSchemeParams(): Flow<CustomColorSchemeParams> = flowOf(CustomColorSchemeParams()) {}
        }
    }
}

// domain layer
package com.yourcompany.yourapp.domain {

    // Model
    package model {
        import androidx.compose.material3.ColorScheme
        import kotlinx.serialization.Serializable

        @Serializable
        enum class PaletteStyle {
            TonalSpot, Neutral, Vibrant, Rainbow, Expressive, FruitSalad, Monochrome, Fidelity, Content,
        }

        @Serializable
        data class CustomColorSchemeParams(
            val seedColor: Long = 0xFF000000, // Default black color as Long
            val isDarkTheme: Boolean = false,
            val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
            val contrastLevel: Double = 0.0,
        )

        data class GeneratedColorScheme(
            val colorScheme: ColorScheme
        )
    }

    // UseCase
    package usecase {
        import com.yourcompany.yourapp.domain.model.CustomColorSchemeParams
        import com.yourcompany.yourapp.domain.model.GeneratedColorScheme
        import kotlinx.coroutines.flow.Flow

        interface GenerateColorSchemeUseCase {
            operator fun invoke(params: CustomColorSchemeParams): Flow<GeneratedColorScheme>
        }

        class GenerateColorSchemeUseCaseImpl(
            // Dependency on the Material3 library (no need for a repository here for the generation logic itself)
        ) : GenerateColorSchemeUseCase {
            override operator fun invoke(params: CustomColorSchemeParams): Flow<GeneratedColorScheme> = flow {
                // Implementation to use the Material3 code to generate ColorScheme
                // based on params.seedColor, params.isDarkTheme, params.paletteStyle, and params.contrastLevel
                // Emit the GeneratedColorScheme
            }
        }

        interface GetColorSchemeParamsUseCase {
            operator fun invoke(): Flow<CustomColorSchemeParams>
        }

        class GetColorSchemeParamsUseCaseImpl(
            private val colorSchemeRepository: com.yourcompany.yourapp.data.repository.ColorSchemeRepository
        ) : GetColorSchemeParamsUseCase {
            override operator fun invoke(): Flow<CustomColorSchemeParams> =
                colorSchemeRepository.getColorSchemeParams()
        }

        interface SaveColorSchemeParamsUseCase {
            suspend operator fun invoke(params: CustomColorSchemeParams)
        }

        class SaveColorSchemeParamsUseCaseImpl(
            private val colorSchemeRepository: com.yourcompany.yourapp.data.repository.ColorSchemeRepository
        ) : SaveColorSchemeParamsUseCase {
            override suspend operator fun invoke(params: CustomColorSchemeParams) {
                colorSchemeRepository.saveColorSchemeParams(params)
            }
        }
    }
}

// presentation layer (MVVM)
package com.yourcompany.yourapp.presentation {

    // ViewModel
    package viewmodel {
        import androidx.compose.runtime.State
        import androidx.compose.runtime.mutableStateOf
        import androidx.lifecycle.ViewModel
        import androidx.lifecycle.viewModelScope
        import androidx.compose.material3.ColorScheme
        import com.yourcompany.yourapp.domain.model.CustomColorSchemeParams
        import com.yourcompany.yourapp.domain.model.PaletteStyle
        import com.yourcompany.yourapp.domain.usecase.GenerateColorSchemeUseCase
        import com.yourcompany.yourapp.domain.usecase.GetColorSchemeParamsUseCase
        import com.yourcompany.yourapp.domain.usecase.SaveColorSchemeParamsUseCase
        import dagger.hilt.android.lifecycle.HiltViewModel
        import kotlinx.coroutines.flow.collectLatest
        import kotlinx.coroutines.launch
        import javax.inject.Inject

        data class ColorSchemeUiState(
            val colorScheme: ColorScheme = androidx.compose.material3.lightColorScheme(),
            val seedColor: Long = 0xFF000000,
            val isDarkTheme: Boolean = false,
            val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
            val contrastLevel: Double = 0.0
        )

        @HiltViewModel
        class ThemeViewModel @Inject constructor(
            private val generateColorSchemeUseCase: GenerateColorSchemeUseCase,
            private val getColorSchemeParamsUseCase: GetColorSchemeParamsUseCase,
            private val saveColorSchemeParamsUseCase: SaveColorSchemeParamsUseCase
        ) : ViewModel() {

            private val _uiState = mutableStateOf(ColorSchemeUiState())
            val uiState: State<ColorSchemeUiState> = _uiState

            init {
                loadColorSchemeParams()
            }

            private fun loadColorSchemeParams() {
                viewModelScope.launch {
                    getColorSchemeParamsUseCase().collectLatest { params ->
                        _uiState.value = _uiState.value.copy(
                            seedColor = params.seedColor,
                            isDarkTheme = params.isDarkTheme,
                            paletteStyle = params.paletteStyle,
                            contrastLevel = params.contrastLevel
                        )
                        generateColorScheme()
                    }
                }
            }

            fun onSeedColorChanged(color: Long) {
                _uiState.value = _uiState.value.copy(seedColor = color)
                generateColorScheme()
                saveColorSchemeParams()
            }

            fun onIsDarkThemeChanged(isDark: Boolean) {
                _uiState.value = _uiState.value.copy(isDarkTheme = isDark)
                generateColorScheme()
                saveColorSchemeParams()
            }

            fun onPaletteStyleChanged(style: PaletteStyle) {
                _uiState.value = _uiState.value.copy(paletteStyle = style)
                generateColorScheme()
                saveColorSchemeParams()
            }

            fun onContrastLevelChanged(level: Double) {
                _uiState.value = _uiState.value.copy(contrastLevel = level)
                generateColorScheme()
                saveColorSchemeParams()
            }

            private fun generateColorScheme() {
                viewModelScope.launch {
                    generateColorSchemeUseCase(
                        CustomColorSchemeParams(
                            seedColor = _uiState.value.seedColor,
                            isDarkTheme = _uiState.value.isDarkTheme,
                            paletteStyle = _uiState.value.paletteStyle,
                            contrastLevel = _uiState.value.contrastLevel
                        )
                    ).collectLatest { generatedColorScheme ->
                        _uiState.value = _uiState.value.copy(colorScheme = generatedColorScheme.colorScheme)
                    }
                }
            }

            private fun saveColorSchemeParams() {
                viewModelScope.launch {
                    saveColorSchemeParamsUseCase(
                        CustomColorSchemeParams(
                            seedColor = _uiState.value.seedColor,
                            isDarkTheme = _uiState.value.isDarkTheme,
                            paletteStyle = _uiState.value.paletteStyle,
                            contrastLevel = _uiState.value.contrastLevel
                        )
                    )
                }
            }
        }
    }

    // UI (Compose Screens/Composables)
    package ui {
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.collectAsState
        import androidx.compose.runtime.getValue
        import androidx.hilt.navigation.compose.hiltViewModel
        import com.yourcompany.yourapp.presentation.viewmodel.ThemeViewModel
        import com.yourcompany.yourapp.domain.model.PaletteStyle

        @Composable
        fun ThemeSettingsScreen(
            viewModel: ThemeViewModel = hiltViewModel()
        ) {
            val uiState by viewModel.uiState.collectAsState()

            // UI elements to allow the user to modify:
            // - seedColor (e.g., a color picker)
            // - isDarkTheme (e.g., a switch)
            // - paletteStyle (e.g., a dropdown or radio buttons)
            // - contrastLevel (e.g., a slider)

            // Example UI elements (replace with your actual implementation):
            // ColorPicker(
            //     selectedColor = uiState.seedColor,
            //     onColorChanged = viewModel::onSeedColorChanged
            // )
            // Switch(
            //     checked = uiState.isDarkTheme,
            //     onCheckedChange = viewModel::onIsDarkThemeChanged
            // )
            // Dropdown(
            //     selectedItem = uiState.paletteStyle,
            //     items = PaletteStyle.values().toList(),
            //     onItemSelected = viewModel::onPaletteStyleChanged
            // )
            // Slider(
            //     value = uiState.contrastLevel.toFloat(),
            //     onValueChange = { viewModel.onContrastLevelChanged(it.toDouble()) }
            // )
        }

        @Composable
        fun MyAppTheme(
            darkTheme: Boolean = false, // Determine dark theme from state
            themeViewModel: ThemeViewModel = hiltViewModel(),
            content: @Composable () -> Unit
        ) {
            val uiState by themeViewModel.uiState.collectAsState()
            val isDark = uiState.isDarkTheme
            val colorScheme = uiState.colorScheme

            androidx.compose.material3.MaterialTheme(
                colorScheme = if (isDark) androidx.compose.material3.darkColorScheme().copy(
                    primary = colorScheme.primary,
                    onPrimary = colorScheme.onPrimary,
                    secondary = colorScheme.secondary,
                    onSecondary = colorScheme.onSecondary,
                    tertiary = colorScheme.tertiary,
                    onTertiary = colorScheme.onTertiary,
                    background = colorScheme.background,
                    onBackground = colorScheme.onBackground,
                    surface = colorScheme.surface,
                    onSurface = colorScheme.onSurface,
                    surfaceVariant = colorScheme.surfaceVariant,
                    onSurfaceVariant = colorScheme.onSurfaceVariant,
                    inverseSurface = colorScheme.inverseSurface,
                    inverseOnSurface = colorScheme.inverseOnSurface,
                    inversePrimary = colorScheme.inversePrimary,
                ) else androidx.compose.material3.lightColorScheme().copy(
                    primary = colorScheme.primary,
                    onPrimary = colorScheme.onPrimary,
                    secondary = colorScheme.secondary,
                    onSecondary = colorScheme.onSecondary,
                    tertiary = colorScheme.tertiary,
                    onTertiary = colorScheme.onTertiary,
                    background = colorScheme.background,
                    onBackground = colorScheme.onBackground,
                    surface = colorScheme.surface,
                    onSurface = colorScheme.onSurface,
                    surfaceVariant = colorScheme.surfaceVariant,
                    onSurfaceVariant = colorScheme.onSurfaceVariant,
                    inverseSurface = colorScheme.inverseSurface,
                    inverseOnSurface = colorScheme.inverseOnSurface,
                    inversePrimary = colorScheme.inversePrimary,
                ),
                content = content
            )
        }
    }
}

// di (Dependency Injection with Hilt)
package com.yourcompany.yourapp.di {
    import android.content.Context
    import androidx.datastore.core.DataStore
    import androidx.datastore.preferences.core.PreferenceDataStoreFactory
    import androidx.datastore.preferences.core.Preferences
    import androidx.datastore.preferences.preferencesDataStoreFile
    import com.yourcompany.yourapp.data.datastore.PreferencesDataSource
    import com.yourcompany.yourapp.data.datastore.PreferencesDataSourceImpl
    import com.yourcompany.yourapp.data.repository.ColorSchemeRepository
    import com.
 */

/*
fun Color.toULong(): ULong = value
fun ULong.toColor(): Color = Color(this)

fun PaletteStyle.toStorageValue(): String = name
fun String.toPaletteStyle(): PaletteStyle = PaletteStyle.valueOf(this)

com.yourapp.theme
├── data
│   ├── local
│   │   ├── datastore
│   │   │   ├── ThemePreferences.kt
│   │   │   ├── ThemePreferenceKeys.kt
│   │   │   └── ThemeDataStoreDataSource.kt
│   ├── repository
│   │   └── ThemeRepositoryImpl.kt
│
├── domain
│   ├── model
│   │   ├── ThemePreferencesModel.kt
│   │   └── PaletteStyle.kt
│   ├── repository
│   │   └── ThemeRepository.kt
│   └── usecase
│       ├── GenerateColorSchemeUseCase.kt
│       └── SaveThemePreferencesUseCase.kt
│       └── LoadThemePreferencesUseCase.kt
│
├── presentation
│   ├── theme
│   │   ├── ThemeViewModel.kt
│   │   └── ThemeUiState.kt
│
├── di
│   └── RepositoryModule.kt
│
└── util
    └── ColorExtensions.kt

package com.yourapp.theme.domain.model

import androidx.compose.ui.graphics.Color

data class ThemePreferencesModel(
    val seedColor: Color,
    val isDarkTheme: Boolean,
    val paletteStyle: PaletteStyle,
    val contrastLevel: Float
)

package com.yourapp.theme.domain.repository

import com.yourapp.theme.domain.model.ThemePreferencesModel
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getThemePreferences(): Flow<ThemePreferencesModel>
    suspend fun saveThemePreferences(preferences: ThemePreferencesModel)
}

package com.yourapp.theme.domain.usecase

import androidx.compose.material3.ColorScheme
import com.yourapp.theme.domain.model.ThemePreferencesModel

interface GenerateColorSchemeUseCase {
    operator fun invoke(preferences: ThemePreferencesModel): ColorScheme
}

package com.yourapp.theme.domain.usecase

import com.yourapp.theme.domain.model.ThemePreferencesModel
import kotlinx.coroutines.flow.Flow

interface SaveThemePreferencesUseCase {
    suspend operator fun invoke(preferences: ThemePreferencesModel)
}

interface LoadThemePreferencesUseCase {
    operator fun invoke(): Flow<ThemePreferencesModel>
}

package com.yourapp.theme.data.local.datastore

import kotlinx.coroutines.flow.Flow

interface ThemeDataStoreDataSource {
    fun getSeedColor(): Flow<ULong>
    fun getIsDarkTheme(): Flow<Boolean>
    fun getPaletteStyle(): Flow<String>
    fun getContrastLevel(): Flow<Float>

    suspend fun saveSeedColor(color: ULong)
    suspend fun saveIsDarkTheme(isDark: Boolean)
    suspend fun savePaletteStyle(style: String)
    suspend fun saveContrastLevel(level: Float)
}

package com.yourapp.theme.data.local.datastore

object ThemePreferenceKeys {
    val SEED_COLOR = preferencesKey<ULong>("seed_color")
    val IS_DARK_THEME = preferencesKey<Boolean>("is_dark_theme")
    val PALETTE_STYLE = preferencesKey<String>("palette_style")
    val CONTRAST_LEVEL = preferencesKey<Float>("contrast_level")
}

package com.yourapp.theme.data.repository

import com.yourapp.theme.domain.model.ThemePreferencesModel
import com.yourapp.theme.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow

class ThemeRepositoryImpl(
    private val dataSource: ThemeDataStoreDataSource
) : ThemeRepository {
    override fun getThemePreferences(): Flow<ThemePreferencesModel>
    override suspend fun saveThemePreferences(preferences: ThemePreferencesModel)
}

package com.yourapp.theme.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.theme.domain.model.ThemePreferencesModel
import com.yourapp.theme.domain.usecase.GenerateColorSchemeUseCase
import com.yourapp.theme.domain.usecase.LoadThemePreferencesUseCase
import com.yourapp.theme.domain.usecase.SaveThemePreferencesUseCase
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val generateColorScheme: GenerateColorSchemeUseCase,
    private val loadPreferences: LoadThemePreferencesUseCase,
    private val savePreferences: SaveThemePreferencesUseCase
) : ViewModel() {
    val uiState: StateFlow<ThemeUiState>

    fun updatePreferences(preferences: ThemePreferencesModel)
}

package com.yourapp.theme.presentation.theme

import androidx.compose.material3.ColorScheme
import com.yourapp.theme.domain.model.ThemePreferencesModel

data class ThemeUiState(
    val preferences: ThemePreferencesModel,
    val colorScheme: ColorScheme
)

package com.yourapp.theme.di

import com.yourapp.theme.domain.repository.ThemeRepository
import com.yourapp.theme.domain.usecase.*
import com.yourapp.theme.data.repository.ThemeRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ThemeModule {

    @Provides
    fun provideThemeRepository(...): ThemeRepository

    @Provides
    fun provideGenerateColorSchemeUseCase(): GenerateColorSchemeUseCase

    @Provides
    fun provideSaveThemePreferencesUseCase(repository: ThemeRepository): SaveThemePreferencesUseCase

    @Provides
    fun provideLoadThemePreferencesUseCase(repository: ThemeRepository): LoadThemePreferencesUseCase
}

package com.yourapp.theme.util

import androidx.compose.ui.graphics.Color

fun Color.toULong(): ULong = value
fun ULong.toColor(): Color = Color(this)

package com.yourapp.theme.domain.usecase

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.yourapp.theme.domain.model.ThemePreferencesModel
import com.yourapp.theme.domain.model.PaletteStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi

class GenerateColorSchemeUseCaseImpl : GenerateColorSchemeUseCase {

    override fun invoke(preferences: ThemePreferencesModel): ColorScheme {
        return generateDynamicColorScheme(
            seedColor = preferences.seedColor,
            isDarkTheme = preferences.isDarkTheme,
            paletteStyle = preferences.paletteStyle,
            contrastLevel = preferences.contrastLevel
        )
    }

    private fun generateDynamicColorScheme(
        seedColor: Color,
        isDarkTheme: Boolean,
        paletteStyle: PaletteStyle,
        contrastLevel: Float
    ): ColorScheme {
        // Integrate with Material3's open source color scheme generator here
        // Use paletteStyle to choose the appropriate style
        // Use contrastLevel to fine-tune the contrast
        // This part will depend on how you've integrated the Material3 open source code
        TODO("Implement using Material3 open source color generator")
    }
}

class ThemeDataStoreDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeDataStoreDataSource {

    private val dataStore = context.createDataStore(name = "theme_preferences")

    override fun getSeedColor(): Flow<ULong> = dataStore.data
        .map { it[ThemePreferenceKeys.SEED_COLOR] ?: DEFAULT_SEED_COLOR }

    override fun getIsDarkTheme(): Flow<Boolean> = dataStore.data
        .map { it[ThemePreferenceKeys.IS_DARK_THEME] ?: false }

    override fun getPaletteStyle(): Flow<String> = dataStore.data
        .map { it[ThemePreferenceKeys.PALETTE_STYLE] ?: PaletteStyle.TonalSpot.name }

    override fun getContrastLevel(): Flow<Float> = dataStore.data
        .map { it[ThemePreferenceKeys.CONTRAST_LEVEL] ?: 1.0f }

    override suspend fun saveSeedColor(color: ULong) {
        dataStore.edit { it[ThemePreferenceKeys.SEED_COLOR] = color }
    }

    override suspend fun saveIsDarkTheme(isDark: Boolean) {
        dataStore.edit { it[ThemePreferenceKeys.IS_DARK_THEME] = isDark }
    }

    override suspend fun savePaletteStyle(style: String) {
        dataStore.edit { it[ThemePreferenceKeys.PALETTE_STYLE] = style }
    }

    override suspend fun saveContrastLevel(level: Float) {
        dataStore.edit { it[ThemePreferenceKeys.CONTRAST_LEVEL] = level }
    }

    companion object {
        private const val DEFAULT_SEED_COLOR: ULong = 0xFF6200EEu // Example fallback
    }
}

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val generateColorScheme: GenerateColorSchemeUseCase,
    private val loadPreferences: LoadThemePreferencesUseCase,
    private val savePreferences: SaveThemePreferencesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeUiState.default())
    val uiState: StateFlow<ThemeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadPreferences().collect { prefs ->
                val scheme = generateColorScheme(prefs)
                _uiState.value = ThemeUiState(
                    preferences = prefs,
                    colorScheme = scheme
                )
            }
        }
    }

    fun updatePreferences(preferences: ThemePreferencesModel) {
        viewModelScope.launch {
            savePreferences(preferences)
        }
    }
}

data class ThemeUiState(
    val preferences: ThemePreferencesModel,
    val colorScheme: ColorScheme
) {
    companion object {
        fun default() = ThemeUiState(
            preferences = ThemePreferencesModel(
                seedColor = Color(0xFF6200EE),
                isDarkTheme = false,
                paletteStyle = PaletteStyle.TonalSpot,
                contrastLevel = 1.0f
            ),
            colorScheme = ColorScheme.light() // or custom fallback
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ThemeModule {

    @Provides
    fun provideThemeDataSource(
        @ApplicationContext context: Context
    ): ThemeDataStoreDataSource = ThemeDataStoreDataSourceImpl(context)

    @Provides
    fun provideThemeRepository(
        dataSource: ThemeDataStoreDataSource
    ): ThemeRepository = ThemeRepositoryImpl(dataSource)

    @Provides
    fun provideGenerateColorSchemeUseCase(): GenerateColorSchemeUseCase =
        GenerateColorSchemeUseCaseImpl()

    @Provides
    fun provideSaveThemePreferencesUseCase(
        repository: ThemeRepository
    ): SaveThemePreferencesUseCase = object : SaveThemePreferencesUseCase {
        override suspend fun invoke(preferences: ThemePreferencesModel) {
            repository.saveThemePreferences(preferences)
        }
    }

    @Provides
    fun provideLoadThemePreferencesUseCase(
        repository: ThemeRepository
    ): LoadThemePreferencesUseCase = object : LoadThemePreferencesUseCase {
        override fun invoke(): Flow<ThemePreferencesModel> =
            repository.getThemePreferences()
    }
}


 */


/*
// Saving to DataStore
val color: Color = // your color
val colorLong: Long = color.value.toLong()

// Retrieving from DataStore
val colorLong: Long = // value from DataStore
val color: Color = Color(colorLong)

// Saving to DataStore
val paletteStyle = PaletteStyle.TonalSpot
val paletteStyleKey = stringPreferencesKey("palette_style")
dataStore.edit { preferences ->
    preferences[paletteStyleKey] = paletteStyle.name
}

// Retrieving from DataStore
val paletteStyleFlow: Flow<PaletteStyle> = dataStore.data.map { preferences ->
    preferences[paletteStyleKey]?.let { PaletteStyle.valueOf(it) } ?: PaletteStyle.TonalSpot // Default value
}

package com.example.myapp.domain.model

import androidx.compose.ui.graphics.Color

// Domain model representing the theme configuration
data class ThemeConfig(
    val seedColor: Color,
    val isDarkTheme: Boolean,
    val paletteStyle: PaletteStyle,
    val contrastLevel: Double
)

package com.example.myapp.domain.model

// Enum representing the palette style options (provided by you)
enum class PaletteStyle {
    TonalSpot, Neutral, Vibrant, Rainbow, Expressive, FruitSalad, Monochrome, Fidelity, Content,
}

package com.example.myapp.domain.repository

import com.example.myapp.domain.model.PaletteStyle
import com.example.myapp.domain.model.ThemeConfig
import kotlinx.coroutines.flow.Flow

// Interface defining the contract for theme-related data operations
interface ThemeRepository {
    // Save the theme configuration
    suspend fun saveThemeConfig(themeConfig: ThemeConfig)

    // Retrieve the theme configuration as a Flow
    fun getThemeConfig(): Flow<ThemeConfig>
}

package com.example.myapp.domain.usecase

import androidx.compose.material3.ColorScheme
import com.example.myapp.domain.model.ThemeConfig

// UseCase responsible for generating the custom ColourScheme
class GenerateColourSchemeUseCase {
    // Generate the ColourScheme based on the provided ThemeConfig
    suspend operator fun invoke(themeConfig: ThemeConfig): ColorScheme
}
package com.example.myapp.domain.usecase

import com.example.myapp.domain.model.ThemeConfig
import com.example.myapp.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow

// UseCase responsible for retrieving the theme configuration
class GetThemeConfigUseCase(
    private val themeRepository: ThemeRepository
) {
    // Retrieve the theme configuration as a Flow
    operator fun invoke(): Flow<ThemeConfig>
}
package com.example.myapp.domain.usecase

import com.example.myapp.domain.model.ThemeConfig
import com.example.myapp.domain.repository.ThemeRepository

// UseCase responsible for saving the theme configuration
class SaveThemeConfigUseCase(
    private val themeRepository: ThemeRepository
) {
    // Save the theme configuration
    suspend operator fun invoke(themeConfig: ThemeConfig)
}
package com.example.myapp.data.datasource

import androidx.compose.ui.graphics.Color
import com.example.myapp.domain.model.PaletteStyle
import com.example.myapp.domain.model.ThemeConfig
import kotlinx.coroutines.flow.Flow

// Interface defining the contract for theme-related data storage
interface ThemeDataSource {
    // Save the theme configuration
    suspend fun saveThemeConfig(seedColor: Color, isDarkTheme: Boolean, paletteStyle: PaletteStyle, contrastLevel: Double)

    // Retrieve the theme configuration as a Flow
    fun getThemeConfig(): Flow<ThemeConfig>
}
package com.example.myapp.data.datasource

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import com.example.myapp.domain.model.PaletteStyle
import com.example.myapp.domain.model.ThemeConfig
import com.example.myapp.util.Constants
import com.example.myapp.util.toColor
import com.example.myapp.util.toLong
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Implementation of ThemeDataSource using DataStore Preferences
class ThemeDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeDataSource {

    private val dataStore: DataStore<Preferences> by lazy {
        context.createDataStore(name = Constants.DATASTORE_PREFERENCES_NAME)
    }

    private object PreferencesKeys {
        val SEED_COLOR = longPreferencesKey("seed_color")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val PALETTE_STYLE = stringPreferencesKey("palette_style")
        val CONTRAST_LEVEL = doublePreferencesKey("contrast_level")
    }

    // Save the theme configuration
    override suspend fun saveThemeConfig(
        seedColor: Color,
        isDarkTheme: Boolean,
        paletteStyle: PaletteStyle,
        contrastLevel: Double
    )

    // Retrieve the theme configuration as a Flow
    override fun getThemeConfig(): Flow<ThemeConfig>
}
package com.example.myapp.data.repository

import com.example.myapp.data.datasource.ThemeDataSource
import com.example.myapp.domain.model.ThemeConfig
import com.example.myapp.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Implementation of ThemeRepository
class ThemeRepositoryImpl @Inject constructor(
    private val themeDataSource: ThemeDataSource
) : ThemeRepository {

    // Save the theme configuration
    override suspend fun saveThemeConfig(themeConfig: ThemeConfig)

    // Retrieve the theme configuration as a Flow
    override fun getThemeConfig(): Flow<ThemeConfig>
}
 */