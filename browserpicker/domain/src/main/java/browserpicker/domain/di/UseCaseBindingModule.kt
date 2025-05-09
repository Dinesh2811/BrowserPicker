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
import browserpicker.domain.usecases.ImportUriHistoryUseCase
import browserpicker.domain.usecases.MonitorSystemBrowserChangesUseCase
import browserpicker.domain.usecases.MonitorUriClipboardUseCase
import browserpicker.domain.usecases.OpenBrowserPreferencesUseCase
import browserpicker.domain.usecases.OpenUriInBrowserUseCase
import browserpicker.domain.usecases.RestoreDataUseCase
import browserpicker.domain.usecases.SetAsDefaultBrowserUseCase
import browserpicker.domain.usecases.ShareUriUseCase
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
import browserpicker.domain.usecases.HandleUriUseCase
import browserpicker.domain.usecases.MoveFolderUseCase
import browserpicker.domain.usecases.MoveHostRuleToFolderUseCase
import browserpicker.domain.usecases.RecordUriInteractionUseCase
import browserpicker.domain.usecases.SaveHostRuleUseCase
import browserpicker.domain.usecases.SearchUrisUseCase
import browserpicker.domain.usecases.UpdateFolderUseCase
import browserpicker.domain.usecases.ValidateUriUseCase
import browserpicker.domain.usecases.RecordBrowserUsageUseCase
import browserpicker.domain.usecases.SearchFoldersUseCase
import browserpicker.domain.usecases.SearchHostRulesUseCase
import browserpicker.domain.usecases.SetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.impl.AnalyzeBrowserUsageTrendsUseCaseImpl
import browserpicker.domain.usecases.impl.AnalyzeUriTrendsUseCaseImpl
import browserpicker.domain.usecases.impl.BackupDataUseCaseImpl
import browserpicker.domain.usecases.impl.BlockHostUseCaseImpl
import browserpicker.domain.usecases.impl.BookmarkHostUseCaseImpl
import browserpicker.domain.usecases.impl.CheckDefaultBrowserStatusUseCaseImpl
import browserpicker.domain.usecases.impl.CleanupUriHistoryUseCaseImpl
import browserpicker.domain.usecases.impl.ClearHostStatusUseCaseImpl
import browserpicker.domain.usecases.impl.ClearPreferredBrowserForHostUseCaseImpl
import browserpicker.domain.usecases.impl.CreateFolderUseCaseImpl
import browserpicker.domain.usecases.impl.DeleteAllUriHistoryUseCaseImpl
import browserpicker.domain.usecases.impl.DeleteFolderUseCaseImpl
import browserpicker.domain.usecases.impl.DeleteHostRuleUseCaseImpl
import browserpicker.domain.usecases.impl.DeleteUriRecordUseCaseImpl
import browserpicker.domain.usecases.impl.EnsureDefaultFoldersExistUseCaseImpl
import browserpicker.domain.usecases.impl.ExportUriHistoryUseCaseImpl
import browserpicker.domain.usecases.impl.FindFolderByNameAndParentUseCaseImpl
import browserpicker.domain.usecases.impl.GenerateBrowserUsageReportUseCaseImpl
import browserpicker.domain.usecases.impl.GenerateHistoryReportUseCaseImpl
import browserpicker.domain.usecases.impl.GetAllFoldersByTypeUseCaseImpl
import browserpicker.domain.usecases.impl.GetAllHostRulesUseCaseImpl
import browserpicker.domain.usecases.impl.GetAvailableBrowsersUseCaseImpl
import browserpicker.domain.usecases.impl.GetBrowserUsageStatUseCaseImpl
import browserpicker.domain.usecases.impl.GetBrowserUsageStatsUseCaseImpl
import browserpicker.domain.usecases.impl.GetChildFoldersUseCaseImpl
import browserpicker.domain.usecases.impl.GetFolderHierarchyUseCaseImpl
import browserpicker.domain.usecases.impl.GetFolderUseCaseImpl
import browserpicker.domain.usecases.impl.GetHostRuleByIdUseCaseImpl
import browserpicker.domain.usecases.impl.GetHostRuleUseCaseImpl
import browserpicker.domain.usecases.impl.GetHostRulesByFolderUseCaseImpl
import browserpicker.domain.usecases.impl.GetHostRulesByStatusUseCaseImpl
import browserpicker.domain.usecases.impl.GetMostFrequentlyUsedBrowserUseCaseImpl
import browserpicker.domain.usecases.impl.GetMostRecentlyUsedBrowserUseCaseImpl
import browserpicker.domain.usecases.impl.GetMostVisitedHostsUseCaseImpl
import browserpicker.domain.usecases.impl.GetPagedUriHistoryUseCaseImpl
import browserpicker.domain.usecases.impl.GetPreferredBrowserForHostUseCaseImpl
import browserpicker.domain.usecases.impl.GetRecentUrisUseCaseImpl
import browserpicker.domain.usecases.impl.GetRootFoldersUseCaseImpl
import browserpicker.domain.usecases.impl.GetRootHostRulesByStatusUseCaseImpl
import browserpicker.domain.usecases.impl.GetTopActionsByHostUseCaseImpl
import browserpicker.domain.usecases.impl.GetUriFilterOptionsUseCaseImpl
import browserpicker.domain.usecases.impl.GetUriHistoryCountUseCaseImpl
import browserpicker.domain.usecases.impl.GetUriHistoryDateCountsUseCaseImpl
import browserpicker.domain.usecases.impl.GetUriHistoryGroupCountsUseCaseImpl
import browserpicker.domain.usecases.impl.GetUriRecordByIdUseCaseImpl
import browserpicker.domain.usecases.impl.HandleUncaughtUriUseCaseImpl
import browserpicker.domain.usecases.impl.HandleUriUseCaseImpl
import browserpicker.domain.usecases.impl.ImportUriHistoryUseCaseImpl
import browserpicker.domain.usecases.impl.MonitorSystemBrowserChangesUseCaseImpl
import browserpicker.domain.usecases.impl.MonitorUriClipboardUseCaseImpl
import browserpicker.domain.usecases.impl.MoveFolderUseCaseImpl
import browserpicker.domain.usecases.impl.MoveHostRuleToFolderUseCaseImpl
import browserpicker.domain.usecases.impl.OpenBrowserPreferencesUseCaseImpl
import browserpicker.domain.usecases.impl.OpenUriInBrowserUseCaseImpl
import browserpicker.domain.usecases.impl.RecordBrowserUsageUseCaseImpl
import browserpicker.domain.usecases.impl.RecordUriInteractionUseCaseImpl
import browserpicker.domain.usecases.impl.RestoreDataUseCaseImpl
import browserpicker.domain.usecases.impl.SaveHostRuleUseCaseImpl
import browserpicker.domain.usecases.impl.SearchFoldersUseCaseImpl
import browserpicker.domain.usecases.impl.SearchHostRulesUseCaseImpl
import browserpicker.domain.usecases.impl.SearchUrisUseCaseImpl
import browserpicker.domain.usecases.impl.SetAsDefaultBrowserUseCaseImpl
import browserpicker.domain.usecases.impl.SetPreferredBrowserForHostUseCaseImpl
import browserpicker.domain.usecases.impl.ShareUriUseCaseImpl
import browserpicker.domain.usecases.impl.UpdateFolderUseCaseImpl
import browserpicker.domain.usecases.impl.ValidateUriUseCaseImpl
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