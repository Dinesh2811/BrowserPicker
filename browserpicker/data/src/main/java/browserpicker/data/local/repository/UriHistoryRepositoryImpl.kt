package browserpicker.data.local.repository

import browserpicker.data.local.dao.BrowserUsageStatDao
import browserpicker.data.local.dao.FolderDao
import browserpicker.data.local.dao.HostRuleDao
import browserpicker.data.local.dao.UriRecordDao
import browserpicker.domain.repository.*
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UriHistoryRepositoryImpl @Inject constructor(
    private val uriRecordDao: UriRecordDao,
    private val hostRuleDao: HostRuleDao,
    private val folderDao: FolderDao,
    private val browserUsageStatDao: BrowserUsageStatDao,
    private val clock: Clock,
): UriHistoryRepository {
    //  TODO("Not yet implemented")
}

@Singleton
class HostRuleRepositoryImpl @Inject constructor(
    private val uriRecordDao: UriRecordDao,
    private val hostRuleDao: HostRuleDao,
    private val folderDao: FolderDao,
    private val browserUsageStatDao: BrowserUsageStatDao,
    private val clock: Clock,
): HostRuleRepository {
    //  TODO("Not yet implemented")
}

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val uriRecordDao: UriRecordDao,
    private val hostRuleDao: HostRuleDao,
    private val folderDao: FolderDao,
    private val browserUsageStatDao: BrowserUsageStatDao,
    private val clock: Clock,
): FolderRepository {
    //  TODO("Not yet implemented")
}

@Singleton
class BrowserStatsRepositoryImpl @Inject constructor(
    private val uriRecordDao: UriRecordDao,
    private val hostRuleDao: HostRuleDao,
    private val folderDao: FolderDao,
    private val browserUsageStatDao: BrowserUsageStatDao,
    private val clock: Clock,
): BrowserStatsRepository {
    //  TODO("Not yet implemented")
}
