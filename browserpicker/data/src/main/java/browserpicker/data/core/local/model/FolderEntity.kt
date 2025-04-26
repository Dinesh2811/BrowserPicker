//package browserpicker.data.core.local.model
//
//import androidx.room.ColumnInfo
//import androidx.room.Entity
//import androidx.room.ForeignKey
//import androidx.room.Index
//import androidx.room.PrimaryKey
//import browserpicker.domain.model.*
//import kotlinx.datetime.Instant
//
///**
// * Represents a folder for organizing Bookmarks or Blocked rules. Folders can be nested.
// */
//@Entity(
//    tableName = "folders",
//    indices = [
//        Index(value = ["name", "type", "parent_folder_id"], unique = true), // Unique name per type within the same parent
//        Index(value = ["parent_folder_id"]), // Index for finding children
//        Index(value = ["type"]) // Index for finding folders by type
//    ],
//    foreignKeys = [
//        ForeignKey(
//            entity = FolderEntity::class,
//            parentColumns = ["id"],
//            childColumns = ["parent_folder_id"],
//            onDelete = ForeignKey.SET_NULL // If parent is deleted, children become top-level
//        )
//    ]
//)
//data class FolderEntity(
//    @PrimaryKey(autoGenerate = true)
//    val id: Long = 0,
//
//    @ColumnInfo(name = "name")
//    val name: String,
//
//    @ColumnInfo(name = "type", index = true) // Index added via table indices above
//    val type: FolderType, // BOOKMARK or BLOCK
//
//    /** Null indicates a top-level folder. */
//    @ColumnInfo(name = "parent_folder_id", index = true) // Index added via table indices above
//    val parentFolderId: Long? = null,
//
//    @ColumnInfo(name = "created_at")
//    val createdAt: Instant,
//
//    @ColumnInfo(name = "updated_at")
//    val updatedAt: Instant
//)
//
///**
// * Represents a user-defined rule targeting a specific host or exact URI,
// * potentially associating it with a bookmark folder, block folder, or preferred browser.
// */
//@Entity(
//    tableName = "uri_rules",
//    indices = [
//        // Ensures only one rule definition exists per unique target (host or exact URI)
//        Index(value = ["target_identifier", "match_type"], unique = true),
//        // Indices for quick lookup based on folder associations
//        Index(value = ["bookmark_folder_id"]),
//        Index(value = ["block_folder_id"])
//    ],
//    foreignKeys = [
//        ForeignKey(
//            entity = FolderEntity::class,
//            parentColumns = ["id"],
//            childColumns = ["bookmark_folder_id"],
//            // If bookmark folder is deleted, just remove the link from the rule
//            onDelete = ForeignKey.SET_NULL
//            // Note: We might need to add constraints later (via Dao/UseCase)
//            // to ensure this folder is actually of type BOOKMARK.
//        ),
//        ForeignKey(
//            entity = FolderEntity::class,
//            parentColumns = ["id"],
//            childColumns = ["block_folder_id"],
//            // If block folder is deleted, just remove the link from the rule
//            onDelete = ForeignKey.SET_NULL
//            // Note: We might need to add constraints later (via Dao/UseCase)
//            // to ensure this folder is actually of type BLOCK.
//        )
//    ],
//    // Enforce mutual exclusivity of bookmark and block folders
//    ignoredColumns = ["check_constraint_placeholder"], // Room needs this syntax for check constraints
//)
//data class UriRuleEntity(
//    @PrimaryKey(autoGenerate = true)
//    val id: Long = 0,
//
//    /** The identifier to match against (host name or full URI string). */
//    @ColumnInfo(name = "target_identifier")
//    val targetIdentifier: String,
//
//    /** How to interpret the target_identifier (HOST or EXACT_URI). */
//    @ColumnInfo(name = "match_type")
//    val matchType: RuleMatchType,
//
//    /** Optional: The ID of the BOOKMARK folder this rule belongs to. */
//    @ColumnInfo(name = "bookmark_folder_id", index = true) // Redundant index, covered by table indices
//    val bookmarkFolderId: Long? = null,
//
//    /** Optional: The ID of the BLOCK folder this rule belongs to. */
//    @ColumnInfo(name = "block_folder_id", index = true) // Redundant index, covered by table indices
//    val blockFolderId: Long? = null,
//
//    /** Optional: The package name of the browser preferred for this rule target. */
//    @ColumnInfo(name = "preferred_browser_package")
//    val preferredBrowserPackage: String? = null,
//
//    @ColumnInfo(name = "is_enabled", defaultValue = "1")
//    val isEnabled: Boolean = true,
//
//    @ColumnInfo(name = "created_at")
//    val createdAt: Instant,
//
//    @ColumnInfo(name = "updated_at")
//    val updatedAt: Instant
//
//    /* --- Check constraint handling ---
//     * Room KSP generates the check based on the @Entity(check=...) annotation.
//     * The `ignoredColumns` property is a workaround needed for KSP if check constraints are present.
//     * https://issuetracker.google.com/issues/176429119 (or similar relevant issue)
//     */
//    // @ColumnInfo(name = "check_constraint_placeholder") val check_constraint_placeholder: Int = 0 // If needed by older KSP versions
//)
//
//
///**
// * Associates specific URIs with a HOST-based UriRuleEntity.
// * If a UriRuleEntity has matchType=HOST and related entries exist in this table,
// * the rule only applies to the specific URIs listed here.
// * If no entries exist here for a HOST rule, it applies to the entire host.
// */
//@Entity(
//    tableName = "rule_scope_uris",
//    primaryKeys = ["rule_id", "specific_uri"], // Composite primary key ensures URI uniqueness per rule
//    foreignKeys = [
//        ForeignKey(
//            entity = UriRuleEntity::class,
//            parentColumns = ["id"],
//            childColumns = ["rule_id"],
//            onDelete = ForeignKey.CASCADE // If the rule is deleted, delete these scope entries
//        )
//    ],
//    indices = [
//        Index(value = ["rule_id"]),
//        Index(value = ["specific_uri"]) // Index specific URIs for potential reverse lookups
//    ]
//)
//data class RuleScopeUriEntity(
//    @ColumnInfo(name = "rule_id", index = true) // Indexed via table indices
//    val ruleId: Long,
//
//    /** The specific, full URI string this rule scope applies to. */
//    @ColumnInfo(name = "specific_uri")
//    val specificUri: String,
//
//    // Note: No separate PK needed as ["rule_id", "specific_uri"] is the composite PK.
//    // Note: No timestamps needed here unless you need to track when a specific scope was added/modified.
//    // Keep it simple for now.
//)
//
//
///**
// * Represents a record of an intercepted URI and the interaction outcome. (Largely unchanged)
// */
//@Entity(
//    tableName = "uri_records",
//    foreignKeys = [
//        ForeignKey(
//            entity = UriRuleEntity::class,
//            parentColumns = ["id"],
//            childColumns = ["applied_rule_id"],
//            onDelete = ForeignKey.SET_NULL // Keep record even if rule is deleted
//        )
//    ],
//    indices = [
//        Index(value = ["timestamp"]),
//        Index(value = ["host"]),
//        Index(value = ["interaction_outcome"]),
//        Index(value = ["applied_rule_id"]),
//        Index(value = ["uri_string"]) // Index the full URI for history searches
//    ]
//)
//data class UriRecordEntity(
//    @PrimaryKey(autoGenerate = true)
//    val id: Long = 0,
//
//    @ColumnInfo(name = "uri_string", index = true) // Indexed via table indices
//    val uriString: String,
//
//    @ColumnInfo(name = "host", index = true) // Indexed via table indices
//    val host: String,
//
//    @ColumnInfo(name = "timestamp", index = true) // Indexed via table indices
//    val timestamp: Instant,
//
//    @ColumnInfo(name = "capture_source")
//    val captureSource: UriCaptureSource,
//
//    @ColumnInfo(name = "interaction_outcome", index = true) // Indexed via table indices
//    val interactionOutcome: InteractionOutcome,
//
//    @ColumnInfo(name = "chosen_browser_package")
//    val chosenBrowserPackage: String? = null,
//
//    @ColumnInfo(name = "applied_rule_id", index = true) // Indexed via table indices
//    val appliedRuleId: Long? = null
//)
