package browserpicker.domain.di

import browserpicker.domain.usecase.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module // Declare this as a Hilt module
@InstallIn(ViewModelComponent::class) // Install in ViewModelComponent
abstract class UseCaseModule { // Must be an abstract class for @Binds

    // InitializeAppDefaultsUseCase could arguably be Singleton if needed early,
    // but let's keep it ViewModelScoped for consistency with others here.
    // If it were Singleton, you'd use @InstallIn(SingletonComponent::class) and @Singleton
    @Binds // Bind the interface to the implementation
    @ViewModelScoped // Apply the scope annotation to the binding method
    abstract fun bindInitializeAppDefaultsUseCase(
        impl: InitializeAppDefaultsUseCaseImpl
    ): InitializeAppDefaultsUseCase


    // URI Handling & History
    @Binds
    @ViewModelScoped
    abstract fun bindHandleInterceptedUriUseCase(
        impl: HandleInterceptedUriUseCaseImpl
    ): HandleInterceptedUriUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindGetPagedUriHistoryUseCase(
        impl: GetPagedUriHistoryUseCaseImpl
    ): GetPagedUriHistoryUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindGetHistoryOverviewUseCase(
        impl: GetHistoryOverviewUseCaseImpl
    ): GetHistoryOverviewUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindRecordUriInteractionUseCase(
        impl: RecordUriInteractionUseCaseImpl
    ): RecordUriInteractionUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindGetHistoryFilterOptionsUseCase(
        impl: GetHistoryFilterOptionsUseCaseImpl
    ): GetHistoryFilterOptionsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDeleteUriRecordUseCase(
        impl: DeleteUriRecordUseCaseImpl
    ): DeleteUriRecordUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindClearUriHistoryUseCase(
        impl: ClearUriHistoryUseCaseImpl
    ): ClearUriHistoryUseCase


    // Host Rules
    @Binds
    @ViewModelScoped
    abstract fun bindGetHostRuleUseCase(
        impl: GetHostRuleUseCaseImpl
    ): GetHostRuleUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindGetHostRulesUseCase(
        impl: GetHostRulesUseCaseImpl
    ): GetHostRulesUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindSaveHostRuleUseCase(
        impl: SaveHostRuleUseCaseImpl
    ): SaveHostRuleUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDeleteHostRuleUseCase(
        impl: DeleteHostRuleUseCaseImpl
    ): DeleteHostRuleUseCase


    // Folders
    @Binds
    @ViewModelScoped
    abstract fun bindGetFoldersUseCase(
        impl: GetFoldersUseCaseImpl
    ): GetFoldersUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindCreateFolderUseCase(
        impl: CreateFolderUseCaseImpl
    ): CreateFolderUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindUpdateFolderUseCase(
        impl: UpdateFolderUseCaseImpl
    ): UpdateFolderUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindDeleteFolderUseCase(
        impl: DeleteFolderUseCaseImpl
    ): DeleteFolderUseCase


    // Browser Stats
    @Binds
    @ViewModelScoped
    abstract fun bindGetBrowserStatsUseCase(
        impl: GetBrowserStatsUseCaseImpl
    ): GetBrowserStatsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindClearBrowserStatsUseCase(
        impl: ClearBrowserStatsUseCaseImpl
    ): ClearBrowserStatsUseCase

}