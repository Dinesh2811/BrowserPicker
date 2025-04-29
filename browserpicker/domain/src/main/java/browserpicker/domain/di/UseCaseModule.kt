package browserpicker.domain.di

import browserpicker.domain.usecase.ClearBrowserStatsUseCase
import browserpicker.domain.usecase.ClearBrowserStatsUseCaseImpl
import browserpicker.domain.usecase.ClearUriHistoryUseCase
import browserpicker.domain.usecase.ClearUriHistoryUseCaseImpl
import browserpicker.domain.usecase.CreateFolderUseCase
import browserpicker.domain.usecase.CreateFolderUseCaseImpl
import browserpicker.domain.usecase.DeleteFolderUseCase
import browserpicker.domain.usecase.DeleteFolderUseCaseImpl
import browserpicker.domain.usecase.DeleteHostRuleUseCase
import browserpicker.domain.usecase.DeleteHostRuleUseCaseImpl
import browserpicker.domain.usecase.DeleteUriRecordUseCase
import browserpicker.domain.usecase.DeleteUriRecordUseCaseImpl
import browserpicker.domain.usecase.GetBrowserStatsUseCase
import browserpicker.domain.usecase.GetBrowserStatsUseCaseImpl
import browserpicker.domain.usecase.GetFoldersUseCase
import browserpicker.domain.usecase.GetFoldersUseCaseImpl
import browserpicker.domain.usecase.GetHistoryFilterOptionsUseCase
import browserpicker.domain.usecase.GetHistoryFilterOptionsUseCaseImpl
import browserpicker.domain.usecase.GetHistoryOverviewUseCase
import browserpicker.domain.usecase.GetHistoryOverviewUseCaseImpl
import browserpicker.domain.usecase.GetHostRuleUseCase
import browserpicker.domain.usecase.GetHostRuleUseCaseImpl
import browserpicker.domain.usecase.GetHostRulesUseCase
import browserpicker.domain.usecase.GetHostRulesUseCaseImpl
import browserpicker.domain.usecase.GetPagedUriHistoryUseCase
import browserpicker.domain.usecase.GetPagedUriHistoryUseCaseImpl
import browserpicker.domain.usecase.HandleInterceptedUriUseCase
import browserpicker.domain.usecase.HandleInterceptedUriUseCaseImpl
import browserpicker.domain.usecase.InitializeAppDefaultsUseCase
import browserpicker.domain.usecase.InitializeAppDefaultsUseCaseImpl
import browserpicker.domain.usecase.RecordUriInteractionUseCase
import browserpicker.domain.usecase.RecordUriInteractionUseCaseImpl
import browserpicker.domain.usecase.SaveHostRuleUseCase
import browserpicker.domain.usecase.SaveHostRuleUseCaseImpl
import browserpicker.domain.usecase.UpdateFolderUseCase
import browserpicker.domain.usecase.UpdateFolderUseCaseImpl
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
