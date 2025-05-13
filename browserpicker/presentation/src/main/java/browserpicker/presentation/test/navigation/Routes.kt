package browserpicker.presentation.test.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class BrowserPickerRoute(val uriString: String? = null) // Default null matches original NavHost argument

@Serializable
object UriHistoryRoute

@Serializable
object BookmarksRoute

@Serializable
object BlockedUrlsRoute

@Serializable
data class FolderDetailsRoute(val folderId: Long, val folderType: Int)

@Serializable
data class UriDetailsRoute(val uriRecordId: Long)

@Serializable
object BrowserAnalyticsRoute // Renamed from BROWSER_STATS for consistency with screen name

@Serializable
data class HostRuleDetailsRoute(val hostRuleId: Long)

@Serializable
object SearchRoute

@Serializable
object SettingsRoute

@Serializable
object UriAnalyticsRoute