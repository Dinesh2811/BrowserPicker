package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable

@Immutable
enum class BrowserStatSortField {
    USAGE_COUNT, LAST_USED_TIMESTAMP
}

@Immutable
data class FilterOptions(
    val distinctHistoryHosts: List<String> = emptyList(),
    val distinctRuleHosts: List<String> = emptyList(),
    val distinctChosenBrowsers: List<String?> = emptyList(),
)

@Immutable
sealed interface HandleUriResult {
    @Immutable
    data object Blocked: HandleUriResult
    @Immutable
    data class OpenDirectly(val browserPackageName: String, val hostRuleId: Long?): HandleUriResult
    @Immutable
    data class ShowPicker(val uriString: String, val host: String, val hostRuleId: Long?): HandleUriResult
    @Immutable
    data class InvalidUri(val reason: String): HandleUriResult
}
