package browserpicker.data.local.di

import android.content.Context
import androidx.room.Room
import browserpicker.data.local.dao.*
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.data.local.db.FolderTypeConverter
import browserpicker.data.local.db.InstantConverter
import browserpicker.data.local.db.InteractionActionConverter
import browserpicker.data.local.db.UriSourceConverter
import browserpicker.data.local.db.UriStatusConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowserPickerDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        instantConverter: InstantConverter,
        uriSourceConverter: UriSourceConverter,
        interactionActionConverter: InteractionActionConverter,
        uriStatusConverter: UriStatusConverter,
        folderTypeConverter: FolderTypeConverter,
    ): BrowserPickerDatabase {
        return Room.databaseBuilder(
            context,
            BrowserPickerDatabase::class.java,
            BrowserPickerDatabase.DATABASE_NAME
        )
            .addTypeConverter(instantConverter)
            .addTypeConverter(uriSourceConverter)
            .addTypeConverter(interactionActionConverter)
            .addTypeConverter(uriStatusConverter)
            .addTypeConverter(folderTypeConverter)
            .fallbackToDestructiveMigration(false)
            // Consider adding .setQueryCallback for logging/debugging during development if needed
            .build()
    }

    @Provides
    @Singleton
    fun provideUriRecordDao(database: BrowserPickerDatabase): UriRecordDao {
        return database.uriRecordDao()
    }

    @Provides
    @Singleton
    fun provideHostRuleDao(database: BrowserPickerDatabase): HostRuleDao {
        return database.hostRuleDao()
    }

    @Provides
    @Singleton
    fun provideFolderDao(database: BrowserPickerDatabase): FolderDao {
        return database.folderDao()
    }

    @Provides
    @Singleton
    fun provideBrowserUsageStatDao(database: BrowserPickerDatabase): BrowserUsageStatDao {
        return database.browserUsageStatDao()
    }


//    @Provides
//    @Singleton
//    fun provideBookmarkFolderDao(database: BrowserPickerDatabase): BookmarkFolderDao {
//        return database.bookmarkFolderDao()
//    }
//
//    @Provides
//    @Singleton
//    fun provideBlockFolderDao(database: BrowserPickerDatabase): BlockFolderDao {
//        return database.blockFolderDao()
//    }
}
