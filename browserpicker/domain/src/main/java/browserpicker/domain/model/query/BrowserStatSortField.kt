package browserpicker.domain.model.query

// package browserpicker.domain.model

// Enum for sorting browser stats
enum class BrowserStatSortField {
    USAGE_COUNT, LAST_USED_TIMESTAMP
}

// Data class to hold distinct values for filtering UI
data class FilterOptions(
    val distinctHistoryHosts: List<String> = emptyList(),
    val distinctRuleHosts: List<String> = emptyList(),
    val distinctChosenBrowsers: List<String?> = emptyList() // Includes null
)

// Sealed Interface for the result of handling an intercepted URI
sealed interface HandleUriResult {
    /** URI should be blocked immediately. */
    data object Blocked : HandleUriResult

    /** URI should be opened directly in the preferred browser. */
    data class OpenDirectly(val browserPackageName: String, val hostRuleId: Long?) : HandleUriResult

    /** The app should show the browser picker UI. */
    data class ShowPicker(val uriString: String, val host: String, val hostRuleId: Long?) : HandleUriResult

    /** The URI provided was invalid or could not be processed. */
    data class InvalidUri(val reason: String) : HandleUriResult
}
