package browserpicker.domain.usecases.impl

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.*
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.query.*
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.usecases.uri.DeleteAllUriHistoryUseCase
import browserpicker.domain.usecases.uri.DeleteUriRecordUseCase
import browserpicker.domain.usecases.uri.ExportUriHistoryUseCase
import browserpicker.domain.usecases.uri.GetPagedUriHistoryUseCase
import browserpicker.domain.usecases.uri.GetUriFilterOptionsUseCase
import browserpicker.domain.usecases.uri.GetUriHistoryCountUseCase
import browserpicker.domain.usecases.uri.GetUriHistoryDateCountsUseCase
import browserpicker.domain.usecases.uri.GetUriHistoryGroupCountsUseCase
import browserpicker.domain.usecases.uri.GetUriRecordByIdUseCase
import browserpicker.domain.usecases.uri.ImportUriHistoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

// Stub implementations for demonstration

class GetPagedUriHistoryUseCaseImpl @Inject constructor(): GetPagedUriHistoryUseCase {
    override operator fun invoke(
        query: UriHistoryQuery,
        pagingConfig: PagingConfig
    ): Flow<PagingData<UriRecord>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetUriHistoryCountUseCaseImpl @Inject constructor(): GetUriHistoryCountUseCase {
    override operator fun invoke(query: UriHistoryQuery): Flow<DomainResult<Long, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetUriHistoryGroupCountsUseCaseImpl @Inject constructor(): GetUriHistoryGroupCountsUseCase {
    override operator fun invoke(query: UriHistoryQuery): Flow<DomainResult<List<GroupCount>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetUriHistoryDateCountsUseCaseImpl @Inject constructor(): GetUriHistoryDateCountsUseCase {
    override operator fun invoke(query: UriHistoryQuery): Flow<DomainResult<List<DateCount>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetUriRecordByIdUseCaseImpl @Inject constructor(): GetUriRecordByIdUseCase {
    override suspend operator fun invoke(id: Long): DomainResult<UriRecord?, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class DeleteUriRecordUseCaseImpl @Inject constructor(): DeleteUriRecordUseCase {
    override suspend operator fun invoke(id: Long): DomainResult<Unit, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class DeleteAllUriHistoryUseCaseImpl @Inject constructor(): DeleteAllUriHistoryUseCase {
    override suspend operator fun invoke(): DomainResult<Int, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class GetUriFilterOptionsUseCaseImpl @Inject constructor(): GetUriFilterOptionsUseCase {
    override operator fun invoke(): Flow<DomainResult<FilterOptions, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class ExportUriHistoryUseCaseImpl @Inject constructor(): ExportUriHistoryUseCase {
    override suspend operator fun invoke(filePath: String, query: UriHistoryQuery): DomainResult<Int, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class ImportUriHistoryUseCaseImpl @Inject constructor(): ImportUriHistoryUseCase {
    override suspend operator fun invoke(filePath: String): DomainResult<Int, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}
//
//class GetDistinctHistoryHostsUseCaseImpl @Inject constructor(
//    private val uriHistoryRepository: UriHistoryRepository
//) : GetDistinctHistoryHostsUseCase {
//    override operator fun invoke(): Flow<DomainResult<List<String>, AppError>> {
//        return uriHistoryRepository.getDistinctHosts()
//            .catchUnexpected()
//    }
//}
//
//class GetDistinctChosenBrowsersUseCaseImpl @Inject constructor(
//    private val uriHistoryRepository: UriHistoryRepository
//) : GetDistinctChosenBrowsersUseCase {
//    override operator fun invoke(): Flow<DomainResult<List<String?>, AppError>> {
//        return uriHistoryRepository.getDistinctChosenBrowsers()
//            .catchUnexpected()
//    }
//}