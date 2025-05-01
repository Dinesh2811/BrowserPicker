package browserpicker.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Instant

@Immutable
data class GroupCount(val groupValue: String?, val count: Int)

@Immutable
data class DateCount(val date: Instant?, val count: Int)
