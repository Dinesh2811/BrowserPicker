package browserpicker.domain.di

import browserpicker.domain.usecases.analytics.AnalyzeBrowserUsageTrendsUseCase
import browserpicker.domain.usecases.analytics.AnalyzeUriStatusChangesUseCase
import browserpicker.domain.usecases.analytics.AnalyzeUriTrendsUseCase
import browserpicker.domain.usecases.system.BackupDataUseCase
import browserpicker.domain.usecases.system.CheckDefaultBrowserStatusUseCase
import browserpicker.domain.usecases.uri.shared.CleanupUriHistoryUseCase
import browserpicker.domain.usecases.uri.host.ClearHostStatusUseCase
import browserpicker.domain.usecases.browser.ClearPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.folder.CreateFolderUseCase
import browserpicker.domain.usecases.uri.history.DeleteAllUriHistoryUseCase
import browserpicker.domain.usecases.folder.DeleteFolderUseCase
import browserpicker.domain.usecases.uri.host.DeleteHostRuleUseCase
import browserpicker.domain.usecases.uri.history.DeleteUriRecordUseCase
import browserpicker.domain.usecases.folder.EnsureDefaultFoldersExistUseCase
import browserpicker.domain.usecases.uri.history.ExportUriHistoryUseCase
import browserpicker.domain.usecases.folder.FindFolderByNameAndParentUseCase
import browserpicker.domain.usecases.analytics.GenerateBrowserUsageReportUseCase
import browserpicker.domain.usecases.analytics.GenerateHistoryReportUseCase
import browserpicker.domain.usecases.folder.GetAllFoldersByTypeUseCase
import browserpicker.domain.usecases.uri.host.GetAllHostRulesUseCase
import browserpicker.domain.usecases.browser.GetAvailableBrowsersUseCase
import browserpicker.domain.usecases.browser.GetBrowserUsageStatUseCase
import browserpicker.domain.usecases.browser.GetBrowserUsageStatsUseCase
import browserpicker.domain.usecases.folder.GetChildFoldersUseCase
import browserpicker.domain.usecases.folder.GetFolderHierarchyUseCase
import browserpicker.domain.usecases.folder.GetFolderUseCase
import browserpicker.domain.usecases.uri.host.GetHostRuleByIdUseCase
import browserpicker.domain.usecases.uri.host.GetHostRuleUseCase
import browserpicker.domain.usecases.uri.host.GetHostRulesByFolderUseCase
import browserpicker.domain.usecases.uri.host.GetHostRulesByStatusUseCase
import browserpicker.domain.usecases.browser.GetMostFrequentlyUsedBrowserUseCase
import browserpicker.domain.usecases.browser.GetMostRecentlyUsedBrowserUseCase
import browserpicker.domain.usecases.analytics.GetMostVisitedHostsUseCase
import browserpicker.domain.usecases.uri.history.GetPagedUriHistoryUseCase
import browserpicker.domain.usecases.browser.GetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.uri.shared.GetRecentUrisUseCase
import browserpicker.domain.usecases.folder.GetRootFoldersUseCase
import browserpicker.domain.usecases.analytics.GetTopActionsByHostUseCase
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
import browserpicker.domain.usecases.browser.RecordBrowserUsageUseCase
import browserpicker.domain.usecases.uri.shared.RecordUriInteractionUseCase
import browserpicker.domain.usecases.system.RestoreDataUseCase
import browserpicker.domain.usecases.uri.host.SaveHostRuleUseCase
import browserpicker.domain.usecases.analytics.SearchFoldersUseCase
import browserpicker.domain.usecases.analytics.SearchHostRulesUseCase
import browserpicker.domain.usecases.analytics.TrackUriActionUseCase
import browserpicker.domain.usecases.system.SetAsDefaultBrowserUseCase
import browserpicker.domain.usecases.browser.SetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.system.ShareUriUseCase
import browserpicker.domain.usecases.folder.UpdateFolderUseCase
import browserpicker.domain.usecases.uri.host.CheckUriStatusUseCase
import browserpicker.domain.usecases.uri.shared.ValidateUriUseCase
import javax.inject.*

@Singleton
class UriHandlingUseCasesImpl @Inject constructor(
    override val handleUriUseCase: HandleUriUseCase,
    override val validateUriUseCase: ValidateUriUseCase,
    override val recordUriInteractionUseCase: RecordUriInteractionUseCase,
    override val getRecentUrisUseCase: GetRecentUrisUseCase,
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
): BrowserUseCases

@Singleton
class HostRuleUseCasesImpl @Inject constructor(
    override val getHostRuleUseCase: GetHostRuleUseCase,
    override val getHostRuleByIdUseCase: GetHostRuleByIdUseCase,
    override val saveHostRuleUseCase: SaveHostRuleUseCase,
    override val deleteHostRuleUseCase: DeleteHostRuleUseCase,
    override val getAllHostRulesUseCase: GetAllHostRulesUseCase,
    override val getHostRulesByStatusUseCase: GetHostRulesByStatusUseCase,
    override val checkUriStatusUseCase: CheckUriStatusUseCase,
    override val getHostRulesByFolderUseCase: GetHostRulesByFolderUseCase,
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
//    override val moveFolderUseCase: MoveFolderUseCase,
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
    override val trackUriActionUseCase: TrackUriActionUseCase,
    override val analyzeUriStatusChangesUseCase: AnalyzeUriStatusChangesUseCase,
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