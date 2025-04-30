package browserpicker.data.local.db

import androidx.room.TypeConverter
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.UriStatus
import kotlinx.datetime.Instant

object Converters {
    @TypeConverter
    @JvmStatic
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    @JvmStatic
    fun longToInstant(value: Long?): Instant? = value?.let {
        runCatching { Instant.fromEpochMilliseconds(it) }.getOrNull()
    }

//    @TypeConverter
//    @JvmStatic
//    fun uriSourceToInt(uriSource: UriSource?): Int? = uriSource?.value
//
//    @TypeConverter
//    @JvmStatic
//    fun intToUriSource(value: Int?): UriSource? = value?.let { UriSource.fromValueOrNull(it) }
//
//    // Providing non-null variants in case column is non-nullable in DB
//    @TypeConverter
//    @JvmStatic
//    fun uriSourceToIntNN(uriSource: UriSource): Int = uriSource.value
//
//    @TypeConverter
//    @JvmStatic
//    fun intToUriSourceNN(value: Int): UriSource = UriSource.fromValue(value) // Throws if invalid
//
//    @TypeConverter
//    @JvmStatic
//    fun interactionActionToInt(interactionAction: InteractionAction?): Int? = interactionAction?.value
//
//    @TypeConverter
//    @JvmStatic
//    fun intToInteractionAction(value: Int?): InteractionAction? = value?.let { InteractionAction.fromValueOrNull(it) }
//
//    // Non-null variant
//    @TypeConverter
//    @JvmStatic
//    fun interactionActionToIntNN(interactionAction: InteractionAction): Int = interactionAction.value
//
//    @TypeConverter
//    @JvmStatic
//    fun intToInteractionActionNN(value: Int): InteractionAction = InteractionAction.fromValue(value) // Defaults to UNKNOWN
//
//    @TypeConverter
//    @JvmStatic
//    fun uriStatusToInt(uriStatus: UriStatus?): Int? = uriStatus?.value
//
//    @TypeConverter
//    @JvmStatic
//    fun intToUriStatus(value: Int?): UriStatus? = value?.let { UriStatus.fromValueOrNull(it) }
//
//    // Non-null variant
//    @TypeConverter
//    @JvmStatic
//    fun uriStatusToIntNN(uriStatus: UriStatus): Int = uriStatus.value
//
//    @TypeConverter
//    @JvmStatic
//    fun intToUriStatusNN(value: Int): UriStatus = UriStatus.fromValue(value) // Defaults to UNKNOWN
//
//    @TypeConverter
//    @JvmStatic
//    fun folderTypeToInt(folderType: FolderType?): Int? = folderType?.value
//
//    @TypeConverter
//    @JvmStatic
//    fun intToFolderType(value: Int?): FolderType? = value?.let { FolderType.fromValueOrNull(it) }
//
//    // Non-null variant needed as Folder type cannot be null
//    @TypeConverter
//    @JvmStatic
//    fun folderTypeToIntNN(folderType: FolderType): Int = folderType.value
//
//    @TypeConverter
//    @JvmStatic
//    fun intToFolderTypeNN(value: Int): FolderType = FolderType.fromValue(value) // Throws if invalid
}
