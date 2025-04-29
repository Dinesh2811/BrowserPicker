package browserpicker.domain.model.query

enum class BrowserStatSortField {
    USAGE_COUNT, LAST_USED_TIMESTAMP
}

data class FilterOptions(
    val distinctHistoryHosts: List<String> = emptyList(),
    val distinctRuleHosts: List<String> = emptyList(),
    val distinctChosenBrowsers: List<String?> = emptyList(),
)

sealed interface HandleUriResult {
    data object Blocked: HandleUriResult
    data class OpenDirectly(val browserPackageName: String, val hostRuleId: Long?): HandleUriResult
    data class ShowPicker(val uriString: String, val host: String, val hostRuleId: Long?): HandleUriResult
    data class InvalidUri(val reason: String): HandleUriResult
}
