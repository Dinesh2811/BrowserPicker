package browserpicker.domain.usecase.history

import browserpicker.domain.model.query.FilterOptions
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.UriHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject

interface GetHistoryFilterOptionsUseCase {
    operator fun invoke(): Flow<FilterOptions>
}

@OptIn(ExperimentalCoroutinesApi::class)
class GetHistoryFilterOptionsUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val hostRuleRepository: HostRuleRepository,
) : GetHistoryFilterOptionsUseCase {
    override fun invoke(): Flow<FilterOptions> {
        Timber.d("Getting filter options...")
        return combine(
            uriHistoryRepository.getDistinctHosts().distinctUntilChanged().catch { emit(emptyList()) },
            hostRuleRepository.getDistinctRuleHosts().distinctUntilChanged().catch { emit(emptyList()) },
            uriHistoryRepository.getDistinctChosenBrowsers().distinctUntilChanged().catch { emit(emptyList()) }
        ) { historyHosts, ruleHosts, browsers ->
            FilterOptions(
                distinctHistoryHosts = historyHosts,
                distinctRuleHosts = ruleHosts,
                distinctChosenBrowsers = browsers
            )
        }
    }
}