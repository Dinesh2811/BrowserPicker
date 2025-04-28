package browserpicker.data.local.query.model

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable

/**
 * DTO for results of queries grouping by a string-based value and counting occurrences.
 * Used for InteractionAction, UriSource, Host, ChosenBrowser groupings.
 */
@Serializable
data class GroupCount(
    /**
     * The value representing the group. This could be an enum name, host, browser package,
     * or a special marker like [browserpicker.domain.query.GroupKey.NULL_BROWSER_GROUP_VALUE].
     */
    @ColumnInfo(name = "groupValue") // Matches alias in SQL query
    val groupValue: String?, // Nullable as COALESCE might return the marker, or the original value could be null/empty
    @ColumnInfo(name = "count") // Matches alias in SQL query
    val count: Int
)

/**
 * DTO for results of queries grouping by date and counting occurrences.
 */
@Serializable
data class DateCount(
    /** The date string in 'YYYY-MM-DD' format. */
    @ColumnInfo(name = "dateString") // Matches alias in SQL query
    val dateString: String?, // Nullable in case the STRFTIME result is unexpectedly null
    @ColumnInfo(name = "count") // Matches alias in SQL query
    val count: Int
)
