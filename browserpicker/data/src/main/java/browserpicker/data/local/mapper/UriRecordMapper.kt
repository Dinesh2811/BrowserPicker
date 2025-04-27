package browserpicker.data.local.mapper

import browserpicker.data.local.entity.*
import browserpicker.domain.model.*

object UriRecordMapper {
    fun UriRecordEntity.toDomainModel(): UriRecord = UriRecord(
        id = this.id,
        uriString = this.uriString,
        host = this.host,
        timestamp = this.timestamp,
        uriSource = this.uriSource,
        interactionAction = this.interactionAction,
        chosenBrowserPackage = this.chosenBrowserPackage,
        associatedHostRuleId = this.associatedHostRuleId
    )

    fun UriRecord.toEntity(): UriRecordEntity = UriRecordEntity(
        id = this.id,
        uriString = this.uriString,
        host = this.host,
        timestamp = this.timestamp,
        uriSource = this.uriSource,
        interactionAction = this.interactionAction,
        chosenBrowserPackage = this.chosenBrowserPackage,
        associatedHostRuleId = this.associatedHostRuleId
    )

    fun List<UriRecordEntity>.toDomainModels(): List<UriRecord> = this.map { it.toDomainModel() }
}

object HostRuleMapper {
    fun HostRuleEntity.toDomainModel(): HostRule = HostRule(
        id = this.id,
        host = this.host,
        uriStatus = this.uriStatus,
        folderId = this.folderId,
        preferredBrowserPackage = this.preferredBrowserPackage,
        isPreferenceEnabled = this.isPreferenceEnabled,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    fun HostRule.toEntity(): HostRuleEntity = HostRuleEntity(
        id = this.id,
        host = this.host,
        uriStatus = this.uriStatus,
        folderId = this.folderId,
        preferredBrowserPackage = this.preferredBrowserPackage,
        isPreferenceEnabled = this.isPreferenceEnabled,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    fun List<HostRuleEntity>.toDomainModels(): List<HostRule> = this.map { it.toDomainModel() }
}

object FolderMapper  {
    fun FolderEntity.toDomainModel(): Folder = Folder(
        id = this.id,
        parentFolderId = this.parentFolderId,
        name = this.name,
        type = this.folderType,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    fun Folder.toEntity(): FolderEntity = FolderEntity(
        id = this.id,
        parentFolderId = this.parentFolderId,
        name = this.name,
        folderType = this.type,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    fun List<FolderEntity>.toDomainModels(): List<Folder> = this.map { it.toDomainModel() }
}

object BrowserUsageStatMapper  {
    fun BrowserUsageStatEntity.toDomainModel(): BrowserUsageStat = BrowserUsageStat(
        browserPackageName = this.browserPackageName,
        usageCount = this.usageCount,
        lastUsedTimestamp = this.lastUsedTimestamp
    )

    fun BrowserUsageStat.toEntity(): BrowserUsageStatEntity = BrowserUsageStatEntity(
        browserPackageName = this.browserPackageName,
        usageCount = this.usageCount,
        lastUsedTimestamp = this.lastUsedTimestamp
    )

    fun List<BrowserUsageStatEntity>.toDomainModels(): List<BrowserUsageStat> = this.map { it.toDomainModel() }
}