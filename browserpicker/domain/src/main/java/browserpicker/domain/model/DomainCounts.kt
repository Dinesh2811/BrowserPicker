package browserpicker.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Immutable
data class DomainGroupCount(val groupValue: String?, val count: Int)
@Immutable
data class DomainDateCount(val date: Instant?, val count: Int)

