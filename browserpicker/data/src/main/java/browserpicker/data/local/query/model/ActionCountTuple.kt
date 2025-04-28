//package browserpicker.data.local.query.model
//
//import androidx.compose.runtime.Immutable
//import androidx.room.ColumnInfo
//import browserpicker.domain.model.InteractionAction
//import browserpicker.domain.model.UriSource
//import kotlinx.datetime.LocalDate
//import kotlinx.serialization.Serializable
//
//@Immutable
//data class UriRecordAdvancedFilter(
//    val customSqlCondition: String,
//    val args: List<Any>
//) {
//    init {
//        val placeholderCount = customSqlCondition.count { it == '?' }
//        require(placeholderCount == args.size) {
//            "Number of placeholders '?' ($placeholderCount) in customSqlCondition must match the number of args (${args.size}). " +
//                    "SQL: '$customSqlCondition', Args: $args"
//        }
//    }
//}
//
//@Immutable
//sealed interface GroupKey {
//    @Immutable @JvmInline value class Date(val value: LocalDate) : GroupKey
//    @Immutable @JvmInline value class InteractionActionKey(val value: InteractionAction) : GroupKey
//    @Immutable @JvmInline value class UriSourceKey(val value: UriSource) : GroupKey
//    @Immutable @JvmInline value class HostKey(val value: String) : GroupKey
//    @Immutable @JvmInline value class ChosenBrowserKey(val value: String) : GroupKey
//
//    companion object {
//        const val NULL_BROWSER_GROUP_VALUE = GroupingConstants.NULL_BROWSER_GROUP_VALUE
//        const val NULL_BROWSER_DISPLAY_NAME = "Unknown Browser"
//    }
//}
//
//fun groupKeyToStableString(key: GroupKey): String = when (key) {
//    is GroupKey.Date -> "DATE_${key.value}"
//    is GroupKey.InteractionActionKey -> "ACTION_${key.value.name}"
//    is GroupKey.UriSourceKey -> "SOURCE_${key.value.name}"
//    is GroupKey.HostKey -> "HOST_${key.value}"
//    is GroupKey.ChosenBrowserKey -> "BROWSER_${key.value}"
//}
//
//@Serializable
//data class GroupCount(
//    @ColumnInfo(name = "groupValue")
//    val groupValue: String?,
//    @ColumnInfo(name = "count")
//    val count: Int
//)
//
//@Serializable
//data class DateCount(
//    @ColumnInfo(name = "dateString")
//    val dateString: String?,
//    @ColumnInfo(name = "count")
//    val count: Int
//)
