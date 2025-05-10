package browserpicker.presentation

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import browserpicker.core.results.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import browserpicker.domain.di.*
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import kotlinx.coroutines.flow.Flow

// Note: In a real application, you would typically inject only the specific
// UseCase factory interfaces needed by this ViewModel, rather than all of them.
// This example injects all for demonstration purposes.
@HiltViewModel
class MainViewModel @Inject constructor(
    private val uriHandlingUseCases: UriHandlingUseCases,
    private val browserUseCases: BrowserUseCases,
    private val hostRuleUseCases: HostRuleUseCases,
    private val uriHistoryUseCases: UriHistoryUseCases,
    private val folderUseCases: FolderUseCases,
    private val searchAndAnalyticsUseCases: SearchAndAnalyticsUseCases,
    private val systemIntegrationUseCases: SystemIntegrationUseCases,
) : ViewModel() {

    // Example functions demonstrating how to access and use the injected UseCases

    suspend fun handleIncomingUri(uriString: String, source: UriSource): DomainResult<HandleUriResult, AppError> {
        return uriHandlingUseCases.handleUriUseCase(uriString, source)
    }

    fun getAvailableBrowsers(): Flow<DomainResult<List<BrowserAppInfo>, AppError>> {
        return browserUseCases.getAvailableBrowsersUseCase()
    }

    fun getPagedHistory(query: UriHistoryQuery): Flow<PagingData<UriRecord>> {
        return uriHistoryUseCases.getPagedUriHistoryUseCase(query)
    }

    suspend fun createNewFolder(name: String, parentFolderId: Long?, type: FolderType): DomainResult<Long, AppError> {
        return folderUseCases.createFolderUseCase(name, parentFolderId, type)
    }

    fun getMostVisitedHosts(): Flow<DomainResult<List<GroupCount>, AppError>> {
        return searchAndAnalyticsUseCases.getMostVisitedHostsUseCase()
    }

    fun checkDefaultBrowserStatus(): Flow<DomainResult<Boolean, AppError>> {
        return systemIntegrationUseCases.checkDefaultBrowserStatusUseCase()
    }

    // Add more functions to expose other use cases as needed by the UI
}
