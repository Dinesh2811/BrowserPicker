package com.dinesh.playground.testing

import androidx.room.TypeConverter
import kotlinx.datetime.Instant

/**
 * Type converters for Room database.
 */
object Converters {

    // --- Instant <-> Long ---
    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }

    @TypeConverter
    fun timestampToInstant(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    // --- UriStatus <-> String ---
    @TypeConverter
    fun uriStatusToString(status: UriStatus?): String? {
        return status?.name // Store enum name as String (e.g., "BOOKMARKED")
    }

    @TypeConverter
    fun stringToUriStatus(value: String?): UriStatus? {
        return value?.let { enumValueOf<UriStatus>(it) }
    }

    // --- UriSource <-> String ---
    @TypeConverter
    fun uriSourceToString(source: UriSource?): String? {
        return source?.name
    }

    @TypeConverter
    fun stringToUriSource(value: String?): UriSource? {
        return value?.let { enumValueOf<UriSource>(it) }
    }

    // --- FolderType <-> String ---
    @TypeConverter
    fun folderTypeToString(type: FolderType?): String? {
        return type?.name
    }

    @TypeConverter
    fun stringToFolderType(value: String?): FolderType? {
        return value?.let { enumValueOf<FolderType>(it) }
    }
}
