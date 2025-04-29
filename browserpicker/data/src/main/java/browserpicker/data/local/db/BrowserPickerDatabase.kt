package browserpicker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import browserpicker.data.local.dao.BrowserUsageStatDao
import browserpicker.data.local.dao.FolderDao
import browserpicker.data.local.dao.HostRuleDao
import browserpicker.data.local.dao.MockBrowserPickerDatabaseDao
import browserpicker.data.local.dao.UriRecordDao
import browserpicker.data.local.entity.BrowserUsageStatEntity
import browserpicker.data.local.entity.FolderEntity
import browserpicker.data.local.entity.HostRuleEntity
import browserpicker.data.local.entity.UriRecordEntity

@Database(
    entities = [
        UriRecordEntity::class,
        HostRuleEntity::class,
        FolderEntity::class,
        BrowserUsageStatEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BrowserPickerDatabase : RoomDatabase() {

    abstract fun uriRecordDao(): UriRecordDao
    abstract fun hostRuleDao(): HostRuleDao
    abstract fun folderDao(): FolderDao
    abstract fun browserUsageStatDao(): BrowserUsageStatDao
    abstract fun mockBrowserPickerDatabaseDao(): MockBrowserPickerDatabaseDao
}
