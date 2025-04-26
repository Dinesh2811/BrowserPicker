package browserpicker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import browserpicker.data.local.dao.BlockFolderDao
import browserpicker.data.local.dao.BookmarkFolderDao
import browserpicker.data.local.dao.HostRuleDao
import browserpicker.data.local.dao.UriRecordDao
import browserpicker.data.local.entity.BlockFolderEntity
import browserpicker.data.local.entity.BookmarkFolderEntity
import browserpicker.data.local.entity.HostRuleEntity
import browserpicker.data.local.entity.UriRecordEntity

@Database(
    entities = [
        UriRecordEntity::class,
        HostRuleEntity::class,
        BookmarkFolderEntity::class,
        BlockFolderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
    UriSourceConverter::class,
    InteractionActionConverter::class,
    RuleTypeConverter::class
    // Add future converters here
)
abstract class BrowserPickerDatabase : RoomDatabase() {

    abstract fun uriRecordDao(): UriRecordDao
    abstract fun hostRuleDao(): HostRuleDao
    abstract fun bookmarkFolderDao(): BookmarkFolderDao
    abstract fun blockFolderDao(): BlockFolderDao

    companion object {
        const val DATABASE_NAME = "browser_picker_database"
    }
}
