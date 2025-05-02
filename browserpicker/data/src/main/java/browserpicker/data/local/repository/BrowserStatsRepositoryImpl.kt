package browserpicker.data.local.repository

import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.MyResult
import browserpicker.data.local.datasource.BrowserStatsLocalDataSource
import browserpicker.data.local.mapper.BrowserUsageStatMapper
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.repository.BrowserStatsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
