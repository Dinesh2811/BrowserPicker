package browserpicker.domain.usecases.uri.shared

import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.query.*
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.service.ParsedUri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

// Stub implementations for demonstration

class HandleUriUseCaseImpl @Inject constructor(): HandleUriUseCase {
    override suspend fun invoke(uriString: String, source: UriSource): DomainResult<HandleUriResult, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class ValidateUriUseCaseImpl @Inject constructor(): ValidateUriUseCase {
    override suspend fun invoke(uriString: String): DomainResult<ParsedUri?, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class RecordUriInteractionUseCaseImpl @Inject constructor(): RecordUriInteractionUseCase {
    override suspend fun invoke(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?
    ): DomainResult<Long, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class GetRecentUrisUseCaseImpl @Inject constructor(): GetRecentUrisUseCase {
    override operator fun invoke(limit: Int): Flow<DomainResult<List<ParsedUri>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class SearchUrisUseCaseImpl @Inject constructor(): SearchUrisUseCase {
    override operator fun invoke(query: String): Flow<DomainResult<List<ParsedUri>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class CleanupUriHistoryUseCaseImpl @Inject constructor(): CleanupUriHistoryUseCase {
    override suspend operator fun invoke(olderThanDays: Int): DomainResult<Int, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
} 