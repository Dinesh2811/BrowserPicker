package browserpicker.data.local.repository

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.SystemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemRepositoryImpl @Inject constructor(
): SystemRepository {
    override fun isDefaultBrowser(): Flow<DomainResult<Boolean, AppError>> {
        TODO("Not yet implemented")
    }

    override suspend fun openBrowserSettings(): DomainResult<Unit, AppError> {
        TODO("Not yet implemented")
    }

    override fun getClipboardUriFlow(): Flow<DomainResult<String, AppError>> {
        TODO("Not yet implemented")
    }

    override suspend fun shareUri(uriString: String): DomainResult<Unit, AppError> {
        TODO("Not yet implemented")
    }

    override suspend fun openUriInExternalBrowser(uriString: String, browserPackageName: String): DomainResult<Unit, AppError> {
        TODO("Not yet implemented")
    }

    override suspend fun requestSetDefaultBrowser(): DomainResult<Boolean, AppError> {
        TODO("Not yet implemented")
    }

    override suspend fun writeBackupFile(filePath: String, backupJsonString: String): DomainResult<Unit, AppError> {
        TODO("Not yet implemented")
    }

    override suspend fun readBackupFile(filePath: String): DomainResult<String, AppError> {
        TODO("Not yet implemented")
    }

    override fun getSystemBrowserChangesFlow(): Flow<DomainResult<List<String>, AppError>> {
        TODO("Not yet implemented")
    }

}
