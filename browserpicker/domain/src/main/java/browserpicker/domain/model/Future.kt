//package browserpicker.domain.model
//
//import androidx.compose.runtime.Immutable
//
//@Immutable
//sealed interface UriRecordAdvancedFilterDomain {
//    val fieldName: String
//    val operator: FilterOperator
//
//    // Example: Filter for records that have a chosen browser
//    @Immutable
//    data class HasChosenBrowser(override val fieldName: String, override val operator: FilterOperator): UriRecordAdvancedFilterDomain
//
//    // Example: Filter for records that are associated with a HostRule
//    @Immutable
//    data class IsAssociatedWithHostRule(override val fieldName: String, override val operator: FilterOperator): UriRecordAdvancedFilterDomain
//
//    @Immutable
//    data class IsBookmarked(override val fieldName: String, override val operator: FilterOperator): UriRecordAdvancedFilterDomain
//    // Add More specific filter types if you can think of that are relevant for my UseCase
//
//    @Immutable
//    data class StringFilter(
//        override val fieldName: String,
//        override val operator: FilterOperator,
//        val value: String
//    ) : UriRecordAdvancedFilterDomain
//
//    @Immutable
//    data class NumberFilter(
//        override val fieldName: String,
//        override val operator: FilterOperator,
//        val value: Long
//    ) : UriRecordAdvancedFilterDomain
//
//    @Immutable
//    data class DateFilter(
//        override val fieldName: String,
//        override val operator: FilterOperator,
//        val value: Instant
//    ) : UriRecordAdvancedFilterDomain
//
//    @Immutable
//    data class EnumFilter<T>(
//        override val fieldName: String,
//        override val operator: FilterOperator,
//        val value: T
//    ) : UriRecordAdvancedFilterDomain
//
//    companion object {
//        const val FIELD_URI_STRING = "uri_string"
//        const val FIELD_HOST = "host"
//        const val FIELD_TIMESTAMP = "timestamp"
//        const val FIELD_URI_SOURCE = "uri_source"
//        const val FIELD_INTERACTION_ACTION = "interaction_action"
//        const val FIELD_CHOSEN_BROWSER = "chosen_browser_package"
//    }
//}
//
//@Immutable
//enum class FilterOperator {
//    EQUALS,
//    NOT_EQUALS,
//    CONTAINS,
//    NOT_CONTAINS,
//    STARTS_WITH,
//    ENDS_WITH,
//    GREATER_THAN,
//    LESS_THAN,
//    GREATER_THAN_OR_EQUALS,
//    LESS_THAN_OR_EQUALS,
//    IS_NULL,
//    IS_NOT_NULL
//}
