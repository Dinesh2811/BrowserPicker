package browserpicker.domain.di

import browserpicker.domain.usecases.analytics.AnalyzeBrowserUsageTrendsUseCase
import browserpicker.domain.usecases.analytics.AnalyzeUriStatusChangesUseCase
import browserpicker.domain.usecases.analytics.AnalyzeUriTrendsUseCase
import browserpicker.domain.usecases.system.BackupDataUseCase
import browserpicker.domain.usecases.system.CheckDefaultBrowserStatusUseCase
import browserpicker.domain.usecases.uri.shared.CleanupUriHistoryUseCase
import browserpicker.domain.usecases.uri.host.ClearHostStatusUseCase
import browserpicker.domain.usecases.folder.CreateFolderUseCase
import browserpicker.domain.usecases.uri.history.DeleteAllUriHistoryUseCase
import browserpicker.domain.usecases.folder.DeleteFolderUseCase
import browserpicker.domain.usecases.uri.host.DeleteHostRuleUseCase
import browserpicker.domain.usecases.uri.history.DeleteUriRecordUseCase
import browserpicker.domain.usecases.folder.EnsureDefaultFoldersExistUseCase
import browserpicker.domain.usecases.uri.history.ExportUriHistoryUseCase
import browserpicker.domain.usecases.folder.FindFolderByNameAndParentUseCase
import browserpicker.domain.usecases.folder.GetAllFoldersByTypeUseCase
import browserpicker.domain.usecases.uri.host.GetAllHostRulesUseCase
import browserpicker.domain.usecases.folder.GetChildFoldersUseCase
import browserpicker.domain.usecases.folder.GetFolderHierarchyUseCase
import browserpicker.domain.usecases.folder.GetFolderUseCase
import browserpicker.domain.usecases.uri.host.GetHostRuleByIdUseCase
import browserpicker.domain.usecases.uri.host.GetHostRuleUseCase
import browserpicker.domain.usecases.uri.host.GetHostRulesByFolderUseCase
import browserpicker.domain.usecases.uri.host.GetHostRulesByStatusUseCase
import browserpicker.domain.usecases.uri.history.GetPagedUriHistoryUseCase
import browserpicker.domain.usecases.uri.shared.GetRecentUrisUseCase
import browserpicker.domain.usecases.folder.GetRootFoldersUseCase
import browserpicker.domain.usecases.uri.history.GetUriFilterOptionsUseCase
import browserpicker.domain.usecases.uri.history.GetUriHistoryCountUseCase
import browserpicker.domain.usecases.uri.history.GetUriHistoryDateCountsUseCase
import browserpicker.domain.usecases.uri.history.GetUriHistoryGroupCountsUseCase
import browserpicker.domain.usecases.uri.history.GetUriRecordByIdUseCase
import browserpicker.domain.usecases.system.HandleUncaughtUriUseCase
import browserpicker.domain.usecases.uri.shared.HandleUriUseCase
import browserpicker.domain.usecases.uri.history.ImportUriHistoryUseCase
import browserpicker.domain.usecases.system.MonitorSystemBrowserChangesUseCase
import browserpicker.domain.usecases.system.MonitorUriClipboardUseCase
import browserpicker.domain.usecases.folder.*
import browserpicker.domain.usecases.system.OpenBrowserPreferencesUseCase
import browserpicker.domain.usecases.system.OpenUriInBrowserUseCase
import browserpicker.domain.usecases.uri.shared.RecordUriInteractionUseCase
import browserpicker.domain.usecases.system.RestoreDataUseCase
import browserpicker.domain.usecases.uri.host.SaveHostRuleUseCase
import browserpicker.domain.usecases.system.SetAsDefaultBrowserUseCase
import browserpicker.domain.usecases.system.ShareUriUseCase
import browserpicker.domain.usecases.folder.UpdateFolderUseCase
import browserpicker.domain.usecases.uri.shared.ValidateUriUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import browserpicker.domain.usecases.browser.ClearPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.analytics.GenerateBrowserUsageReportUseCase
import browserpicker.domain.usecases.analytics.GenerateHistoryReportUseCase
import browserpicker.domain.usecases.browser.GetAvailableBrowsersUseCase
import browserpicker.domain.usecases.browser.GetBrowserUsageStatUseCase
import browserpicker.domain.usecases.browser.GetBrowserUsageStatsUseCase
import browserpicker.domain.usecases.browser.GetMostFrequentlyUsedBrowserUseCase
import browserpicker.domain.usecases.browser.GetMostRecentlyUsedBrowserUseCase
import browserpicker.domain.usecases.analytics.GetMostVisitedHostsUseCase
import browserpicker.domain.usecases.browser.GetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.analytics.GetTopActionsByHostUseCase
import browserpicker.domain.usecases.browser.RecordBrowserUsageUseCase
import browserpicker.domain.usecases.analytics.SearchFoldersUseCase
import browserpicker.domain.usecases.analytics.SearchHostRulesUseCase
import browserpicker.domain.usecases.analytics.TrackUriActionUseCase
import browserpicker.domain.usecases.browser.SetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.uri.host.CheckUriStatusUseCase
import browserpicker.domain.usecases.uri.host.UpdateHostRuleStatusUseCase
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
    val checkUriStatusUseCase: CheckUriStatusUseCase
    val getHostRulesByFolderUseCase: GetHostRulesByFolderUseCase
//    val getRootHostRulesByStatusUseCase: GetRootHostRulesByStatusUseCase
//    val bookmarkHostUseCase: BookmarkHostUseCase
//    val blockHostUseCase: BlockHostUseCase
    val clearHostStatusUseCase: ClearHostStatusUseCase
    val updateHostRuleStatusUseCase: UpdateHostRuleStatusUseCase
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
//    val getDistinctHistoryHostsUseCase: GetDistinctHistoryHostsUseCase
//    val getDistinctChosenBrowsersUseCase: GetDistinctChosenBrowsersUseCase
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
//    val moveFolderUseCase: MoveFolderUseCase
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
    val trackUriActionUseCase: TrackUriActionUseCase
    val analyzeUriStatusChangesUseCase: AnalyzeUriStatusChangesUseCase
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