package browserpicker.data.core.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import browserpicker.domain.model.*
import kotlinx.datetime.Instant

@Entity(
    tableName = "uri_records",
    foreignKeys = [
        ForeignKey(
            entity = HostRuleEntity::class,
            parentColumns = ["host_id"],
            childColumns = ["host_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("host_id"),
        Index("timestamp"),
        Index("interaction_action"),
        Index("uri_string")
    ]
)
data class UriRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uri_record_id")
    val uriRecordId: Long = 0,

    @ColumnInfo(name = "uri_string", index = true)
    val uriString: String,

    @ColumnInfo(name = "host_id", index = true)
    val hostId: Long,

    @ColumnInfo(name = "timestamp", index = true)
    val timestamp: Instant,

    @ColumnInfo(name = "uri_source", typeAffinity = ColumnInfo.INTEGER)
    val uriSource: UriSource,

    @ColumnInfo(name = "interaction_action", index = true, typeAffinity = ColumnInfo.INTEGER)
    val interactionAction: InteractionAction,

    @ColumnInfo(name = "chosen_browser_package")
    val chosenBrowserPackage: String? = null,
)
