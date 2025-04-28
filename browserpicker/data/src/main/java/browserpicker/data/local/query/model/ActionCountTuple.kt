package browserpicker.data.local.query.model

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable

@Serializable
data class GroupCount(
    @ColumnInfo(name = "groupValue")
    val groupValue: String?,
    @ColumnInfo(name = "count")
    val count: Int
)

@Serializable
data class DateCount(
    /** The date string in 'YYYY-MM-DD' format. */
    @ColumnInfo(name = "dateString")
    val dateString: String?,
    @ColumnInfo(name = "count")
    val count: Int
)
