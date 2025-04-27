package browserpicker.data.local.di

import android.content.Context
import androidx.room.Room
import browserpicker.data.local.datasource.*
import browserpicker.data.local.db.*
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

    @Binds
    @Singleton
    abstract fun bindUriRecordLocalDataSource(dataSourceImpl: UriRecordLocalDataSourceImpl): UriRecordLocalDataSource

    @Binds
    @Singleton
    abstract fun bindHostRuleLocalDataSource(dataSourceImpl: HostRuleLocalDataSourceImpl): HostRuleLocalDataSource

    @Binds
    @Singleton
    abstract fun bindFolderLocalDataSource(dataSourceImpl: FolderLocalDataSourceImpl): FolderLocalDataSource

    @Binds
    @Singleton
    abstract fun bindBrowserUsageStatLocalDataSource(dataSourceImpl: BrowserUsageStatLocalDataSourceImpl): BrowserUsageStatLocalDataSource

//    @Binds // Binds are more efficient for concrete types if possible
//    abstract fun bindInstantConverter(converter: InstantConverter): Any // Bind to Any or specific interface if converter hierarchy exists
//
//    @Binds
//    abstract fun bindUriSourceConverter(converter: UriSourceConverter): Any
//
//    @Binds
//    abstract fun bindInteractionActionConverter(converter: InteractionActionConverter): Any
//
//    @Binds
//    abstract fun bindUriStatusConverter(converter: UriStatusConverter): Any
//
//    @Binds
//    abstract fun bindFolderTypeConverter(converter: FolderTypeConverter): Any

}
