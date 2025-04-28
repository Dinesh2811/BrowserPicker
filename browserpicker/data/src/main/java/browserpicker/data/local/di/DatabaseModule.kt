package browserpicker.data.local.di

import android.content.Context
import androidx.room.Room
import browserpicker.data.local.dao.*
import browserpicker.data.local.db.BrowserPickerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowserPickerDatabaseModule {
    private const val DATABASE_NAME = "browser_picker_database"

    @Provides
    @Singleton
    fun provideBrowserPickerDatabase(@ApplicationContext context: Context): BrowserPickerDatabase {
        return Room.databaseBuilder(
            context,
            BrowserPickerDatabase::class.java,
            DATABASE_NAME
        )
            // IMPORTANT: Use proper migrations in a real app!
            // .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration(false)
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
}
