package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import kotlinx.datetime.Instant


@Immutable
enum class SortOrder { ASC, DESC }

@Immutable
enum class UriRecordSortField {
    TIMESTAMP,
    URI_STRING,
    HOST,
    CHOSEN_BROWSER,
    INTERACTION_ACTION,
    URI_SOURCE
}

@Immutable
enum class UriRecordGroupField {
    NONE,
    INTERACTION_ACTION,
    CHOSEN_BROWSER,
    URI_SOURCE,
    HOST,
    DATE
}
