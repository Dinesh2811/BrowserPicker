package browserpicker.data.local.di

import browserpicker.data.local.repository.BrowserStatsRepositoryImpl
import browserpicker.data.local.repository.FolderRepositoryImpl
import browserpicker.data.local.repository.HostRuleRepositoryImpl
import browserpicker.data.local.repository.UriHistoryRepositoryImpl
import browserpicker.domain.repository.BrowserStatsRepository
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.UriHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BrowserPickerRepositoryModule {
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
