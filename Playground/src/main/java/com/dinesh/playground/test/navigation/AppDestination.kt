package com.dinesh.playground.test.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination

@Serializable
data object HomeScreen: AppDestination

@Serializable
data object UriHistoryScreen: AppDestination {
    @Serializable
    data object NestedScreen1: AppDestination
    @Serializable
    data object NestedScreen2: AppDestination
    @Serializable
    data object NestedScreen3: AppDestination
}

@Serializable
data object PreferencesScreen: AppDestination {
    @Serializable
    data object NestedScreen1: AppDestination
    @Serializable
    data object NestedScreen2: AppDestination
    @Serializable
    data object NestedScreen3: AppDestination
}

@Serializable
data object FolderDetailsScreen: AppDestination {
    @Serializable
    data object NestedScreen1: AppDestination
    @Serializable
    data object NestedScreen2: AppDestination
    @Serializable
    data object NestedScreen3: AppDestination
}

@Serializable
data object BrowserAnalyticsScreen: AppDestination

// You might add more complex destinations with arguments later, like:
// @Serializable
// data class UserProfileScreen(val userId: String) : AppDestination
