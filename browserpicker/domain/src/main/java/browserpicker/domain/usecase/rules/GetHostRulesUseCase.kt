package browserpicker.domain.usecase.rules

import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.repository.HostRuleRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

interface GetHostRulesUseCase {
    // Could add filters/sorting later if needed
    operator fun invoke(
        statusFilter: UriStatus? = null,
        folderFilter: Long? = null,
        rootOnlyForStatus: UriStatus? = null
    ): Flow<List<HostRule>>
}

class GetHostRulesUseCaseImpl @Inject constructor(
    private val repository: HostRuleRepository
) : GetHostRulesUseCase {
    override fun invoke(
        statusFilter: UriStatus?,
        folderFilter: Long?,
        rootOnlyForStatus: UriStatus?
    ): Flow<List<HostRule>> {
        Timber.d("Getting host rules with filters: status=$statusFilter, folder=$folderFilter, rootOnly=$rootOnlyForStatus")
        return when {
            rootOnlyForStatus != null -> repository.getRootHostRulesByStatus(rootOnlyForStatus)
            folderFilter != null -> repository.getHostRulesByFolder(folderFilter)
            statusFilter != null -> repository.getHostRulesByStatus(statusFilter)
            else -> repository.getAllHostRules()
        }
    }
}
