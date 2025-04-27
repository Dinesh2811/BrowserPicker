package browserpicker.data.local.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import browserpicker.domain.model.FolderType
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriStatus
import browserpicker.domain.model.UriSource

// Note: @ProvidedTypeConverter requires these to be available via DI (Hilt)

@ProvidedTypeConverter
class InstantConverter @Inject constructor() {
    @TypeConverter
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let {
        // Use runCatching for safety against potential malformed data
        runCatching { Instant.fromEpochMilliseconds(it) }.getOrNull()
    }
}

@ProvidedTypeConverter
class UriSourceConverter @Inject constructor() {
    @TypeConverter
    fun uriSourceToInt(uriSource: UriSource?): Int? = uriSource?.value

    @TypeConverter
    fun intToUriSource(value: Int?): UriSource? = value?.let { UriSource.fromValueOrNull(it) }

    // Providing non-null variants in case column is non-nullable in DB
    @TypeConverter
    fun uriSourceToIntNN(uriSource: UriSource): Int = uriSource.value

    @TypeConverter
    fun intToUriSourceNN(value: Int): UriSource = UriSource.fromValue(value) // Throws if invalid
}

@ProvidedTypeConverter
class InteractionActionConverter @Inject constructor() {
    @TypeConverter
    fun interactionActionToInt(interactionAction: InteractionAction?): Int? = interactionAction?.value

    @TypeConverter
    fun intToInteractionAction(value: Int?): InteractionAction? = value?.let { InteractionAction.fromValueOrNull(it) }

    // Non-null variant
    @TypeConverter
    fun interactionActionToIntNN(interactionAction: InteractionAction): Int = interactionAction.value

    @TypeConverter
    fun intToInteractionActionNN(value: Int): InteractionAction = InteractionAction.fromValue(value) // Defaults to UNKNOWN
}

@ProvidedTypeConverter
class UriStatusConverter @Inject constructor() {
    @TypeConverter
    fun uriStatusToInt(uriStatus: UriStatus?): Int? = uriStatus?.value

    @TypeConverter
    fun intToUriStatus(value: Int?): UriStatus? = value?.let { UriStatus.fromValueOrNull(it) }

    // Non-null variant
    @TypeConverter
    fun uriStatusToIntNN(uriStatus: UriStatus): Int = uriStatus.value

    @TypeConverter
    fun intToUriStatusNN(value: Int): UriStatus = UriStatus.fromValue(value) // Defaults to UNKNOWN
}

@ProvidedTypeConverter
class FolderTypeConverter @Inject constructor() {
    // Non-null variant needed as Folder type cannot be null
    @TypeConverter
    fun folderTypeToIntNN(folderType: FolderType): Int = folderType.value

    @TypeConverter
    fun intToFolderTypeNN(value: Int): FolderType = FolderType.fromValue(value) // Throws if invalid
}

/*
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriStatus
import browserpicker.domain.model.UriSource
import kotlinx.datetime.Instant
import javax.inject.Inject

@ProvidedTypeConverter
class InstantConverter @Inject constructor() {
    @TypeConverter
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let {
        runCatching { Instant.fromEpochMilliseconds(it) }.getOrNull()
    }
}

@ProvidedTypeConverter
class UriSourceConverter @Inject constructor() {
    @TypeConverter
    fun uriSourceToInt(uriSource: UriSource): Int = uriSource.value

    @TypeConverter
    fun intToUriSource(value: Int): UriSource = UriSource.fromValue(value)
}

@ProvidedTypeConverter
class InteractionActionConverter @Inject constructor() {
    @TypeConverter
    fun interactionActionToInt(interactionAction: InteractionAction?): Int = interactionAction?.value?: InteractionAction.UNKNOWN.value

    @TypeConverter
    fun intToInteractionAction(value: Int): InteractionAction = InteractionAction.fromValue(value)
}

@ProvidedTypeConverter
class RuleTypeConverter @Inject constructor() {
    @TypeConverter
    fun ruleTypeToInt(uriStatus: UriStatus?): Int = uriStatus?.value?: UriStatus.UNKNOWN.value

    @TypeConverter
    fun intToRuleType(value: Int): UriStatus = UriStatus.fromValue(value)
}

@ProvidedTypeConverter
class FolderTypeConverter @Inject constructor() {
    // Non-null variant needed as Folder type cannot be null
    @TypeConverter
    fun folderTypeToInt(folderType: FolderType): Int = folderType.value

    @TypeConverter
    fun intToFolderType(value: Int): FolderType = FolderType.fromValue(value)
}

 */

