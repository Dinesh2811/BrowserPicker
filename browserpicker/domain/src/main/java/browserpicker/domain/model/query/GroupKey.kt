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
    data class InteractionActionKey(val value: InteractionAction): GroupKey

    @Immutable
    data class UriSourceKey(val value: UriSource): GroupKey

    @Immutable
    data class HostKey(val value: String): GroupKey

    @Immutable
    data class ChosenBrowserKey(val value: String): GroupKey {
        companion object {
            const val NULL_BROWSER_MARKER = "Unknown Browser"
        }
    }
}

fun groupKeyToStableString(key: GroupKey): String = when (key) {
    is GroupKey.Date -> "DATE_${key.value.toString()}"
    is GroupKey.InteractionActionKey -> "ACTION_${key.value.name}"
    is GroupKey.UriSourceKey -> "SOURCE_${key.value.name}"
    is GroupKey.HostKey -> "HOST_${key.value}"
    is GroupKey.ChosenBrowserKey -> "BROWSER_${key.value}"
}
