package browserpicker.domain.usecases.system.model

import browserpicker.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class BackupDataContainer(
    val appVersion: String,
    val backupTimestamp: Instant,
    val uriRecords: List<UriRecord>? = null, // Nullable if history is not included
    val hostRules: List<HostRule>,
    val folders: List<Folder>,
    val browserStats: List<BrowserUsageStat>
    // Add other necessary data models here if they need to be backed up
)
