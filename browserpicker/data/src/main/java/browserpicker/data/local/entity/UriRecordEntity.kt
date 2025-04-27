package browserpicker.data.local.entity

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
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
            parentColumns = ["host_rule_id"],
            childColumns = ["associated_host_rule_id"],
            onDelete = ForeignKey.SET_NULL, // Keep history even if rule deleted
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [
        Index("associated_host_rule_id"),
        Index("host"),
        Index("timestamp"),
        Index("uri_string"),
        Index("interaction_action"),
        Index("uri_source"),
        Index("chosen_browser_package")
    ]
)
data class UriRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uri_record_id")
    val id: Long = 0,

    @ColumnInfo(name = "uri_string", collate = ColumnInfo.NOCASE)
    val uriString: String,

    @ColumnInfo(name = "host", collate = ColumnInfo.NOCASE)
    val host: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Instant,

    @ColumnInfo(name = "uri_source", typeAffinity = ColumnInfo.INTEGER)
    val uriSource: UriSource,

    @ColumnInfo(name = "interaction_action", typeAffinity = ColumnInfo.INTEGER)
    val interactionAction: InteractionAction,

    @ColumnInfo(name = "chosen_browser_package")
    val chosenBrowserPackage: String? = null,

    @ColumnInfo(name = "associated_host_rule_id")
    val associatedHostRuleId: Long?
)
