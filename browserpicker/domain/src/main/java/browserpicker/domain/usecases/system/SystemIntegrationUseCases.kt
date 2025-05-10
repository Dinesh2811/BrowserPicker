package browserpicker.domain.usecases.system

import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import kotlinx.coroutines.flow.Flow

interface CheckDefaultBrowserStatusUseCase {
    /**
     * Checks if this app is set as the default browser
     */
    operator fun invoke(): Flow<DomainResult<Boolean, AppError>>
}

interface OpenBrowserPreferencesUseCase {
    /**
     * Opens system settings to change default browser
     */
    suspend operator fun invoke(): DomainResult<Unit, AppError>
}

interface MonitorUriClipboardUseCase {
    /**
     * Monitors the clipboard for web URIs
     */
    operator fun invoke(): Flow<DomainResult<String, AppError>>
}

interface ShareUriUseCase {
    /**
     * Shares a URI with another app
     */
    suspend operator fun invoke(uriString: String): DomainResult<Unit, AppError>
}

interface OpenUriInBrowserUseCase {
    /**
     * Opens a URI in the specified browser
     */
    suspend operator fun invoke(uriString: String, browserPackageName: String): DomainResult<Unit, AppError>
}

interface SetAsDefaultBrowserUseCase {
    /**
     * Attempts to set this app as the default browser
     */
    suspend operator fun invoke(): DomainResult<Boolean, AppError>
}

interface BackupDataUseCase {
    /**
     * Backs up app data to the specified file
     */
    suspend operator fun invoke(filePath: String, includeHistory: Boolean = true): DomainResult<Unit, AppError>
}

interface RestoreDataUseCase {
    /**
     * Restores app data from the specified file
     */
    suspend operator fun invoke(filePath: String, clearExistingData: Boolean = false): DomainResult<Unit, AppError>
}

interface MonitorSystemBrowserChangesUseCase {
    /**
     * Monitors for changes to installed browsers on the device
     */
    operator fun invoke(): Flow<DomainResult<List<String>, AppError>>
}

interface HandleUncaughtUriUseCase {
    /**
     * Processes URIs not caught by intent filters (e.g. from share intents)
     */
    suspend operator fun invoke(data: String): DomainResult<Boolean, AppError>
} 