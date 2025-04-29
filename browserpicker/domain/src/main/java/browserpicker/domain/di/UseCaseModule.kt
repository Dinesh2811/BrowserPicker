package browserpicker.domain.di

import browserpicker.domain.usecase.stats.ClearBrowserStatsUseCase
import browserpicker.domain.usecase.stats.ClearBrowserStatsUseCaseImpl
import browserpicker.domain.usecase.history.ClearUriHistoryUseCase
import browserpicker.domain.usecase.history.ClearUriHistoryUseCaseImpl
import browserpicker.domain.usecase.folders.CreateFolderUseCase
import browserpicker.domain.usecase.folders.CreateFolderUseCaseImpl
import browserpicker.domain.usecase.folders.DeleteFolderUseCase
import browserpicker.domain.usecase.folders.DeleteFolderUseCaseImpl
import browserpicker.domain.usecase.rules.DeleteHostRuleUseCase
import browserpicker.domain.usecase.rules.DeleteHostRuleUseCaseImpl
import browserpicker.domain.usecase.history.DeleteUriRecordUseCase
import browserpicker.domain.usecase.history.DeleteUriRecordUseCaseImpl
import browserpicker.domain.usecase.stats.GetBrowserStatsUseCase
import browserpicker.domain.usecase.stats.GetBrowserStatsUseCaseImpl
import browserpicker.domain.usecase.folders.GetFoldersUseCase
import browserpicker.domain.usecase.folders.GetFoldersUseCaseImpl
import browserpicker.domain.usecase.history.GetHistoryFilterOptionsUseCase
import browserpicker.domain.usecase.history.GetHistoryFilterOptionsUseCaseImpl
import browserpicker.domain.usecase.history.GetHistoryOverviewUseCase
import browserpicker.domain.usecase.history.GetHistoryOverviewUseCaseImpl
import browserpicker.domain.usecase.rules.GetHostRuleUseCase
import browserpicker.domain.usecase.rules.GetHostRuleUseCaseImpl
import browserpicker.domain.usecase.rules.GetHostRulesUseCase
import browserpicker.domain.usecase.rules.GetHostRulesUseCaseImpl
import browserpicker.domain.usecase.history.GetPagedUriHistoryUseCase
import browserpicker.domain.usecase.history.GetPagedUriHistoryUseCaseImpl
import browserpicker.domain.usecase.history.HandleInterceptedUriUseCase
import browserpicker.domain.usecase.history.HandleInterceptedUriUseCaseImpl
import browserpicker.domain.usecase.initialization.InitializeAppDefaultsUseCase
import browserpicker.domain.usecase.initialization.InitializeAppDefaultsUseCaseImpl
import browserpicker.domain.usecase.history.RecordUriInteractionUseCase
import browserpicker.domain.usecase.history.RecordUriInteractionUseCaseImpl
import browserpicker.domain.usecase.rules.SaveHostRuleUseCase
import browserpicker.domain.usecase.rules.SaveHostRuleUseCaseImpl
import browserpicker.domain.usecase.folders.UpdateFolderUseCase
import browserpicker.domain.usecase.folders.UpdateFolderUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InitializeAppDefaultsUseCaseModule {

    // InitializeAppDefaultsUseCase could arguably be Singleton if needed early,
    // but let's keep it ViewModelScoped for consistency with others here.
    // If it were Singleton, you'd use @InstallIn(SingletonComponent::class) and @Singleton
    @Binds @Singleton
    abstract fun bindInitializeAppDefaultsUseCase(impl: InitializeAppDefaultsUseCaseImpl): InitializeAppDefaultsUseCase
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class BrowserPickerUseCaseModule {
    @Binds @ViewModelScoped
    abstract fun bindHandleInterceptedUriUseCase(impl: HandleInterceptedUriUseCaseImpl): HandleInterceptedUriUseCase

    @Binds @ViewModelScoped
    abstract fun bindGetPagedUriHistoryUseCase(impl: GetPagedUriHistoryUseCaseImpl): GetPagedUriHistoryUseCase

    @Binds @ViewModelScoped
    abstract fun bindGetHistoryOverviewUseCase(impl: GetHistoryOverviewUseCaseImpl): GetHistoryOverviewUseCase

    @Binds @ViewModelScoped
    abstract fun bindRecordUriInteractionUseCase(impl: RecordUriInteractionUseCaseImpl): RecordUriInteractionUseCase

    @Binds @ViewModelScoped
    abstract fun bindGetHistoryFilterOptionsUseCase(impl: GetHistoryFilterOptionsUseCaseImpl): GetHistoryFilterOptionsUseCase

    @Binds @ViewModelScoped
    abstract fun bindDeleteUriRecordUseCase(impl: DeleteUriRecordUseCaseImpl): DeleteUriRecordUseCase

    @Binds @ViewModelScoped
    abstract fun bindClearUriHistoryUseCase(impl: ClearUriHistoryUseCaseImpl): ClearUriHistoryUseCase


    // Host Rules
    @Binds @ViewModelScoped
    abstract fun bindGetHostRuleUseCase(impl: GetHostRuleUseCaseImpl): GetHostRuleUseCase

    @Binds @ViewModelScoped
    abstract fun bindGetHostRulesUseCase(impl: GetHostRulesUseCaseImpl): GetHostRulesUseCase

    @Binds @ViewModelScoped
    abstract fun bindSaveHostRuleUseCase(impl: SaveHostRuleUseCaseImpl): SaveHostRuleUseCase

    @Binds @ViewModelScoped
    abstract fun bindDeleteHostRuleUseCase(impl: DeleteHostRuleUseCaseImpl): DeleteHostRuleUseCase


    // Folders
    @Binds @ViewModelScoped
    abstract fun bindGetFoldersUseCase(impl: GetFoldersUseCaseImpl): GetFoldersUseCase

    @Binds @ViewModelScoped
    abstract fun bindCreateFolderUseCase(impl: CreateFolderUseCaseImpl): CreateFolderUseCase

    @Binds @ViewModelScoped
    abstract fun bindUpdateFolderUseCase(impl: UpdateFolderUseCaseImpl): UpdateFolderUseCase

    @Binds @ViewModelScoped
    abstract fun bindDeleteFolderUseCase(impl: DeleteFolderUseCaseImpl): DeleteFolderUseCase


    // Browser Stats
    @Binds @ViewModelScoped
    abstract fun bindGetBrowserStatsUseCase(impl: GetBrowserStatsUseCaseImpl): GetBrowserStatsUseCase

    @Binds @ViewModelScoped
    abstract fun bindClearBrowserStatsUseCase(impl: ClearBrowserStatsUseCaseImpl): ClearBrowserStatsUseCase
}
