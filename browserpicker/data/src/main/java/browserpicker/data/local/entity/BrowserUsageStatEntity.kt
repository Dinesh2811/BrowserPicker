package browserpicker.data.local.entity

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    tableName = "browser_usage_stats",
    indices = [
        Index(value = ["last_used_timestamp"]),
        Index(value = ["usage_count"])
    ]
)
data class BrowserUsageStatEntity(
    @PrimaryKey
    @ColumnInfo(name = "browser_package_name")
    val browserPackageName: String,

    @ColumnInfo(name = "usage_count", defaultValue = "0")
    val usageCount: Long = 0,

    @ColumnInfo(name = "last_used_timestamp")
    val lastUsedTimestamp: Instant
)
