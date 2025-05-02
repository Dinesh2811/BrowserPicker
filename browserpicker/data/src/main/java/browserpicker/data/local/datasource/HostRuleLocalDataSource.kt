package browserpicker.data.local.datasource

import browserpicker.data.local.dao.HostRuleDao
import browserpicker.data.local.entity.HostRuleEntity
import browserpicker.data.local.mapper.HostRuleMapper
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface HostRuleLocalDataSource {
    fun getHostRuleByHost(host: String): Flow<HostRuleEntity?>
    suspend fun getHostRuleById(id: Long): HostRuleEntity?
    suspend fun upsertHostRule(rule: HostRuleEntity): Long
    fun getAllHostRules(): Flow<List<HostRuleEntity>>
    fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRuleEntity>>
    fun getHostRulesByFolder(folderId: Long): Flow<List<HostRuleEntity>>
    fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRuleEntity>>
    suspend fun deleteHostRuleById(id: Long): Boolean
    suspend fun deleteHostRuleByHost(host: String): Boolean
    suspend fun clearFolderIdForRules(folderId: Long)
    fun getDistinctRuleHosts(): Flow<List<String>>
}

@Singleton
class HostRuleLocalDataSourceImpl @Inject constructor(
    private val hostRuleDao: HostRuleDao,
): HostRuleLocalDataSource {

    override fun getHostRuleByHost(host: String): Flow<HostRuleEntity?> {
        return hostRuleDao.getHostRuleByHost(host)
    }

    override suspend fun getHostRuleById(id: Long): HostRuleEntity? {
        return hostRuleDao.getHostRuleById(id)
    }

    override suspend fun upsertHostRule(rule: HostRuleEntity): Long {
        return hostRuleDao.upsertHostRule(rule)
    }

    override fun getAllHostRules(): Flow<List<HostRuleEntity>> {
        return hostRuleDao.getAllHostRules()
    }

    override fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRuleEntity>> {
        return hostRuleDao.getHostRulesByStatus(status)
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<List<HostRuleEntity>> {
        return hostRuleDao.getHostRulesByFolderId(folderId)
    }

    override fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRuleEntity>> {
        return hostRuleDao.getRootHostRulesByStatus(status)
    }

    override suspend fun deleteHostRuleById(id: Long): Boolean {
        return hostRuleDao.deleteHostRuleById(id) > 0
    }

    override suspend fun deleteHostRuleByHost(host: String): Boolean {
        return hostRuleDao.deleteHostRuleByHost(host) > 0
    }

    override suspend fun clearFolderIdForRules(folderId: Long) {
        hostRuleDao.clearFolderIdForRules(folderId)
    }

    override fun getDistinctRuleHosts(): Flow<List<String>> {
        return hostRuleDao.getDistinctRuleHosts()
    }
}
