package browserpicker.data.local.di

import android.content.Context
import androidx.room.Room
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.SystemClockInstantProvider
import browserpicker.data.local.datasource.*
import browserpicker.data.local.db.*
import browserpicker.data.local.query.UriRecordQueryBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    // Bind implementations to their interfaces
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

    @Binds
    @Singleton
    abstract fun bindInstantProvider(impl: SystemClockInstantProvider): InstantProvider
}
