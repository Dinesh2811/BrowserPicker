package browserpicker.domain.di

import browserpicker.domain.usecases.AnalyzeBrowserUsageTrendsUseCase
import browserpicker.domain.usecases.AnalyzeUriTrendsUseCase
import browserpicker.domain.usecases.BackupDataUseCase
import browserpicker.domain.usecases.BlockHostUseCase
import browserpicker.domain.usecases.BookmarkHostUseCase
import browserpicker.domain.usecases.CheckDefaultBrowserStatusUseCase
import browserpicker.domain.usecases.CleanupUriHistoryUseCase
import browserpicker.domain.usecases.ClearHostStatusUseCase
import browserpicker.domain.usecases.ClearPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.CreateFolderUseCase
import browserpicker.domain.usecases.DeleteAllUriHistoryUseCase
import browserpicker.domain.usecases.DeleteFolderUseCase
import browserpicker.domain.usecases.DeleteHostRuleUseCase
import browserpicker.domain.usecases.DeleteUriRecordUseCase
import browserpicker.domain.usecases.EnsureDefaultFoldersExistUseCase
import browserpicker.domain.usecases.ExportUriHistoryUseCase
import browserpicker.domain.usecases.FindFolderByNameAndParentUseCase
import browserpicker.domain.usecases.GenerateBrowserUsageReportUseCase
import browserpicker.domain.usecases.GenerateHistoryReportUseCase
import browserpicker.domain.usecases.GetAllFoldersByTypeUseCase
import browserpicker.domain.usecases.GetAllHostRulesUseCase
import browserpicker.domain.usecases.GetAvailableBrowsersUseCase
import browserpicker.domain.usecases.GetBrowserUsageStatUseCase
import browserpicker.domain.usecases.GetBrowserUsageStatsUseCase
import browserpicker.domain.usecases.GetChildFoldersUseCase
import browserpicker.domain.usecases.GetFolderHierarchyUseCase
import browserpicker.domain.usecases.GetFolderUseCase
import browserpicker.domain.usecases.GetHostRuleByIdUseCase
import browserpicker.domain.usecases.GetHostRuleUseCase
import browserpicker.domain.usecases.GetHostRulesByFolderUseCase
import browserpicker.domain.usecases.GetHostRulesByStatusUseCase
import browserpicker.domain.usecases.GetMostFrequentlyUsedBrowserUseCase
import browserpicker.domain.usecases.GetMostRecentlyUsedBrowserUseCase
import browserpicker.domain.usecases.GetMostVisitedHostsUseCase
import browserpicker.domain.usecases.GetPagedUriHistoryUseCase
import browserpicker.domain.usecases.GetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.GetRecentUrisUseCase
import browserpicker.domain.usecases.GetRootFoldersUseCase
import browserpicker.domain.usecases.GetRootHostRulesByStatusUseCase
import browserpicker.domain.usecases.GetTopActionsByHostUseCase
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
import browserpicker.domain.usecases.RecordBrowserUsageUseCase
import browserpicker.domain.usecases.RecordUriInteractionUseCase
import browserpicker.domain.usecases.RestoreDataUseCase
import browserpicker.domain.usecases.SaveHostRuleUseCase
import browserpicker.domain.usecases.SearchFoldersUseCase
import browserpicker.domain.usecases.SearchHostRulesUseCase
import browserpicker.domain.usecases.SearchUrisUseCase
import browserpicker.domain.usecases.SetAsDefaultBrowserUseCase
import browserpicker.domain.usecases.SetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.ShareUriUseCase
import browserpicker.domain.usecases.UpdateFolderUseCase
import browserpicker.domain.usecases.ValidateUriUseCase
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