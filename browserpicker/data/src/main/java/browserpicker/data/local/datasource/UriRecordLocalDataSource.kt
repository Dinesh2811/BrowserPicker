package browserpicker.data.local.datasource

import browserpicker.data.local.dao.BrowserUsageStatDao
import browserpicker.data.local.dao.FolderDao
import browserpicker.data.local.dao.HostRuleDao
import browserpicker.data.local.dao.UriRecordDao
import javax.inject.Inject
import javax.inject.Singleton

interface UriRecordLocalDataSource {
    //  TODO("Not yet implemented")
}

interface HostRuleLocalDataSource {
    //  TODO("Not yet implemented")
}

interface FolderLocalDataSource {
    //  TODO("Not yet implemented")
}

interface BrowserUsageStatLocalDataSource {
    //  TODO("Not yet implemented")
}

@Singleton // Scope based on your DI setup
class UriRecordLocalDataSourceImpl @Inject constructor(
    private val uriRecordDao: UriRecordDao,
): UriRecordLocalDataSource {
    //  TODO("Not yet implemented")
}

@Singleton
class HostRuleLocalDataSourceImpl @Inject constructor(
    private val hostRuleDao: HostRuleDao,
): HostRuleLocalDataSource {
    //  TODO("Not yet implemented")
}

@Singleton
class FolderLocalDataSourceImpl @Inject constructor(
    private val folderDao: FolderDao,
    private val hostRuleDao: HostRuleDao,
): FolderLocalDataSource {
    //  TODO("Not yet implemented")
}

@Singleton
class BrowserUsageStatLocalDataSourceImpl @Inject constructor(
    private val browserUsageStatDao: BrowserUsageStatDao,
): BrowserUsageStatLocalDataSource {
    //  TODO("Not yet implemented")
}