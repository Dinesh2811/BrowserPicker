package browserpicker.data.local.model

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
    @ColumnInfo(name = "dateString")
    val dateString: String?,
    @ColumnInfo(name = "count")
    val count: Int
)

