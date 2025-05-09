package browserpicker.domain.di

import browserpicker.domain.usecases.AnalyzeBrowserUsageTrendsUseCase
import browserpicker.domain.usecases.AnalyzeUriTrendsUseCase
import browserpicker.domain.usecases.BackupDataUseCase
import browserpicker.domain.usecases.BlockHostUseCase
import browserpicker.domain.usecases.BookmarkHostUseCase
import browserpicker.domain.usecases.CheckDefaultBrowserStatusUseCase
import browserpicker.domain.usecases.CleanupUriHistoryUseCase
import browserpicker.domain.usecases.ClearHostStatusUseCase
import browserpicker.domain.usecases.CreateFolderUseCase
import browserpicker.domain.usecases.DeleteAllUriHistoryUseCase
import browserpicker.domain.usecases.DeleteFolderUseCase
import browserpicker.domain.usecases.DeleteHostRuleUseCase
import browserpicker.domain.usecases.DeleteUriRecordUseCase
import browserpicker.domain.usecases.EnsureDefaultFoldersExistUseCase
import browserpicker.domain.usecases.ExportUriHistoryUseCase
import browserpicker.domain.usecases.FindFolderByNameAndParentUseCase
import browserpicker.domain.usecases.GetAllFoldersByTypeUseCase
import browserpicker.domain.usecases.GetAllHostRulesUseCase
import browserpicker.domain.usecases.GetChildFoldersUseCase
import browserpicker.domain.usecases.GetFolderHierarchyUseCase
import browserpicker.domain.usecases.GetFolderUseCase
import browserpicker.domain.usecases.GetHostRuleByIdUseCase
import browserpicker.domain.usecases.GetHostRuleUseCase
import browserpicker.domain.usecases.GetHostRulesByFolderUseCase
import browserpicker.domain.usecases.GetHostRulesByStatusUseCase
import browserpicker.domain.usecases.GetPagedUriHistoryUseCase
import browserpicker.domain.usecases.GetRecentUrisUseCase
import browserpicker.domain.usecases.GetRootFoldersUseCase
import browserpicker.domain.usecases.GetRootHostRulesByStatusUseCase
import browserpicker.domain.usecases.GetUriFilterOptionsUseCase
import browserpicker.domain.usecases.GetUriHistoryCountUseCase
import browserpicker.domain.usecases.GetUriHistoryDateCountsUseCase
import browserpicker.domain.usecases.GetUriHistoryGroupCountsUseCase
import browserpicker.domain.usecases.GetUriRecordByIdUseCase
import browserpicker.domain.usecases.HandleUncaughtUriUseCase
import browserpicker.domain.usecases.HandleUriUseCase
import browserpicker.domain.usecases.ImportUriHistoryUseCase
import browserpicker.domain.usecases.MonitorSystemBrowserChangesUseCase
import browserpicker.domain.usecases.MonitorUriClipboardUseCase
import browserpicker.domain.usecases.MoveFolderUseCase
import browserpicker.domain.usecases.MoveHostRuleToFolderUseCase
import browserpicker.domain.usecases.OpenBrowserPreferencesUseCase
import browserpicker.domain.usecases.OpenUriInBrowserUseCase
import browserpicker.domain.usecases.RecordUriInteractionUseCase
import browserpicker.domain.usecases.RestoreDataUseCase
import browserpicker.domain.usecases.SaveHostRuleUseCase
import browserpicker.domain.usecases.SearchUrisUseCase
import browserpicker.domain.usecases.SetAsDefaultBrowserUseCase
import browserpicker.domain.usecases.ShareUriUseCase
import browserpicker.domain.usecases.UpdateFolderUseCase
import browserpicker.domain.usecases.ValidateUriUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import browserpicker.domain.usecases.ClearPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.GenerateBrowserUsageReportUseCase
import browserpicker.domain.usecases.GenerateHistoryReportUseCase
import browserpicker.domain.usecases.GetAvailableBrowsersUseCase
import browserpicker.domain.usecases.GetBrowserUsageStatUseCase
import browserpicker.domain.usecases.GetBrowserUsageStatsUseCase
import browserpicker.domain.usecases.GetMostFrequentlyUsedBrowserUseCase
import browserpicker.domain.usecases.GetMostRecentlyUsedBrowserUseCase
import browserpicker.domain.usecases.GetMostVisitedHostsUseCase
import browserpicker.domain.usecases.GetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.GetTopActionsByHostUseCase
import browserpicker.domain.usecases.RecordBrowserUsageUseCase
import browserpicker.domain.usecases.SearchFoldersUseCase
import browserpicker.domain.usecases.SearchHostRulesUseCase
import browserpicker.domain.usecases.SetPreferredBrowserForHostUseCase
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
//    val deleteBrowserStatUseCase: DeleteBrowserStatUseCase
//    val deleteAllStatsUseCase: DeleteAllStatsUseCase
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