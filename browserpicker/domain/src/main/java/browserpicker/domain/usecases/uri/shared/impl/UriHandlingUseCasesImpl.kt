package browserpicker.domain.usecases.uri.shared.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.UriParser
import browserpicker.domain.usecases.uri.shared.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject

class ValidateUriUseCaseImpl @Inject constructor(
    private val uriParser: UriParser
) : ValidateUriUseCase {
    override suspend fun invoke(uriString: String): DomainResult<ParsedUri?, AppError> {
        if (uriString.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("URI string cannot be blank"))
        }

        return uriParser.parseAndValidateWebUri(uriString)
    }
}

class HandleUriUseCaseImpl @Inject constructor(
    private val validateUriUseCase: ValidateUriUseCase,
    private val hostRuleRepository: HostRuleRepository
) : HandleUriUseCase {
    override suspend fun invoke(uriString: String, source: UriSource): DomainResult<HandleUriResult, AppError> {
        // First validate the URI
        val validationResult = validateUriUseCase(uriString)
        if (validationResult is DomainResult.Failure) {
            return DomainResult.Success(HandleUriResult.InvalidUri("Invalid URI: ${validationResult.error.message}"))
        }

        val parsedUri = (validationResult as DomainResult.Success).data
            ?: return DomainResult.Success(HandleUriResult.InvalidUri("URI could not be parsed"))

        // Check if there's a rule for this host
        val hostRuleResult = hostRuleRepository.getHostRuleByHost(parsedUri.host)
        if (hostRuleResult is DomainResult.Failure) {
            return DomainResult.Failure(hostRuleResult.error)
        }

        val hostRule = (hostRuleResult as DomainResult.Success).data

        // If no rule exists or status is NONE, show the picker
        if (hostRule == null || hostRule.uriStatus == UriStatus.NONE) {
            return DomainResult.Success(
                HandleUriResult.ShowPicker(
                    uriString = uriString,
                    host = parsedUri.host,
                    hostRuleId = hostRule?.id
                )
            )
        }

        // If host is blocked, return blocked result
        if (hostRule.uriStatus == UriStatus.BLOCKED) {
            return DomainResult.Success(HandleUriResult.Blocked)
        }

        // If host is bookmarked with a preferred browser and preference is enabled,
        // open directly in that browser
        if (hostRule.uriStatus == UriStatus.BOOKMARKED &&
            hostRule.preferredBrowserPackage != null &&
            hostRule.isPreferenceEnabled) {
            return DomainResult.Success(
                HandleUriResult.OpenDirectly(
                    browserPackageName = hostRule.preferredBrowserPackage,
                    hostRuleId = hostRule.id
                )
            )
        }

        // Otherwise, show the picker
        return DomainResult.Success(
            HandleUriResult.ShowPicker(
                uriString = uriString,
                host = parsedUri.host,
                hostRuleId = hostRule.id
            )
        )
    }
}

class RecordUriInteractionUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository
) : RecordUriInteractionUseCase {
    override suspend fun invoke(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?
    ): DomainResult<Long, AppError> {
        // Validate inputs
        if (uriString.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("URI string cannot be blank"))
        }

        if (host.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Host cannot be blank"))
        }

        if (source == UriSource.UNKNOWN) {
            return DomainResult.Failure(AppError.ValidationError("Source cannot be UNKNOWN"))
        }

        if (action == InteractionAction.UNKNOWN) {
            return DomainResult.Failure(AppError.ValidationError("Action cannot be UNKNOWN"))
        }

        if (chosenBrowser != null && chosenBrowser.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Chosen browser cannot be blank if provided"))
        }

        return uriHistoryRepository.addUriRecord(
            uriString = uriString,
            host = host,
            source = source,
            action = action,
            chosenBrowser = chosenBrowser,
            associatedHostRuleId = associatedHostRuleId
        )
    }
}

class GetRecentUrisUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository
) : GetRecentUrisUseCase {
    override fun invoke(limit: Int): Flow<DomainResult<List<UriRecord>, AppError>> {
        if (limit <= 0) {
            return flow {
                emit(DomainResult.Failure(AppError.ValidationError("Limit must be greater than zero")))
            }
        }

        // Create a query to get recent URIs
        val query = UriHistoryQuery(
            sortBy = UriRecordSortField.TIMESTAMP,
            sortOrder = SortOrder.DESC
        )

        // Use the repository to get paged URI records
        // This is a simplification as we can't easily convert PagingData to a simple List
        // In a real implementation, the repository should provide a method for this specific use case
        return uriHistoryRepository.getTotalUriRecordCount(query).map { countResult ->
            // Placeholder implementation - in a real app, you would need to implement
            // a repository method that returns a Flow<DomainResult<List<UriRecord>, AppError>>
            // for the most recent records
            DomainResult.Success(emptyList())
        }
    }
}

class CleanupUriHistoryUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository
) : CleanupUriHistoryUseCase {
    override suspend fun invoke(olderThanDays: Int): DomainResult<Int, AppError> {
        if (olderThanDays <= 0) {
            return DomainResult.Failure(AppError.ValidationError("Days must be greater than zero"))
        }

        // This is a simplified implementation
        // In a real implementation, the repository would need a method to delete records older than a certain date
        // For now, we just return a success with 0 records deleted
        return DomainResult.Success(0)
    }
}
