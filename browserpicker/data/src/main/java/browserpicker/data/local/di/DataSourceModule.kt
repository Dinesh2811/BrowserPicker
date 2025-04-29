package browserpicker.data.local.di

import browserpicker.data.local.datasource.BrowserStatsLocalDataSource
import browserpicker.data.local.datasource.BrowserStatsLocalDataSourceImpl
import browserpicker.data.local.datasource.FolderLocalDataSource
import browserpicker.data.local.datasource.FolderLocalDataSourceImpl
import browserpicker.data.local.datasource.HostRuleLocalDataSource
import browserpicker.data.local.datasource.HostRuleLocalDataSourceImpl
import browserpicker.data.local.datasource.UriHistoryLocalDataSource
import browserpicker.data.local.datasource.UriHistoryLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BrowserPickerDataSourceModule {
    @Binds
    @Singleton
    abstract fun bindUriHistoryLocalDataSource(impl: UriHistoryLocalDataSourceImpl): UriHistoryLocalDataSource

    @Binds
    @Singleton
    abstract fun bindHostRuleLocalDataSource(impl: HostRuleLocalDataSourceImpl): HostRuleLocalDataSource

    @Binds
    @Singleton
    abstract fun bindFolderLocalDataSource(impl: FolderLocalDataSourceImpl): FolderLocalDataSource

    @Binds
    @Singleton
    abstract fun bindBrowserStatsLocalDataSource(impl: BrowserStatsLocalDataSourceImpl): BrowserStatsLocalDataSource
}
