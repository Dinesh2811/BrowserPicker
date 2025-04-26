package browserpicker.data.core.local.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.RuleType
import browserpicker.domain.model.UriSource
import kotlinx.datetime.Instant

@ProvidedTypeConverter
class InstantConverter {
    @TypeConverter
    fun dateInstantToLong(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun toDateInstant(value: Long?): Instant? = value?.let {
        try { Instant.fromEpochMilliseconds(it) } catch (e: Exception) { null }
    }
}

@ProvidedTypeConverter
class UriSourceConverter {
    @TypeConverter
    fun uriSourceToInt(uriSource: UriSource): Int = uriSource.value

    @TypeConverter
    fun toUriSource(value: Int): UriSource? = try { UriSource.fromValue(value) } catch (e: IllegalArgumentException) { null }
}

@ProvidedTypeConverter
class InteractionActionConverter {
    @TypeConverter
    fun interactionActionToInt(interactionAction: InteractionAction): Int = interactionAction.value

    @TypeConverter
    fun toInteractionAction(value: Int): InteractionAction? = try { InteractionAction.fromValue(value) } catch (e: IllegalArgumentException) { null }
}

@ProvidedTypeConverter
class RuleTypeConverter {
    @TypeConverter
    fun ruleTypeToInt(ruleType: RuleType): Int = ruleType.value

    @TypeConverter
    fun toRuleType(value: Int): RuleType? = try { RuleType.fromValue(value) } catch (e: IllegalArgumentException) { null }
}

