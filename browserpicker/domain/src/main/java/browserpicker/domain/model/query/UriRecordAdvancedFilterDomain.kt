package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.UriStatus

/**
 * Represents specific, predefined advanced filtering criteria that can be applied
 * to UriRecord queries, defined purely within the domain layer.
 * The Data layer is responsible for translating these into actual SQL.
 */
@Immutable
sealed interface UriRecordAdvancedFilterDomain {
    /** Filter records based on whether they have an associated HostRule. */
    @Immutable
    data class HasAssociatedRule(val hasRule: Boolean) : UriRecordAdvancedFilterDomain

    /** Filter records based on their UriStatus (derived from associated HostRule). */
    // Note: This requires a JOIN in the data layer query builder.
    @Immutable
    data class HasUriStatus(val status: UriStatus) : UriRecordAdvancedFilterDomain

    // Add other domain-specific advanced filter types here as needed.
    // Examples:
    // data class IsInFolder(val folderId: Long) : UriRecordAdvancedFilterDomain
    // data class HasPreferenceEnabled(val isEnabled: Boolean) : UriRecordAdvancedFilterDomain
}
