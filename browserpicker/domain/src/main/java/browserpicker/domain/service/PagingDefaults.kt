package browserpicker.domain.service

import androidx.paging.PagingConfig

object PagingDefaults {
    val DEFAULT_PAGING_CONFIG = PagingConfig(
        pageSize = 30,
        prefetchDistance = 10,
        enablePlaceholders = false,
        initialLoadSize = 60
    )
}