package browserpicker.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination

@Serializable
data object HomeRoute: AppDestination

@Serializable
data object UriHistoryRoute: AppDestination {
    @Serializable
    data object NestedRoute1: AppDestination
    @Serializable
    data object NestedRoute2: AppDestination
    @Serializable
    data object NestedRoute3: AppDestination
}

@Serializable
data object PreferencesRoute: AppDestination {
    @Serializable
    data object NestedRoute1: AppDestination
    @Serializable
    data object NestedRoute2: AppDestination
    @Serializable
    data object NestedRoute3: AppDestination
}

@Serializable
data object FolderDetailsRoute: AppDestination {
    @Serializable
    data object NestedRoute1: AppDestination
    @Serializable
    data object NestedRoute2: AppDestination
    @Serializable
    data object NestedRoute3: AppDestination
}

@Serializable
data object BrowserAnalyticsRoute: AppDestination

// You might add more complex destinations with arguments later, like:
// @Serializable
// data class UserProfileRoute(val userId: String) : AppDestination
