package browserpicker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import browserpicker.data.local.model.BlockFolderEntity
import browserpicker.data.local.model.BookmarkFolderEntity
import browserpicker.data.local.model.HostRuleEntity
import browserpicker.data.local.model.UriRecordEntity

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

//    // Abstract DAO getters - Room generates the implementation
//    abstract fun uriRecordDao(): UriRecordDao
//    abstract fun hostRuleDao(): HostRuleDao
//    abstract fun bookmarkFolderDao(): BookmarkFolderDao
//    abstract fun blockFolderDao(): BlockFolderDao

    companion object {
        const val DATABASE_NAME = "browser_picker_database"
    }
}
