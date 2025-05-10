package browserpicker.domain.di

import browserpicker.domain.usecases.analytics.AnalyzeBrowserUsageTrendsUseCase
import browserpicker.domain.usecases.analytics.AnalyzeUriTrendsUseCase
import browserpicker.domain.usecases.system.BackupDataUseCase
import browserpicker.domain.usecases.uri.BlockHostUseCase
import browserpicker.domain.usecases.uri.BookmarkHostUseCase
import browserpicker.domain.usecases.system.CheckDefaultBrowserStatusUseCase
import browserpicker.domain.usecases.uri.CleanupUriHistoryUseCase
import browserpicker.domain.usecases.uri.ClearHostStatusUseCase
import browserpicker.domain.usecases.browser.ClearPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.folder.CreateFolderUseCase
import browserpicker.domain.usecases.uri.DeleteAllUriHistoryUseCase
import browserpicker.domain.usecases.folder.DeleteFolderUseCase
import browserpicker.domain.usecases.uri.DeleteHostRuleUseCase
import browserpicker.domain.usecases.uri.DeleteUriRecordUseCase
import browserpicker.domain.usecases.folder.EnsureDefaultFoldersExistUseCase
import browserpicker.domain.usecases.uri.ExportUriHistoryUseCase
import browserpicker.domain.usecases.folder.FindFolderByNameAndParentUseCase
import browserpicker.domain.usecases.analytics.GenerateBrowserUsageReportUseCase
import browserpicker.domain.usecases.analytics.GenerateHistoryReportUseCase
import browserpicker.domain.usecases.folder.GetAllFoldersByTypeUseCase
import browserpicker.domain.usecases.uri.GetAllHostRulesUseCase
import browserpicker.domain.usecases.browser.GetAvailableBrowsersUseCase
import browserpicker.domain.usecases.browser.GetBrowserUsageStatUseCase
import browserpicker.domain.usecases.browser.GetBrowserUsageStatsUseCase
import browserpicker.domain.usecases.folder.GetChildFoldersUseCase
import browserpicker.domain.usecases.folder.GetFolderHierarchyUseCase
import browserpicker.domain.usecases.folder.GetFolderUseCase
import browserpicker.domain.usecases.uri.GetHostRuleByIdUseCase
import browserpicker.domain.usecases.uri.GetHostRuleUseCase
import browserpicker.domain.usecases.uri.GetHostRulesByFolderUseCase
import browserpicker.domain.usecases.uri.GetHostRulesByStatusUseCase
import browserpicker.domain.usecases.browser.GetMostFrequentlyUsedBrowserUseCase
import browserpicker.domain.usecases.browser.GetMostRecentlyUsedBrowserUseCase
import browserpicker.domain.usecases.analytics.GetMostVisitedHostsUseCase
import browserpicker.domain.usecases.uri.GetPagedUriHistoryUseCase
import browserpicker.domain.usecases.browser.GetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.uri.GetRecentUrisUseCase
import browserpicker.domain.usecases.folder.GetRootFoldersUseCase
import browserpicker.domain.usecases.uri.GetRootHostRulesByStatusUseCase
import browserpicker.domain.usecases.analytics.GetTopActionsByHostUseCase
import browserpicker.domain.usecases.uri.GetUriFilterOptionsUseCase
import browserpicker.domain.usecases.uri.GetUriHistoryCountUseCase
import browserpicker.domain.usecases.uri.GetUriHistoryDateCountsUseCase
import browserpicker.domain.usecases.uri.GetUriHistoryGroupCountsUseCase
import browserpicker.domain.usecases.uri.GetUriRecordByIdUseCase
import browserpicker.domain.usecases.system.HandleUncaughtUriUseCase
import browserpicker.domain.usecases.uri.HandleUriUseCase
import browserpicker.domain.usecases.uri.ImportUriHistoryUseCase
import browserpicker.domain.usecases.system.MonitorSystemBrowserChangesUseCase
import browserpicker.domain.usecases.system.MonitorUriClipboardUseCase
import browserpicker.domain.usecases.folder.MoveFolderUseCase
import browserpicker.domain.usecases.folder.MoveHostRuleToFolderUseCase
import browserpicker.domain.usecases.system.OpenBrowserPreferencesUseCase
import browserpicker.domain.usecases.system.OpenUriInBrowserUseCase
import browserpicker.domain.usecases.browser.RecordBrowserUsageUseCase
import browserpicker.domain.usecases.uri.RecordUriInteractionUseCase
import browserpicker.domain.usecases.system.RestoreDataUseCase
import browserpicker.domain.usecases.uri.SaveHostRuleUseCase
import browserpicker.domain.usecases.analytics.SearchFoldersUseCase
import browserpicker.domain.usecases.analytics.SearchHostRulesUseCase
import browserpicker.domain.usecases.uri.SearchUrisUseCase
import browserpicker.domain.usecases.system.SetAsDefaultBrowserUseCase
import browserpicker.domain.usecases.browser.SetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.system.ShareUriUseCase
import browserpicker.domain.usecases.folder.UpdateFolderUseCase
import browserpicker.domain.usecases.uri.ValidateUriUseCase
import javax.inject.*

@Singleton
class UriHandlingUseCasesImpl @Inject constructor(
    override val handleUriUseCase: HandleUriUseCase,
    override val validateUriUseCase: ValidateUriUseCase,
    override val recordUriInteractionUseCase: RecordUriInteractionUseCase,
    override val getRecentUrisUseCase: GetRecentUrisUseCase,
    override val searchUrisUseCase: SearchUrisUseCase,
    override val cleanupUriHistoryUseCase: CleanupUriHistoryUseCase,
): UriHandlingUseCases

@Singleton
class BrowserUseCasesImpl @Inject constructor(
    override val getAvailableBrowsersUseCase: GetAvailableBrowsersUseCase,
    override val getPreferredBrowserForHostUseCase: GetPreferredBrowserForHostUseCase,
    override val setPreferredBrowserForHostUseCase: SetPreferredBrowserForHostUseCase,
    override val clearPreferredBrowserForHostUseCase: ClearPreferredBrowserForHostUseCase,
    override val recordBrowserUsageUseCase: RecordBrowserUsageUseCase,
    override val getBrowserUsageStatsUseCase: GetBrowserUsageStatsUseCase,
    override val getBrowserUsageStatUseCase: GetBrowserUsageStatUseCase,
    override val getMostFrequentlyUsedBrowserUseCase: GetMostFrequentlyUsedBrowserUseCase,
    override val getMostRecentlyUsedBrowserUseCase: GetMostRecentlyUsedBrowserUseCase,
//    override val deleteBrowserStatUseCase: DeleteBrowserStatUseCase,
//    override val deleteAllStatsUseCase: DeleteAllStatsUseCase,
): BrowserUseCases

@Singleton
class HostRuleUseCasesImpl @Inject constructor(
    override val getHostRuleUseCase: GetHostRuleUseCase,
    override val getHostRuleByIdUseCase: GetHostRuleByIdUseCase,
    override val saveHostRuleUseCase: SaveHostRuleUseCase,
    override val deleteHostRuleUseCase: DeleteHostRuleUseCase,
    override val getAllHostRulesUseCase: GetAllHostRulesUseCase,
    override val getHostRulesByStatusUseCase: GetHostRulesByStatusUseCase,
    override val getHostRulesByFolderUseCase: GetHostRulesByFolderUseCase,
    override val getRootHostRulesByStatusUseCase: GetRootHostRulesByStatusUseCase,
    override val bookmarkHostUseCase: BookmarkHostUseCase,
    override val blockHostUseCase: BlockHostUseCase,
    override val clearHostStatusUseCase: ClearHostStatusUseCase,
): HostRuleUseCases

@Singleton
class UriHistoryUseCasesImpl @Inject constructor(
    override val getPagedUriHistoryUseCase: GetPagedUriHistoryUseCase,
    override val getUriHistoryCountUseCase: GetUriHistoryCountUseCase,
    override val getUriHistoryGroupCountsUseCase: GetUriHistoryGroupCountsUseCase,
    override val getUriHistoryDateCountsUseCase: GetUriHistoryDateCountsUseCase,
    override val getUriRecordByIdUseCase: GetUriRecordByIdUseCase,
    override val deleteUriRecordUseCase: DeleteUriRecordUseCase,
    override val deleteAllUriHistoryUseCase: DeleteAllUriHistoryUseCase,
    override val getUriFilterOptionsUseCase: GetUriFilterOptionsUseCase,
    override val exportUriHistoryUseCase: ExportUriHistoryUseCase,
    override val importUriHistoryUseCase: ImportUriHistoryUseCase,
//    override val getDistinctHistoryHostsUseCase: GetDistinctHistoryHostsUseCase,
//    override val getDistinctChosenBrowsersUseCase: GetDistinctChosenBrowsersUseCase,
): UriHistoryUseCases

@Singleton
class FolderUseCasesImpl @Inject constructor(
    override val getFolderUseCase: GetFolderUseCase,
    override val getChildFoldersUseCase: GetChildFoldersUseCase,
    override val getRootFoldersUseCase: GetRootFoldersUseCase,
    override val getAllFoldersByTypeUseCase: GetAllFoldersByTypeUseCase,
    override val findFolderByNameAndParentUseCase: FindFolderByNameAndParentUseCase,
    override val createFolderUseCase: CreateFolderUseCase,
    override val updateFolderUseCase: UpdateFolderUseCase,
    override val deleteFolderUseCase: DeleteFolderUseCase,
    override val moveFolderUseCase: MoveFolderUseCase,
    override val moveHostRuleToFolderUseCase: MoveHostRuleToFolderUseCase,
    override val getFolderHierarchyUseCase: GetFolderHierarchyUseCase,
    override val ensureDefaultFoldersExistUseCase: EnsureDefaultFoldersExistUseCase,
): FolderUseCases

@Singleton
class SearchAndAnalyticsUseCasesImpl @Inject constructor(
    override val analyzeUriTrendsUseCase: AnalyzeUriTrendsUseCase,
    override val analyzeBrowserUsageTrendsUseCase: AnalyzeBrowserUsageTrendsUseCase,
    override val getMostVisitedHostsUseCase: GetMostVisitedHostsUseCase,
    override val getTopActionsByHostUseCase: GetTopActionsByHostUseCase,
    override val searchHostRulesUseCase: SearchHostRulesUseCase,
    override val searchFoldersUseCase: SearchFoldersUseCase,
    override val generateHistoryReportUseCase: GenerateHistoryReportUseCase,
    override val generateBrowserUsageReportUseCase: GenerateBrowserUsageReportUseCase,
): SearchAndAnalyticsUseCases

@Singleton
class SystemIntegrationUseCasesImpl @Inject constructor(
    override val checkDefaultBrowserStatusUseCase: CheckDefaultBrowserStatusUseCase,
    override val openBrowserPreferencesUseCase: OpenBrowserPreferencesUseCase,
    override val monitorUriClipboardUseCase: MonitorUriClipboardUseCase,
    override val shareUriUseCase: ShareUriUseCase,
    override val openUriInBrowserUseCase: OpenUriInBrowserUseCase,
    override val setAsDefaultBrowserUseCase: SetAsDefaultBrowserUseCase,
    override val backupDataUseCase: BackupDataUseCase,
    override val restoreDataUseCase: RestoreDataUseCase,
    override val monitorSystemBrowserChangesUseCase: MonitorSystemBrowserChangesUseCase,
    override val handleUncaughtUriUseCase: HandleUncaughtUriUseCase,
): SystemIntegrationUseCases