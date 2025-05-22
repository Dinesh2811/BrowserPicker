package com.dinesh.browserpicker.testing1

import android.net.Uri
import browserpicker.core.results.AppError
import browserpicker.domain.model.*
import browserpicker.domain.service.*

data class UriProcessingResult(
    val parsedUri: ParsedUri,
    val uriSource: UriSource,
    val effectivePreference: UriRecord? = null,
    val isBlocked: Boolean = false,
    val alwaysOpenBrowserPackage: String? = null,
    val isBookmarked: Boolean = false,
    val hostRule: HostRule? = null,
)

sealed interface UiError: AppError
sealed interface UiResult<out T, out E: UiError> {
    data object Loading: UiResult<Nothing, Nothing>
    data object Idle: UiResult<Nothing, Nothing>
    data class Success<T>(val data: T): UiResult<T, Nothing>
    data class Error<E: UiError>(val error: E): UiResult<Nothing, E>
    data object Blocked: UiResult<Nothing, Nothing>
}

sealed interface PersistentError: UiError {
    sealed interface InstalledBrowserApps: PersistentError {
        data class Empty(override val message: String = "No installed browser apps found"): InstalledBrowserApps
        data class LoadFailed(override val message: String = "Failed to load installed browser apps", override val cause: Throwable? = null): InstalledBrowserApps
        data class UnknownError(override val message: String = "An unknown error occurred while loading installed browser apps", override val cause: Throwable): InstalledBrowserApps
    }

    data class HostRuleAccessFailed(override val message: String, override val cause: Throwable? = null): PersistentError
    // Add more when needed...
    data class FolderAccessFailed(override val message: String, override val cause: Throwable? = null): PersistentError // Added

    companion object {
        fun uiErrorState(uiState: PersistentError): UiResult.Error<UiError> {
            return UiResult.Error(uiState)
        }
    }
}

enum class TransientError(override val message: String): UiError {
    NULL_OR_EMPTY_URL("URL cannot be empty"),
    NO_BROWSER_SELECTED("Please select a browser first"),
    INVALID_URL_FORMAT("Invalid URL format"),
    LAUNCH_FAILED("Failed to launch browser"),
    SELECTION_REQUIRED("Please select a browser first"),
    HOST_RULE_ACCESS_FAILED("Failed to fetch host rule"),
    UNEXPECTED_ERROR_PROCESSING_URI("Unexpected error processing URI"),
    // Add more when needed...
}

sealed interface BrowserPickerUiEffect {
    data object AutoOpenBrowser: BrowserPickerUiEffect
    data object BrowserAppsLoaded: BrowserPickerUiEffect
    data object SettingsSaved: BrowserPickerUiEffect
    data object UriBookmarked: BrowserPickerUiEffect
    data object UriBlocked: BrowserPickerUiEffect
    data class UriOpenedOnce(val packageName: String): BrowserPickerUiEffect
    // Add more when needed...
}

data class BrowserPickerUiState(
    val allAvailableBrowsers: List<BrowserAppInfo> = emptyList(),
    val selectedBrowserAppInfo: BrowserAppInfo? = null,
    val uiResult: UiResult<BrowserPickerUiEffect, UiError> = UiResult.Idle,
    val searchQuery: String = "",
    val uri: Uri? = null,
    val uriProcessingResult: UriProcessingResult? = null,
    // Add more when needed...
)


//sealed interface BrowserPickerUiEffect {
//    data object AutoOpenBrowser: BrowserPickerUiEffect
//    data object BrowserAppsLoaded: BrowserPickerUiEffect
//    data class OpenAndSavePreference(val packageName: String, val host: String): BrowserPickerUiEffect // Added host for potential snackbar message
//    data class UriBookmarkChanged(val host: String, val isBookmarked: Boolean): BrowserPickerUiEffect // More specific for bookmark
//    data class UriBlockStatusChanged(val host: String, val isBlocked: Boolean): BrowserPickerUiEffect // More specific for block
//    data class UriOpenedOnce(val packageName: String, val host: String): BrowserPickerUiEffect // Added host for potential snackbar/logging
//    // Add more when needed...
//}

//sealed interface PersistentError: UiError {
//    sealed interface InstalledBrowserApps: PersistentError {
//        data class Empty(override val message: String = "No installed browser apps found"): InstalledBrowserApps
//        data class LoadFailed(override val message: String = "Failed to load installed browser apps", override val cause: Throwable? = null): InstalledBrowserApps
//        data class UnknownError(override val message: String = "An unknown error occurred while loading installed browser apps", override val cause: Throwable): InstalledBrowserApps
//    }
//
//    data class HostRuleAccessFailed(override val message: String, override val cause: Throwable? = null): PersistentError
//    data class FolderAccessFailed(override val message: String, override val cause: Throwable? = null): PersistentError // Added
//    data class UriHistoryRecordFailed(override val message: String, override val cause: Throwable? = null): PersistentError // Added
//    data class BrowserStatsRecordFailed(override val message: String, override val cause: Throwable? = null): PersistentError // Added
//
//    companion object {
//        fun uiErrorState(uiState: PersistentError): UiResult.Error<UiError> {
//            return UiResult.Error(uiState)
//        }
//    }
//}

sealed interface FolderError: UiError { // Changed to UiError from AppError
    data class DefaultFolderNotFound(override val message: String): FolderError
    data class FolderAccessFailed(override val message: String, override val cause: Throwable? = null): FolderError
}