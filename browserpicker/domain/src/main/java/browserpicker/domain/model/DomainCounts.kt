package browserpicker.domain.model

import kotlinx.datetime.Instant

data class DomainGroupCount(val groupValue: String?, val count: Int)
data class DomainDateCount(val date: Instant?, val count: Int)

