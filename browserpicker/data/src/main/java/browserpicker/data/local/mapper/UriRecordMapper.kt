package browserpicker.data.local.mapper

import browserpicker.data.local.entity.*
import browserpicker.domain.model.*
import kotlinx.datetime.Instant

object UriRecordMapper {
    fun UriRecordEntity.toDomainModel(): UriRecord {
        return UriRecord(
            id = this.id,
            uriString = this.uriString,
            hostRuleId = this.hostRuleId, // Keep as is
            timestamp = this.timestamp,
            uriSource = this.uriSource, // Assumes enum conversion is correct
            interactionAction = this.interactionAction,
            chosenBrowserPackage = this.chosenBrowserPackage
        )
    }

    fun UriRecord.toEntity(): UriRecordEntity {
        return UriRecordEntity(
            id = this.id,
            uriString = this.uriString,
            hostRuleId = this.hostRuleId,
            timestamp = this.timestamp,
            uriSource = this.uriSource,
            interactionAction = this.interactionAction,
            chosenBrowserPackage = this.chosenBrowserPackage
        )
    }
}

object HostRuleMapper {
    fun HostRuleEntity.toDomainModel(): HostRule {
        return HostRule(
            id = this.id,
            host = this.host,
            uriStatus = this.uriStatus, // Assumes enum conversion is correct
            bookmarkFolderId = this.bookmarkFolderId,
            blockFolderId = this.blockFolderId,
            preferredBrowserPackage = this.preferredBrowserPackage,
            isPreferenceEnabled = this.isPreferenceEnabled,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    fun HostRule.toEntity(currentTime: Instant): HostRuleEntity {
        // Determine if it's an insert or update based on ID for timestamp setting
        val isInsert = this.id == 0L
        return HostRuleEntity(
            id = this.id,
            host = this.host,
            uriStatus = this.uriStatus,
            bookmarkFolderId = this.bookmarkFolderId,
            blockFolderId = this.blockFolderId,
            preferredBrowserPackage = this.preferredBrowserPackage,
            isPreferenceEnabled = this.isPreferenceEnabled,
            createdAt = if (isInsert) currentTime else this.createdAt, // Set on insert
            updatedAt = currentTime // Always update 'updatedAt'
        )
    }
}

object FolderMapper  {
    fun BookmarkFolderEntity.toDomainModel(): Folder {
        return Folder(
            id = this.id,
            parentFolderId = this.parentFolderId,
            name = this.name,
            type = FolderType.BOOKMARK, // Set type explicitly
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    fun BlockFolderEntity.toDomainModel(): Folder {
        return Folder(
            id = this.id,
            parentFolderId = this.parentFolderId,
            name = this.name,
            type = FolderType.BLOCK, // Set type explicitly
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    // Map Domain Folder back to specific Entity type
    fun Folder.toBookmarkEntity(currentTime: Instant): BookmarkFolderEntity {
        if (this.type != FolderType.BOOKMARK) throw IllegalArgumentException("Folder must be of type BOOKMARK")
        val isInsert = this.id == 0L
        return BookmarkFolderEntity(
            id = this.id,
            parentFolderId = this.parentFolderId,
            name = this.name,
            createdAt = if(isInsert) currentTime else this.createdAt,
            updatedAt = currentTime
        )
    }

    fun Folder.toBlockEntity(currentTime: Instant): BlockFolderEntity {
        if (this.type != FolderType.BLOCK) throw IllegalArgumentException("Folder must be of type BLOCK")
        val isInsert = this.id == 0L
        return BlockFolderEntity(
            id = this.id,
            parentFolderId = this.parentFolderId,
            name = this.name,
            createdAt = if(isInsert) currentTime else this.createdAt,
            updatedAt = currentTime
        )
    }
}
