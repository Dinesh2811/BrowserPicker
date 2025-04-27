package browserpicker.data.local.db

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

