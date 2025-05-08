package com.dinesh.browserpicker.v1.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.dinesh.browserpicker.v1.domain.usecases.*
import com.dinesh.browserpicker.v1.domain.usecases.impl.*
import com.dinesh.browserpicker.v1.domain.usecases.ClearPreferredBrowserForHostUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetAvailableBrowsersUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetBrowserUsageStatUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetBrowserUsageStatsUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetMostFrequentlyUsedBrowserUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetMostRecentlyUsedBrowserUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetPreferredBrowserForHostUseCase
import com.dinesh.browserpicker.v1.domain.usecases.RecordBrowserUsageUseCase
import com.dinesh.browserpicker.v1.domain.usecases.SetPreferredBrowserForHostUseCase
import javax.inject.Singleton

/**
 * Dagger-Hilt module for providing UseCase interfaces and their factory implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    // Although this is an abstract class, we can define @Provides methods here if needed
    // for cases where constructor injection is not directly applicable (e.g., interfaces
    // with abstract methods or complex instantiation logic).
    // However, for the UseCase factory interfaces, we can provide their concrete implementations.

    companion object {
        @Provides
        @Singleton
        fun provideUriHandlingUseCases(impl: UriHandlingUseCasesImpl): UriHandlingUseCases = impl

        @Provides
        @Singleton
        fun provideBrowserUseCases(impl: BrowserUseCasesImpl): BrowserUseCases = impl

        @Provides
        @Singleton
        fun provideHostRuleUseCases(impl: HostRuleUseCasesImpl): HostRuleUseCases = impl

        @Provides
        @Singleton
        fun provideUriHistoryUseCases(impl: UriHistoryUseCasesImpl): UriHistoryUseCases = impl

        @Provides
        @Singleton
        fun provideFolderUseCases(impl: FolderUseCasesImpl): FolderUseCases = impl

        @Provides
        @Singleton
        fun provideSearchAndAnalyticsUseCases(impl: SearchAndAnalyticsUseCasesImpl): SearchAndAnalyticsUseCases = impl

        @Provides
        @Singleton
        fun provideSystemIntegrationUseCases(impl: SystemIntegrationUseCasesImpl): SystemIntegrationUseCases = impl
    }
}

/**
 * Factory interface for retrieving all URI handling use cases
 */
interface UriHandlingUseCases {
    val handleUriUseCase: HandleUriUseCase
    val validateUriUseCase: ValidateUriUseCase
    val recordUriInteractionUseCase: RecordUriInteractionUseCase
    val getRecentUrisUseCase: GetRecentUrisUseCase
    val searchUrisUseCase: SearchUrisUseCase
    val cleanupUriHistoryUseCase: CleanupUriHistoryUseCase
}

/**
 * Factory interface for retrieving all browser-related use cases
 */
interface BrowserUseCases {
    val getAvailableBrowsersUseCase: GetAvailableBrowsersUseCase
    val getPreferredBrowserForHostUseCase: GetPreferredBrowserForHostUseCase
    val setPreferredBrowserForHostUseCase: SetPreferredBrowserForHostUseCase
    val clearPreferredBrowserForHostUseCase: ClearPreferredBrowserForHostUseCase
    val recordBrowserUsageUseCase: RecordBrowserUsageUseCase
    val getBrowserUsageStatsUseCase: GetBrowserUsageStatsUseCase
    val getBrowserUsageStatUseCase: GetBrowserUsageStatUseCase
    val getMostFrequentlyUsedBrowserUseCase: GetMostFrequentlyUsedBrowserUseCase
    val getMostRecentlyUsedBrowserUseCase: GetMostRecentlyUsedBrowserUseCase
}

/**
 * Factory interface for retrieving all host rule-related use cases
 */
interface HostRuleUseCases {
    val getHostRuleUseCase: GetHostRuleUseCase
    val getHostRuleByIdUseCase: GetHostRuleByIdUseCase
    val saveHostRuleUseCase: SaveHostRuleUseCase
    val deleteHostRuleUseCase: DeleteHostRuleUseCase
    val getAllHostRulesUseCase: GetAllHostRulesUseCase
    val getHostRulesByStatusUseCase: GetHostRulesByStatusUseCase
    val getHostRulesByFolderUseCase: GetHostRulesByFolderUseCase
    val getRootHostRulesByStatusUseCase: GetRootHostRulesByStatusUseCase
    val bookmarkHostUseCase: BookmarkHostUseCase
    val blockHostUseCase: BlockHostUseCase
    val clearHostStatusUseCase: ClearHostStatusUseCase
}

/**
 * Factory interface for retrieving all URI history-related use cases
 */
interface UriHistoryUseCases {
    val getPagedUriHistoryUseCase: GetPagedUriHistoryUseCase
    val getUriHistoryCountUseCase: GetUriHistoryCountUseCase
    val getUriHistoryGroupCountsUseCase: GetUriHistoryGroupCountsUseCase
    val getUriHistoryDateCountsUseCase: GetUriHistoryDateCountsUseCase
    val getUriRecordByIdUseCase: GetUriRecordByIdUseCase
    val deleteUriRecordUseCase: DeleteUriRecordUseCase
    val deleteAllUriHistoryUseCase: DeleteAllUriHistoryUseCase
    val getUriFilterOptionsUseCase: GetUriFilterOptionsUseCase
    val exportUriHistoryUseCase: ExportUriHistoryUseCase
    val importUriHistoryUseCase: ImportUriHistoryUseCase
}

/**
 * Factory interface for retrieving all folder management use cases
 */
interface FolderUseCases {
    val getFolderUseCase: GetFolderUseCase
    val getChildFoldersUseCase: GetChildFoldersUseCase
    val getRootFoldersUseCase: GetRootFoldersUseCase
    val getAllFoldersByTypeUseCase: GetAllFoldersByTypeUseCase
    val findFolderByNameAndParentUseCase: FindFolderByNameAndParentUseCase
    val createFolderUseCase: CreateFolderUseCase
    val updateFolderUseCase: UpdateFolderUseCase
    val deleteFolderUseCase: DeleteFolderUseCase
    val moveFolderUseCase: MoveFolderUseCase
    val moveHostRuleToFolderUseCase: MoveHostRuleToFolderUseCase
    val getFolderHierarchyUseCase: GetFolderHierarchyUseCase
    val ensureDefaultFoldersExistUseCase: EnsureDefaultFoldersExistUseCase
}

/**
 * Factory interface for retrieving all search and analytics use cases
 */
interface SearchAndAnalyticsUseCases {
    val analyzeUriTrendsUseCase: AnalyzeUriTrendsUseCase
    val analyzeBrowserUsageTrendsUseCase: AnalyzeBrowserUsageTrendsUseCase
    val getMostVisitedHostsUseCase: GetMostVisitedHostsUseCase
    val getTopActionsByHostUseCase: GetTopActionsByHostUseCase
    val searchHostRulesUseCase: SearchHostRulesUseCase
    val searchFoldersUseCase: SearchFoldersUseCase
    val generateHistoryReportUseCase: GenerateHistoryReportUseCase
    val generateBrowserUsageReportUseCase: GenerateBrowserUsageReportUseCase
}

/**
 * Factory interface for retrieving all system integration use cases
 */
interface SystemIntegrationUseCases {
    val checkDefaultBrowserStatusUseCase: CheckDefaultBrowserStatusUseCase
    val openBrowserPreferencesUseCase: OpenBrowserPreferencesUseCase
    val monitorUriClipboardUseCase: MonitorUriClipboardUseCase
    val shareUriUseCase: ShareUriUseCase
    val openUriInBrowserUseCase: OpenUriInBrowserUseCase
    val setAsDefaultBrowserUseCase: SetAsDefaultBrowserUseCase
    val backupDataUseCase: BackupDataUseCase
    val restoreDataUseCase: RestoreDataUseCase
    val monitorSystemBrowserChangesUseCase: MonitorSystemBrowserChangesUseCase
    val handleUncaughtUriUseCase: HandleUncaughtUriUseCase
} 