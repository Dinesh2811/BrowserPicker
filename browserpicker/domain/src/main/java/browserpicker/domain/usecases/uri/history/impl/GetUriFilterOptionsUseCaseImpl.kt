package browserpicker.domain.usecases.uri.history.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.query.FilterOptions
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.GetUriFilterOptionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetUriFilterOptionsUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val hostRuleRepository: HostRuleRepository,
): GetUriFilterOptionsUseCase {
    override operator fun invoke(): Flow<DomainResult<FilterOptions, AppError>> {
        return combine(
            uriHistoryRepository.getDistinctHosts(),
            uriHistoryRepository.getDistinctChosenBrowsers(),
            hostRuleRepository.getDistinctRuleHosts()
        ) { historyHostsResult, chosenBrowsersResult, ruleHostsResult ->
            val historyHosts = historyHostsResult.getOrNull() ?: emptyList()
            val chosenBrowsers = chosenBrowsersResult.getOrNull() ?: emptyList()
            val ruleHosts = ruleHostsResult.getOrNull() ?: emptyList()

            // If any of the results failed, propagate the first encountered error.
            // A more sophisticated error handling could combine errors if needed.
            listOf(historyHostsResult, chosenBrowsersResult, ruleHostsResult)
                .firstOrNull { it.isFailure }?.let {
                    @Suppress("UNCHECKED_CAST")
                    return@combine it as DomainResult<FilterOptions, AppError> // Propagate failure
                }

            DomainResult.Success(
                FilterOptions(
                    distinctHistoryHosts = historyHosts,
                    distinctChosenBrowsers = chosenBrowsers,
                    distinctRuleHosts = ruleHosts
                )
            )
        }
    }
}