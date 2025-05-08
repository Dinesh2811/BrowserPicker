package com.dinesh.browserpicker.v1.domain.di

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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UriHandlingUseCasesImpl @Inject constructor(
    override val handleUriUseCase: HandleUriUseCase,
    override val validateUriUseCase: ValidateUriUseCase,
    override val recordUriInteractionUseCase: RecordUriInteractionUseCase,
    override val getRecentUrisUseCase: GetRecentUrisUseCase,
    override val searchUrisUseCase: SearchUrisUseCase,
    override val cleanupUriHistoryUseCase: CleanupUriHistoryUseCase
) : UriHandlingUseCases

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
    override val getMostRecentlyUsedBrowserUseCase: GetMostRecentlyUsedBrowserUseCase
) : BrowserUseCases

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
    override val clearHostStatusUseCase: ClearHostStatusUseCase
) : HostRuleUseCases

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
    override val importUriHistoryUseCase: ImportUriHistoryUseCase
) : UriHistoryUseCases

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
    override val ensureDefaultFoldersExistUseCase: EnsureDefaultFoldersExistUseCase
) : FolderUseCases

@Singleton
class SearchAndAnalyticsUseCasesImpl @Inject constructor(
    override val analyzeUriTrendsUseCase: AnalyzeUriTrendsUseCase,
    override val analyzeBrowserUsageTrendsUseCase: AnalyzeBrowserUsageTrendsUseCase,
    override val getMostVisitedHostsUseCase: GetMostVisitedHostsUseCase,
    override val getTopActionsByHostUseCase: GetTopActionsByHostUseCase,
    override val searchHostRulesUseCase: SearchHostRulesUseCase,
    override val searchFoldersUseCase: SearchFoldersUseCase,
    override val generateHistoryReportUseCase: GenerateHistoryReportUseCase,
    override val generateBrowserUsageReportUseCase: GenerateBrowserUsageReportUseCase
) : SearchAndAnalyticsUseCases

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
    override val handleUncaughtUriUseCase: HandleUncaughtUriUseCase
) : SystemIntegrationUseCases 