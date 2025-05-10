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
    private val hostRuleRepository: HostRuleRepository
) : GetUriFilterOptionsUseCase {
    override fun invoke(): Flow<DomainResult<FilterOptions, AppError>> {
        return combine(
            uriHistoryRepository.getDistinctHosts(),
            hostRuleRepository.getDistinctRuleHosts(),
            uriHistoryRepository.getDistinctChosenBrowsers()
        ) { historyHostsResult, ruleHostsResult, chosenBrowsersResult ->
            when {
                historyHostsResult is DomainResult.Failure ->
                    DomainResult.Failure(historyHostsResult.error)
                ruleHostsResult is DomainResult.Failure ->
                    DomainResult.Failure(ruleHostsResult.error)
                chosenBrowsersResult is DomainResult.Failure ->
                    DomainResult.Failure(chosenBrowsersResult.error)
                else -> {
                    val historyHosts = (historyHostsResult as DomainResult.Success).data
                    val ruleHosts = (ruleHostsResult as DomainResult.Success).data
                    val chosenBrowsers = (chosenBrowsersResult as DomainResult.Success).data

                    listOf(historyHostsResult, chosenBrowsersResult, ruleHostsResult)
                        .firstOrNull { it.isFailure }?.let {
                            @Suppress("UNCHECKED_CAST")
                            return@combine it as DomainResult<FilterOptions, AppError> // Propagate failure
                        }

                    DomainResult.Success(FilterOptions(
                        distinctHistoryHosts = historyHosts,
                        distinctRuleHosts = ruleHosts,
                        distinctChosenBrowsers = chosenBrowsers
                    ))
                }
            }
        }
    }
}
