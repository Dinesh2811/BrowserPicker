package browserpicker.playground.browserpicker.data.local.datasource

import android.content.pm.ResolveInfo
import browserpicker.playground.browserpicker.core.common.utils.AppError
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for accessing raw browser information from the underlying system.
 */
interface BrowserInfoDataSource {
    /**
     * Retrieves a list of ResolveInfo objects representing activities that can handle
     * a generic web intent.
     *
     * @return A Flow emitting a Result containing either a list of ResolveInfo or a DataSourceError.
     */
//    fun getInstalledBrowserActivities(): Flow<Result<List<ResolveInfo>, AppError.DataSourceError>>
}