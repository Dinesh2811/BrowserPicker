package com.dinesh.playground.testing

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant // Use kotlinx.datetime

/**
 * Represents a folder in the database.
 */
@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parent_id"]),
        Index(value = ["name", "type", "parent_id"], unique = true) // Ensure unique name within parent/type
    ]
)
data class FolderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // Using String for UUIDs

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "parent_id", index = true) // Index for faster lookup of children
    val parentId: String?, // Null for root folders

    @ColumnInfo(name = "type")
    val type: FolderType, // Stored via TypeConverter

    @ColumnInfo(name = "created_at")
    val createdAt: Instant, // Stored via TypeConverter

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant // Stored via TypeConverter
)

/**
 * Represents a URI entry in the database.
 */
@Entity(
    tableName = "uri_entries",
    indices = [
        Index(value = ["uri_string"], unique = true),
        Index(value = ["status"]),
        Index(value = ["folder_id"]),
        Index(value = ["intercepted_at"]),
        Index(value = ["last_accessed_at"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.RESTRICT, // Prevent deleting a folder if URIs still reference it directly
            // Deletion logic must be handled manually in UseCase/Repository
            onUpdate = ForeignKey.CASCADE // If folder ID changes, update it here
        )
    ]
)
data class UriEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // Using String for UUIDs

    @ColumnInfo(name = "uri_string")
    val uriString: String,

    @ColumnInfo(name = "status")
    val status: UriStatus, // Stored via TypeConverter

    @ColumnInfo(name = "folder_id", index = true) // Null if status is NONE
    val folderId: String?, // Foreign key to FolderEntity.id

    @ColumnInfo(name = "source")
    val source: UriSource, // Stored via TypeConverter

    @ColumnInfo(name = "intercepted_at")
    val interceptedAt: Instant, // Stored via TypeConverter

    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Instant?, // Stored via TypeConverter

    @ColumnInfo(name = "status_updated_at")
    val statusUpdatedAt: Instant // Stored via TypeConverter
)

/**
 * Represents a URI preference in the database.
 */
@Entity(
    tableName = "uri_preferences",
    indices = [
        Index(value = ["uri_string"], unique = true),
        Index(value = ["preferred_browser_package_name"])
    ]
)
data class UriPreferenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // Using String for UUIDs

    @ColumnInfo(name = "uri_string")
    val uriString: String,

    @ColumnInfo(name = "preferred_browser_package_name")
    val preferredBrowserPackageName: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant // Stored via TypeConverter
)


/**
 * Data class to hold results from queries joining UriEntryEntity and FolderEntity.
 * Room can populate this directly from JOIN queries.
 * Used by `observeUriEntriesWithFolders` in LocalDataSource.
 */
data class UriEntryWithFolderEntity(
    // Embed the UriEntryEntity fields directly
    @androidx.room.Embedded
    val uriEntry: UriEntryEntity,

    // Embed the FolderEntity fields, prefixed to avoid name collisions
    @androidx.room.Embedded(prefix = "folder_")
    val folder: FolderEntity? // Folder can be null if uriEntry.folderId is null
)
