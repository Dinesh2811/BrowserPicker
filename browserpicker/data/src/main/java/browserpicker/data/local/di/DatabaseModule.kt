package browserpicker.data.local.di

import android.content.Context
import androidx.room.Room
import browserpicker.data.local.dao.*
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.data.local.db.InstantConverter
import browserpicker.data.local.db.InteractionActionConverter
import browserpicker.data.local.db.RuleTypeConverter
import browserpicker.data.local.db.UriSourceConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        instantConverter: InstantConverter,
        uriSourceConverter: UriSourceConverter,
        interactionActionConverter: InteractionActionConverter,
        ruleTypeConverter: RuleTypeConverter,
    ): BrowserPickerDatabase {
        return Room.databaseBuilder(
            context,
            BrowserPickerDatabase::class.java,
            BrowserPickerDatabase.DATABASE_NAME
        )
            .addTypeConverter(instantConverter)
            .addTypeConverter(uriSourceConverter)
            .addTypeConverter(interactionActionConverter)
            .addTypeConverter(ruleTypeConverter)
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
    fun provideBookmarkFolderDao(database: BrowserPickerDatabase): BookmarkFolderDao {
        return database.bookmarkFolderDao()
    }

    @Provides
    @Singleton
    fun provideBlockFolderDao(database: BrowserPickerDatabase): BlockFolderDao {
        return database.blockFolderDao()
    }
}
