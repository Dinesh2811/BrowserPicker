package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import kotlinx.datetime.LocalDate

@Immutable
sealed interface GroupKey {

    @Immutable
    data class Date(val value: LocalDate): GroupKey

    @Immutable
    data class Action(val value: InteractionAction): GroupKey

    @Immutable
    data class Browser(val value: String): GroupKey

    @Immutable
    data class Source(val value: UriSource): GroupKey
}

fun groupKeyToStableString(key: GroupKey): String = when (key) {
    is GroupKey.Date -> key.value.toString()
    is GroupKey.Action -> key.value.name
    is GroupKey.Browser -> key.value
    is GroupKey.Source -> key.value.name
}
