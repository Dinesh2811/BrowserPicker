package com.dinesh.browserpicker.v1.domain.di

import com.dinesh.browserpicker.v1.domain.usecases.ClearPreferredBrowserForHostUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetAvailableBrowsersUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetBrowserUsageStatUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetBrowserUsageStatsUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetMostFrequentlyUsedBrowserUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetMostRecentlyUsedBrowserUseCase
import com.dinesh.browserpicker.v1.domain.usecases.GetPreferredBrowserForHostUseCase
import com.dinesh.browserpicker.v1.domain.usecases.HandleUriUseCase
import com.dinesh.browserpicker.v1.domain.usecases.RecordBrowserUsageUseCase
import com.dinesh.browserpicker.v1.domain.usecases.*
import com.dinesh.browserpicker.v1.domain.usecases.impl.*
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