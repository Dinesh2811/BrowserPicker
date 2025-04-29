package browserpicker.playground.browserpicker.data

import browserpicker.playground.browserpicker.domain.*

object UriRecordMapper {
    fun toDomainModel(entity: UriRecordEntity): UriRecord = UriRecord(
        id = entity.id,
        uriString = entity.uriString,
        host = entity.host,
        timestamp = entity.timestamp,
        uriSource = UriSource.fromValue(entity.uriSource),
        interactionAction = InteractionAction.fromValue(entity.interactionAction),
        chosenBrowserPackage = entity.chosenBrowserPackage,
        associatedHostRuleId = entity.associatedHostRuleId
    )

    fun toEntity(model: UriRecord): UriRecordEntity = UriRecordEntity(
        id = model.id,
        uriString = model.uriString,
        host = model.host,
        timestamp = model.timestamp,
        uriSource = model.uriSource.value,
        interactionAction = model.interactionAction.value,
        chosenBrowserPackage = model.chosenBrowserPackage,
        associatedHostRuleId = model.associatedHostRuleId
    )

    fun toDomainModels(entities: List<UriRecordEntity>): List<UriRecord> = entities.map { toDomainModel(it) }
}

object HostRuleMapper {
    fun toDomainModel(entity: HostRuleEntity): HostRule = HostRule(
        id = entity.id,
        host = entity.host,
        uriStatus = UriStatus.fromValue(entity.uriStatus),
        folderId = entity.folderId,
        preferredBrowserPackage = entity.preferredBrowserPackage,
        isPreferenceEnabled = entity.isPreferenceEnabled,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(model: HostRule): HostRuleEntity = HostRuleEntity(
        id = model.id,
        host = model.host,
        uriStatus = model.uriStatus.value,
        folderId = model.folderId,
        preferredBrowserPackage = model.preferredBrowserPackage,
        isPreferenceEnabled = model.isPreferenceEnabled,
        createdAt = model.createdAt,
        updatedAt = model.updatedAt
    )

    fun toDomainModels(entities: List<HostRuleEntity>): List<HostRule> = entities.map { toDomainModel(it) }
}

object FolderMapper  {
    fun toDomainModel(entity: FolderEntity): Folder = Folder(
        id = entity.id,
        parentFolderId = entity.parentFolderId,
        name = entity.name,
        type = FolderType.fromValue(entity.folderType),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(model: Folder): FolderEntity = FolderEntity(
        id = model.id,
        parentFolderId = model.parentFolderId,
        name = model.name,
        folderType = model.type.value,
        createdAt = model.createdAt,
        updatedAt = model.updatedAt
    )

    fun toDomainModels(entities: List<FolderEntity>): List<Folder> = entities.map { toDomainModel(it) }
}

object BrowserUsageStatMapper  {
    fun toDomainModel(entity: BrowserUsageStatEntity): BrowserUsageStat = BrowserUsageStat(
        browserPackageName = entity.browserPackageName,
        usageCount = entity.usageCount,
        lastUsedTimestamp = entity.lastUsedTimestamp
    )

    fun toEntity(model: BrowserUsageStat): BrowserUsageStatEntity = BrowserUsageStatEntity(
        browserPackageName = model.browserPackageName,
        usageCount = model.usageCount,
        lastUsedTimestamp = model.lastUsedTimestamp
    )

    fun toDomainModels(entities: List<BrowserUsageStatEntity>): List<BrowserUsageStat> = entities.map { toDomainModel(it) }
}
