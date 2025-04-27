package browserpicker.data.local.entity

import androidx.compose.runtime.Immutable
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
            childColumns = ["host_rule_id"],
            onDelete = ForeignKey.SET_NULL, // If the rule for the host is deleted, keep the history but unlink the specific rule ID
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        ),
    ],
    indices = [
        Index("host_rule_id"),
        Index("timestamp"),
        Index("uri_string"),
        Index("interaction_action"),
        Index("uri_source")
    ],
)
@Immutable
data class UriRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uri_record_id")
    val id: Long = 0,

    @ColumnInfo(name = "uri_string")
    val uriString: String,

    @ColumnInfo(name = "host_rule_id")
    val hostRuleId: Long?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Instant,

    @ColumnInfo(name = "uri_source", typeAffinity = ColumnInfo.INTEGER)
    val uriSource: UriSource,

    @ColumnInfo(name = "interaction_action", typeAffinity = ColumnInfo.INTEGER)
    val interactionAction: InteractionAction,

    @ColumnInfo(name = "chosen_browser_package")
    val chosenBrowserPackage: String? = null,
)
