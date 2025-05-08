package com.dinesh.browserpicker.v1.domain.usecases

import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.service.ParsedUri
import kotlinx.coroutines.flow.Flow

interface HandleUriUseCase {
    /**
     * Processes an incoming URI and determines how it should be handled:
     * blocked, opened directly in a preferred browser, or shown in picker
     */
    suspend operator fun invoke(uriString: String, source: UriSource): DomainResult<HandleUriResult, AppError>
}

interface ValidateUriUseCase {
    /**
     * Validates and parses a URI string to ensure it's a valid web URI
     */
    suspend operator fun invoke(uriString: String): DomainResult<ParsedUri?, AppError>
}

interface RecordUriInteractionUseCase {
    /**
     * Records a user interaction with a URI (opening, dismissing, etc.)
     */
    suspend operator fun invoke(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?
    ): DomainResult<Long, AppError>
}

interface GetRecentUrisUseCase {
    /**
     * Gets a flow of recently handled URIs
     */
    operator fun invoke(limit: Int = 10): Flow<DomainResult<List<ParsedUri>, AppError>>
}

interface SearchUrisUseCase {
    /**
     * Searches for URIs by query string
     */
    operator fun invoke(query: String): Flow<DomainResult<List<ParsedUri>, AppError>>
}

interface CleanupUriHistoryUseCase {
    /**
     * Removes URIs older than the specified number of days
     */
    suspend operator fun invoke(olderThanDays: Int): DomainResult<Int, AppError>
} 