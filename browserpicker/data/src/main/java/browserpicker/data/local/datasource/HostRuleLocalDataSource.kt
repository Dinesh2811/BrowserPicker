package browserpicker.data.local.datasource

import browserpicker.data.local.dao.HostRuleDao
import browserpicker.data.local.mapper.HostRuleMapper
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface HostRuleLocalDataSource {
    fun getHostRuleByHost(host: String): Flow<HostRule?>
    suspend fun getHostRuleById(id: Long): HostRule?
    suspend fun upsertHostRule(rule: HostRule): Long
    fun getAllHostRules(): Flow<List<HostRule>>
    fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>>
    fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>>
    fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>>
    suspend fun deleteHostRuleById(id: Long): Boolean
    suspend fun deleteHostRuleByHost(host: String): Boolean
    suspend fun clearFolderIdForRules(folderId: Long)
    fun getDistinctRuleHosts(): Flow<List<String>>
}

@Singleton
class HostRuleLocalDataSourceImpl @Inject constructor(
    private val hostRuleDao: HostRuleDao,
): HostRuleLocalDataSource {

    override fun getHostRuleByHost(host: String): Flow<HostRule?> {
        return hostRuleDao.getHostRuleByHost(host).map { entity ->
            entity?.let { HostRuleMapper.toDomainModel(it) }
        }
    }

    override suspend fun getHostRuleById(id: Long): HostRule? {
        return hostRuleDao.getHostRuleById(id)?.let { HostRuleMapper.toDomainModel(it) }
    }

    override suspend fun upsertHostRule(rule: HostRule): Long {
        return hostRuleDao.upsertHostRule(HostRuleMapper.toEntity(rule))
    }

    override fun getAllHostRules(): Flow<List<HostRule>> {
        return hostRuleDao.getAllHostRules().map { HostRuleMapper.toDomainModels(it) }
    }

    override fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDao.getHostRulesByStatus(status).map { HostRuleMapper.toDomainModels(it) }
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>> {
        return hostRuleDao.getHostRulesByFolderId(folderId).map { HostRuleMapper.toDomainModels(it) }
    }

    override fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDao.getRootHostRulesByStatus(status).map { HostRuleMapper.toDomainModels(it) }
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
