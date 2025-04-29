package browserpicker.domain.usecase.stats

import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.query.BrowserStatSortField
import browserpicker.domain.repository.BrowserStatsRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import javax.inject.Inject

interface GetBrowserStatsUseCase {
    operator fun invoke(sortBy: BrowserStatSortField = BrowserStatSortField.USAGE_COUNT): Flow<List<BrowserUsageStat>>
}

class GetBrowserStatsUseCaseImpl @Inject constructor(
    private val repository: BrowserStatsRepository,
): GetBrowserStatsUseCase {
    override fun invoke(sortBy: BrowserStatSortField): Flow<List<BrowserUsageStat>> {
        Timber.d("Getting browser stats sorted by: $sortBy")
        return (
                if (sortBy == BrowserStatSortField.LAST_USED_TIMESTAMP) {
                    repository.getAllBrowserStatsSortedByLastUsed()
                } else {
                    repository.getAllBrowserStats() // Default sort by count in repo
                }
                ).catch { e ->
                Timber.e(e, "Error getting browser stats")
                emit(emptyList()) // Emit empty list on error
            }
    }
}
