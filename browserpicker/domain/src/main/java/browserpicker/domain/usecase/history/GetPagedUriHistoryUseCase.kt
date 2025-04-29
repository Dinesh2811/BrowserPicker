package browserpicker.domain.usecase.history

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.PagingDefaults
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

interface GetPagedUriHistoryUseCase {
    operator fun invoke(
        query: UriHistoryQuery = UriHistoryQuery.DEFAULT,
        pagingConfig: PagingConfig = PagingDefaults.DEFAULT_PAGING_CONFIG,
    ): Flow<PagingData<UriRecord>>
}

class GetPagedUriHistoryUseCaseImpl @Inject constructor(
    private val repository: UriHistoryRepository,
) : GetPagedUriHistoryUseCase {
    override fun invoke(
        query: UriHistoryQuery,
        pagingConfig: PagingConfig,
    ): Flow<PagingData<UriRecord>> {
        Timber.d("Getting paged URI history with query: $query")
        return repository.getPagedUriRecords(query, pagingConfig)
    }
}