package browserpicker.presentation.stats

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.query.BrowserStatSortField
import browserpicker.presentation.common.LoadingStatus
import browserpicker.presentation.common.UserMessage

@Immutable
data class StatsScreenState(
    val isLoading: Boolean = false,
    val stats: List<BrowserUsageStat> = emptyList(),
    val sortField: BrowserStatSortField = BrowserStatSortField.USAGE_COUNT,
    val userMessages: List<UserMessage> = emptyList()
)
