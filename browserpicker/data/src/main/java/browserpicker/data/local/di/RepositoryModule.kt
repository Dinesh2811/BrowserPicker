package browserpicker.data.local.di

import browserpicker.data.local.dao.BrowserUsageStatDao
import browserpicker.data.local.dao.FolderDao
import browserpicker.data.local.dao.HostRuleDao
import browserpicker.data.local.dao.UriRecordDao
import browserpicker.data.local.repository.*
import browserpicker.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

//    @Binds
//    @Singleton
//    abstract fun bindBrowserPickerRepository(impl: BrowserPickerRepositoryImpl): BrowserPickerRepository

    @Binds
    @Singleton
    abstract fun bindUriHistoryRepository(repositoryImpl: UriHistoryRepositoryImpl): UriHistoryRepository

    @Binds
    @Singleton
    abstract fun bindHostRuleRepository(repositoryImpl: HostRuleRepositoryImpl): HostRuleRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(repositoryImpl: FolderRepositoryImpl): FolderRepository

    @Binds
    @Singleton
    abstract fun bindBrowserStatsRepository(repositoryImpl: BrowserStatsRepositoryImpl): BrowserStatsRepository
}
