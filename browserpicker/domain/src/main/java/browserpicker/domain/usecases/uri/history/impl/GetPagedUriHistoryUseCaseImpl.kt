package browserpicker.domain.usecases.uri.history.impl

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.GetPagedUriHistoryUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPagedUriHistoryUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
): GetPagedUriHistoryUseCase {
    override operator fun invoke(
        query: UriHistoryQuery,
        pagingConfig: PagingConfig,
    ): Flow<PagingData<UriRecord>> {
        return uriHistoryRepository.getPagedUriRecords(query, pagingConfig)
    }
}
