package browserpicker.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.usecases.system.CheckDefaultBrowserStatusUseCase
import browserpicker.domain.usecases.system.OpenBrowserPreferencesUseCase
import browserpicker.domain.usecases.system.SetAsDefaultBrowserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Onboarding screen.
 *
 * This ViewModel handles:
 * - First-time user setup
 * - Default browser setup
 * - Permission requests
 * - Tutorial display
 *
 * Used by: OnboardingScreen
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
//    private val getInitialSetupStatusUseCase: GetInitialSetupStatusUseCase,
    private val checkDefaultBrowserStatusUseCase: CheckDefaultBrowserStatusUseCase,
    private val setAsDefaultBrowserUseCase: SetAsDefaultBrowserUseCase,
    private val openBrowserPreferencesUseCase: OpenBrowserPreferencesUseCase,
//    private val setupCompleteUseCase: SetupCompleteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        checkInitialSetup()
        checkDefaultBrowserStatus()
    }

    /**
     * Check if initial setup has been completed
     */
    private fun checkInitialSetup() {
//        viewModelScope.launch {
//            val result = getInitialSetupStatusUseCase()
//
//            result.onSuccess { isCompleted ->
//                _uiState.value = _uiState.value.copy(
//                    hasCompletedSetup = isCompleted
//                )
//            }
//        }
    }

    /**
     * Check if this app is set as the default browser
     */
    private fun checkDefaultBrowserStatus() {
        viewModelScope.launch {
            checkDefaultBrowserStatusUseCase()
                .collect { result ->
                    result.onSuccess { isDefault ->
                        _uiState.value = _uiState.value.copy(
                            isDefaultBrowser = isDefault
                        )
                    }
                }
        }
    }

    /**
     * Set this app as the default browser
     */
    fun setAsDefaultBrowser() {
        viewModelScope.launch {
            val result = setAsDefaultBrowserUseCase()

            result.onSuccess { isSet ->
                _uiState.value = _uiState.value.copy(
                    isDefaultBrowser = isSet
                )
            }
        }
    }

    /**
     * Open system browser preferences
     */
    fun openBrowserPreferences() {
        viewModelScope.launch {
            openBrowserPreferencesUseCase()
        }
    }

    /**
     * Mark onboarding as complete
     */
    fun completeSetup() {
//        viewModelScope.launch {
//            val result = setupCompleteUseCase()
//
//            result.onSuccess { isCompleted ->
//                _uiState.value = _uiState.value.copy(
//                    hasCompletedSetup = isCompleted,
//                    setupComplete = true
//                )
//            }
//        }
    }

    /**
     * Navigate to the next onboarding step
     */
    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep < OnboardingStep.values().size - 1) {
            _uiState.value = _uiState.value.copy(
                currentStep = currentStep + 1
            )
        } else {
            completeSetup()
        }
    }

    /**
     * Navigate to the previous onboarding step
     */
    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep > 0) {
            _uiState.value = _uiState.value.copy(
                currentStep = currentStep - 1
            )
        }
    }

    /**
     * Skip onboarding
     */
    fun skipOnboarding() {
        completeSetup()
    }
}

/**
 * UI state for the Onboarding screen
 */
data class OnboardingUiState(
    val currentStep: Int = 0,
    val isDefaultBrowser: Boolean = false,
    val hasCompletedSetup: Boolean = false,
    val setupComplete: Boolean = false
)

/**
 * Onboarding steps
 */
enum class OnboardingStep {
    WELCOME,
    DEFAULT_BROWSER,
    BROWSER_SELECTION,
    FOLDERS,
    COMPLETE
}
