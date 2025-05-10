package browserpicker.domain.repository

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import kotlinx.coroutines.flow.Flow

interface SystemRepository {
    /**
     * Checks if this app is set as the default browser.
     */
    fun isDefaultBrowser(): Flow<DomainResult<Boolean, AppError>>

    /**
     * Opens system settings to change default browser.
     */
    suspend fun openBrowserSettings(): DomainResult<Unit, AppError>

    /**
     * Monitors the clipboard for web URIs.
     * Emits the URI string when a new one is detected.
     */
    fun getClipboardUriFlow(): Flow<DomainResult<String, AppError>>

    /**
     * Shares a URI with another app using the system share sheet.
     */
    suspend fun shareUri(uriString: String): DomainResult<Unit, AppError>

    /**
     * Opens a URI in the specified browser package.
     */
    suspend fun openUriInExternalBrowser(uriString: String, browserPackageName: String): DomainResult<Unit, AppError>

    /**
     * Attempts to prompt the user to set this app as the default browser.
     * Returns true if the request was successfully initiated or if the app is already default.
     */
    suspend fun requestSetDefaultBrowser(): DomainResult<Boolean, AppError>

    /**
     * Writes the provided JSON string data to a backup file at the specified path.
     */
    suspend fun writeBackupFile(filePath: String, backupJsonString: String): DomainResult<Unit, AppError>

    /**
     * Reads the content of a backup file from the specified path as a JSON string.
     */
    suspend fun readBackupFile(filePath: String): DomainResult<String, AppError>

    /**
     * Monitors for changes to installed browsers on the device.
     * Emits a list of package names of currently installed browsers.
     */
    fun getSystemBrowserChangesFlow(): Flow<DomainResult<List<String>, AppError>>
}
