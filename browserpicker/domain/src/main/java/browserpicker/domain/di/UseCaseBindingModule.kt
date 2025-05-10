package browserpicker.domain.di

import browserpicker.domain.usecases.analytics.AnalyzeBrowserUsageTrendsUseCase
import browserpicker.domain.usecases.analytics.AnalyzeUriTrendsUseCase
import browserpicker.domain.usecases.system.BackupDataUseCase
import browserpicker.domain.usecases.uri.host.BlockHostUseCase
import browserpicker.domain.usecases.uri.host.BookmarkHostUseCase
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
import browserpicker.domain.usecases.uri.host.GetRootHostRulesByStatusUseCase
import browserpicker.domain.usecases.uri.history.GetUriFilterOptionsUseCase
import browserpicker.domain.usecases.uri.history.GetUriHistoryCountUseCase
import browserpicker.domain.usecases.uri.history.GetUriHistoryDateCountsUseCase
import browserpicker.domain.usecases.uri.history.GetUriHistoryGroupCountsUseCase
import browserpicker.domain.usecases.uri.history.GetUriRecordByIdUseCase
import browserpicker.domain.usecases.system.HandleUncaughtUriUseCase
import browserpicker.domain.usecases.uri.history.ImportUriHistoryUseCase
import browserpicker.domain.usecases.system.MonitorSystemBrowserChangesUseCase
import browserpicker.domain.usecases.system.MonitorUriClipboardUseCase
import browserpicker.domain.usecases.system.OpenBrowserPreferencesUseCase
import browserpicker.domain.usecases.system.OpenUriInBrowserUseCase
import browserpicker.domain.usecases.system.RestoreDataUseCase
import browserpicker.domain.usecases.system.SetAsDefaultBrowserUseCase
import browserpicker.domain.usecases.system.ShareUriUseCase
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
import browserpicker.domain.usecases.uri.shared.HandleUriUseCase
import browserpicker.domain.usecases.folder.MoveFolderUseCase
import browserpicker.domain.usecases.folder.MoveHostRuleToFolderUseCase
import browserpicker.domain.usecases.uri.shared.RecordUriInteractionUseCase
import browserpicker.domain.usecases.uri.host.SaveHostRuleUseCase
import browserpicker.domain.usecases.uri.shared.SearchUrisUseCase
import browserpicker.domain.usecases.folder.UpdateFolderUseCase
import browserpicker.domain.usecases.uri.shared.ValidateUriUseCase
import browserpicker.domain.usecases.browser.RecordBrowserUsageUseCase
import browserpicker.domain.usecases.analytics.SearchFoldersUseCase
import browserpicker.domain.usecases.analytics.SearchHostRulesUseCase
import browserpicker.domain.usecases.browser.SetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.analytics.AnalyzeBrowserUsageTrendsUseCaseImpl
import browserpicker.domain.usecases.analytics.AnalyzeUriTrendsUseCaseImpl
import browserpicker.domain.usecases.system.BackupDataUseCaseImpl
import browserpicker.domain.usecases.uri.host.BlockHostUseCaseImpl
import browserpicker.domain.usecases.uri.host.BookmarkHostUseCaseImpl
import browserpicker.domain.usecases.system.CheckDefaultBrowserStatusUseCaseImpl
import browserpicker.domain.usecases.uri.shared.CleanupUriHistoryUseCaseImpl
import browserpicker.domain.usecases.uri.host.ClearHostStatusUseCaseImpl
import browserpicker.domain.usecases.browser.ClearPreferredBrowserForHostUseCaseImpl
import browserpicker.domain.usecases.folder.CreateFolderUseCaseImpl
import browserpicker.domain.usecases.uri.history.DeleteAllUriHistoryUseCaseImpl
import browserpicker.domain.usecases.folder.DeleteFolderUseCaseImpl
import browserpicker.domain.usecases.uri.host.DeleteHostRuleUseCaseImpl
import browserpicker.domain.usecases.uri.history.DeleteUriRecordUseCaseImpl
import browserpicker.domain.usecases.folder.EnsureDefaultFoldersExistUseCaseImpl
import browserpicker.domain.usecases.uri.history.ExportUriHistoryUseCaseImpl
import browserpicker.domain.usecases.folder.FindFolderByNameAndParentUseCaseImpl
import browserpicker.domain.usecases.analytics.GenerateBrowserUsageReportUseCaseImpl
import browserpicker.domain.usecases.analytics.GenerateHistoryReportUseCaseImpl
import browserpicker.domain.usecases.folder.GetAllFoldersByTypeUseCaseImpl
import browserpicker.domain.usecases.uri.host.GetAllHostRulesUseCaseImpl
import browserpicker.domain.usecases.browser.GetAvailableBrowsersUseCaseImpl
import browserpicker.domain.usecases.browser.GetBrowserUsageStatUseCaseImpl
import browserpicker.domain.usecases.browser.GetBrowserUsageStatsUseCaseImpl
import browserpicker.domain.usecases.folder.GetChildFoldersUseCaseImpl
import browserpicker.domain.usecases.folder.GetFolderHierarchyUseCaseImpl
import browserpicker.domain.usecases.folder.GetFolderUseCaseImpl
import browserpicker.domain.usecases.uri.host.GetHostRuleByIdUseCaseImpl
import browserpicker.domain.usecases.uri.host.GetHostRuleUseCaseImpl
import browserpicker.domain.usecases.uri.host.GetHostRulesByFolderUseCaseImpl
import browserpicker.domain.usecases.uri.host.GetHostRulesByStatusUseCaseImpl
import browserpicker.domain.usecases.browser.GetMostFrequentlyUsedBrowserUseCaseImpl
import browserpicker.domain.usecases.browser.GetMostRecentlyUsedBrowserUseCaseImpl
import browserpicker.domain.usecases.analytics.GetMostVisitedHostsUseCaseImpl
import browserpicker.domain.usecases.uri.history.GetPagedUriHistoryUseCaseImpl
import browserpicker.domain.usecases.browser.GetPreferredBrowserForHostUseCaseImpl
import browserpicker.domain.usecases.uri.shared.GetRecentUrisUseCaseImpl
import browserpicker.domain.usecases.folder.GetRootFoldersUseCaseImpl
import browserpicker.domain.usecases.uri.host.GetRootHostRulesByStatusUseCaseImpl
import browserpicker.domain.usecases.analytics.GetTopActionsByHostUseCaseImpl
import browserpicker.domain.usecases.uri.history.GetUriFilterOptionsUseCaseImpl
import browserpicker.domain.usecases.uri.history.GetUriHistoryCountUseCaseImpl
import browserpicker.domain.usecases.uri.history.GetUriHistoryDateCountsUseCaseImpl
import browserpicker.domain.usecases.uri.history.GetUriHistoryGroupCountsUseCaseImpl
import browserpicker.domain.usecases.uri.history.GetUriRecordByIdUseCaseImpl
import browserpicker.domain.usecases.system.HandleUncaughtUriUseCaseImpl
import browserpicker.domain.usecases.uri.shared.HandleUriUseCaseImpl
import browserpicker.domain.usecases.uri.history.ImportUriHistoryUseCaseImpl
import browserpicker.domain.usecases.system.MonitorSystemBrowserChangesUseCaseImpl
import browserpicker.domain.usecases.system.MonitorUriClipboardUseCaseImpl
import browserpicker.domain.usecases.folder.MoveFolderUseCaseImpl
import browserpicker.domain.usecases.folder.MoveHostRuleToFolderUseCaseImpl
import browserpicker.domain.usecases.system.OpenBrowserPreferencesUseCaseImpl
import browserpicker.domain.usecases.system.OpenUriInBrowserUseCaseImpl
import browserpicker.domain.usecases.browser.RecordBrowserUsageUseCaseImpl
import browserpicker.domain.usecases.uri.shared.RecordUriInteractionUseCaseImpl
import browserpicker.domain.usecases.system.RestoreDataUseCaseImpl
import browserpicker.domain.usecases.uri.host.SaveHostRuleUseCaseImpl
import browserpicker.domain.usecases.analytics.SearchFoldersUseCaseImpl
import browserpicker.domain.usecases.analytics.SearchHostRulesUseCaseImpl
import browserpicker.domain.usecases.uri.shared.SearchUrisUseCaseImpl
import browserpicker.domain.usecases.system.SetAsDefaultBrowserUseCaseImpl
import browserpicker.domain.usecases.browser.SetPreferredBrowserForHostUseCaseImpl
import browserpicker.domain.usecases.system.ShareUriUseCaseImpl
import browserpicker.domain.usecases.folder.UpdateFolderUseCaseImpl
import browserpicker.domain.usecases.uri.shared.ValidateUriUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseBindingModule {

    @Binds
    @Singleton
    abstract fun bindHandleUriUseCase(impl: HandleUriUseCaseImpl): HandleUriUseCase

    @Binds
    @Singleton
    abstract fun bindValidateUriUseCase(impl: ValidateUriUseCaseImpl): ValidateUriUseCase

    @Binds
    @Singleton
    abstract fun bindRecordUriInteractionUseCase(impl: RecordUriInteractionUseCaseImpl): RecordUriInteractionUseCase

    @Binds
    @Singleton
    abstract fun bindGetRecentUrisUseCase(impl: GetRecentUrisUseCaseImpl): GetRecentUrisUseCase

    @Binds
    @Singleton
    abstract fun bindSearchUrisUseCase(impl: SearchUrisUseCaseImpl): SearchUrisUseCase

    @Binds
    @Singleton
    abstract fun bindCleanupUriHistoryUseCase(impl: CleanupUriHistoryUseCaseImpl): CleanupUriHistoryUseCase

    @Binds
    @Singleton
    abstract fun bindGetAvailableBrowsersUseCase(impl: GetAvailableBrowsersUseCaseImpl): GetAvailableBrowsersUseCase

    @Binds
    @Singleton
    abstract fun bindGetPreferredBrowserForHostUseCase(impl: GetPreferredBrowserForHostUseCaseImpl): GetPreferredBrowserForHostUseCase

    @Binds
    @Singleton
    abstract fun bindSetPreferredBrowserForHostUseCase(impl: SetPreferredBrowserForHostUseCaseImpl): SetPreferredBrowserForHostUseCase

    @Binds
    @Singleton
    abstract fun bindClearPreferredBrowserForHostUseCase(impl: ClearPreferredBrowserForHostUseCaseImpl): ClearPreferredBrowserForHostUseCase

    @Binds
    @Singleton
    abstract fun bindRecordBrowserUsageUseCase(impl: RecordBrowserUsageUseCaseImpl): RecordBrowserUsageUseCase

    @Binds
    @Singleton
    abstract fun bindGetBrowserUsageStatsUseCase(impl: GetBrowserUsageStatsUseCaseImpl): GetBrowserUsageStatsUseCase

    @Binds
    @Singleton
    abstract fun bindGetBrowserUsageStatUseCase(impl: GetBrowserUsageStatUseCaseImpl): GetBrowserUsageStatUseCase

    @Binds
    @Singleton
    abstract fun bindGetMostFrequentlyUsedBrowserUseCase(impl: GetMostFrequentlyUsedBrowserUseCaseImpl): GetMostFrequentlyUsedBrowserUseCase

    @Binds
    @Singleton
    abstract fun bindGetMostRecentlyUsedBrowserUseCase(impl: GetMostRecentlyUsedBrowserUseCaseImpl): GetMostRecentlyUsedBrowserUseCase

    @Binds
    @Singleton
    abstract fun bindGetHostRuleUseCase(impl: GetHostRuleUseCaseImpl): GetHostRuleUseCase

    @Binds
    @Singleton
    abstract fun bindGetHostRuleByIdUseCase(impl: GetHostRuleByIdUseCaseImpl): GetHostRuleByIdUseCase

    @Binds
    @Singleton
    abstract fun bindSaveHostRuleUseCase(impl: SaveHostRuleUseCaseImpl): SaveHostRuleUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteHostRuleUseCase(impl: DeleteHostRuleUseCaseImpl): DeleteHostRuleUseCase

    @Binds
    @Singleton
    abstract fun bindGetAllHostRulesUseCase(impl: GetAllHostRulesUseCaseImpl): GetAllHostRulesUseCase

    @Binds
    @Singleton
    abstract fun bindGetHostRulesByStatusUseCase(impl: GetHostRulesByStatusUseCaseImpl): GetHostRulesByStatusUseCase

    @Binds
    @Singleton
    abstract fun bindGetHostRulesByFolderUseCase(impl: GetHostRulesByFolderUseCaseImpl): GetHostRulesByFolderUseCase

    @Binds
    @Singleton
    abstract fun bindGetRootHostRulesByStatusUseCase(impl: GetRootHostRulesByStatusUseCaseImpl): GetRootHostRulesByStatusUseCase

    @Binds
    @Singleton
    abstract fun bindBookmarkHostUseCase(impl: BookmarkHostUseCaseImpl): BookmarkHostUseCase

    @Binds
    @Singleton
    abstract fun bindBlockHostUseCase(impl: BlockHostUseCaseImpl): BlockHostUseCase

    @Binds
    @Singleton
    abstract fun bindClearHostStatusUseCase(impl: ClearHostStatusUseCaseImpl): ClearHostStatusUseCase

    @Binds
    @Singleton
    abstract fun bindGetPagedUriHistoryUseCase(impl: GetPagedUriHistoryUseCaseImpl): GetPagedUriHistoryUseCase

    @Binds
    @Singleton
    abstract fun bindGetUriHistoryCountUseCase(impl: GetUriHistoryCountUseCaseImpl): GetUriHistoryCountUseCase

    @Binds
    @Singleton
    abstract fun bindGetUriHistoryGroupCountsUseCase(impl: GetUriHistoryGroupCountsUseCaseImpl): GetUriHistoryGroupCountsUseCase

    @Binds
    @Singleton
    abstract fun bindGetUriHistoryDateCountsUseCase(impl: GetUriHistoryDateCountsUseCaseImpl): GetUriHistoryDateCountsUseCase

    @Binds
    @Singleton
    abstract fun bindGetUriRecordByIdUseCase(impl: GetUriRecordByIdUseCaseImpl): GetUriRecordByIdUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteUriRecordUseCase(impl: DeleteUriRecordUseCaseImpl): DeleteUriRecordUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteAllUriHistoryUseCase(impl: DeleteAllUriHistoryUseCaseImpl): DeleteAllUriHistoryUseCase

    @Binds
    @Singleton
    abstract fun bindGetUriFilterOptionsUseCase(impl: GetUriFilterOptionsUseCaseImpl): GetUriFilterOptionsUseCase

    @Binds
    @Singleton
    abstract fun bindExportUriHistoryUseCase(impl: ExportUriHistoryUseCaseImpl): ExportUriHistoryUseCase

    @Binds
    @Singleton
    abstract fun bindImportUriHistoryUseCase(impl: ImportUriHistoryUseCaseImpl): ImportUriHistoryUseCase

//    @Binds
//    @Singleton
//    abstract fun bindGetDistinctHistoryHostsUseCase(impl: GetDistinctHistoryHostsUseCaseImpl): GetDistinctHistoryHostsUseCase
//
//    @Binds
//    @Singleton
//    abstract fun bindGetDistinctChosenBrowsersUseCase(impl: GetDistinctChosenBrowsersUseCaseImpl): GetDistinctChosenBrowsersUseCase

    @Binds
    @Singleton
    abstract fun bindGetFolderUseCase(impl: GetFolderUseCaseImpl): GetFolderUseCase

    @Binds
    @Singleton
    abstract fun bindGetChildFoldersUseCase(impl: GetChildFoldersUseCaseImpl): GetChildFoldersUseCase

    @Binds
    @Singleton
    abstract fun bindGetRootFoldersUseCase(impl: GetRootFoldersUseCaseImpl): GetRootFoldersUseCase

    @Binds
    @Singleton
    abstract fun bindGetAllFoldersByTypeUseCase(impl: GetAllFoldersByTypeUseCaseImpl): GetAllFoldersByTypeUseCase

    @Binds
    @Singleton
    abstract fun bindFindFolderByNameAndParentUseCase(impl: FindFolderByNameAndParentUseCaseImpl): FindFolderByNameAndParentUseCase

    @Binds
    @Singleton
    abstract fun bindCreateFolderUseCase(impl: CreateFolderUseCaseImpl): CreateFolderUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateFolderUseCase(impl: UpdateFolderUseCaseImpl): UpdateFolderUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteFolderUseCase(impl: DeleteFolderUseCaseImpl): DeleteFolderUseCase

    @Binds
    @Singleton
    abstract fun bindMoveFolderUseCase(impl: MoveFolderUseCaseImpl): MoveFolderUseCase

    @Binds
    @Singleton
    abstract fun bindMoveHostRuleToFolderUseCase(impl: MoveHostRuleToFolderUseCaseImpl): MoveHostRuleToFolderUseCase

    @Binds
    @Singleton
    abstract fun bindGetFolderHierarchyUseCase(impl: GetFolderHierarchyUseCaseImpl): GetFolderHierarchyUseCase

    @Binds
    @Singleton
    abstract fun bindEnsureDefaultFoldersExistUseCase(impl: EnsureDefaultFoldersExistUseCaseImpl): EnsureDefaultFoldersExistUseCase

    @Binds
    @Singleton
    abstract fun bindAnalyzeUriTrendsUseCase(impl: AnalyzeUriTrendsUseCaseImpl): AnalyzeUriTrendsUseCase

    @Binds
    @Singleton
    abstract fun bindAnalyzeBrowserUsageTrendsUseCase(impl: AnalyzeBrowserUsageTrendsUseCaseImpl): AnalyzeBrowserUsageTrendsUseCase

    @Binds
    @Singleton
    abstract fun bindGetMostVisitedHostsUseCase(impl: GetMostVisitedHostsUseCaseImpl): GetMostVisitedHostsUseCase

    @Binds
    @Singleton
    abstract fun bindGetTopActionsByHostUseCase(impl: GetTopActionsByHostUseCaseImpl): GetTopActionsByHostUseCase

    @Binds
    @Singleton
    abstract fun bindSearchHostRulesUseCase(impl: SearchHostRulesUseCaseImpl): SearchHostRulesUseCase

    @Binds
    @Singleton
    abstract fun bindSearchFoldersUseCase(impl: SearchFoldersUseCaseImpl): SearchFoldersUseCase

    @Binds
    @Singleton
    abstract fun bindGenerateHistoryReportUseCase(impl: GenerateHistoryReportUseCaseImpl): GenerateHistoryReportUseCase

    @Binds
    @Singleton
    abstract fun bindGenerateBrowserUsageReportUseCase(impl: GenerateBrowserUsageReportUseCaseImpl): GenerateBrowserUsageReportUseCase

    @Binds
    @Singleton
    abstract fun bindCheckDefaultBrowserStatusUseCase(impl: CheckDefaultBrowserStatusUseCaseImpl): CheckDefaultBrowserStatusUseCase

    @Binds
    @Singleton
    abstract fun bindOpenBrowserPreferencesUseCase(impl: OpenBrowserPreferencesUseCaseImpl): OpenBrowserPreferencesUseCase

    @Binds
    @Singleton
    abstract fun bindMonitorUriClipboardUseCase(impl: MonitorUriClipboardUseCaseImpl): MonitorUriClipboardUseCase

    @Binds
    @Singleton
    abstract fun bindShareUriUseCase(impl: ShareUriUseCaseImpl): ShareUriUseCase

    @Binds
    @Singleton
    abstract fun bindOpenUriInBrowserUseCase(impl: OpenUriInBrowserUseCaseImpl): OpenUriInBrowserUseCase

    @Binds
    @Singleton
    abstract fun bindSetAsDefaultBrowserUseCase(impl: SetAsDefaultBrowserUseCaseImpl): SetAsDefaultBrowserUseCase

    @Binds
    @Singleton
    abstract fun bindBackupDataUseCase(impl: BackupDataUseCaseImpl): BackupDataUseCase

    @Binds
    @Singleton
    abstract fun bindRestoreDataUseCase(impl: RestoreDataUseCaseImpl): RestoreDataUseCase

    @Binds
    @Singleton
    abstract fun bindMonitorSystemBrowserChangesUseCase(impl: MonitorSystemBrowserChangesUseCaseImpl): MonitorSystemBrowserChangesUseCase

    @Binds
    @Singleton
    abstract fun bindHandleUncaughtUriUseCase(impl: HandleUncaughtUriUseCaseImpl): HandleUncaughtUriUseCase

//    @Binds
//    @Singleton
//    abstract fun bindDeleteBrowserStatUseCase(impl: DeleteBrowserStatUseCaseImpl): DeleteBrowserStatUseCase
//
//    @Binds
//    @Singleton
//    abstract fun bindDeleteAllStatsUseCase(impl: DeleteAllStatsUseCaseImpl): DeleteAllStatsUseCase
} 