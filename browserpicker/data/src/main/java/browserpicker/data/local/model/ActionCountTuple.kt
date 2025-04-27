package browserpicker.data.local.model

import androidx.room.ColumnInfo
import browserpicker.domain.model.InteractionAction
import kotlinx.serialization.Serializable

data class ActionCountTuple(
    @ColumnInfo(name = "action")
    val action: InteractionAction,

    @ColumnInfo(name = "count")
    val count: Long
)

@Serializable
data class GroupCountTuple(
    val groupValue: String,
    val count: Int
)

@Serializable
data class DateCountTuple(
    val dateString: String,
    val count: Int
)
