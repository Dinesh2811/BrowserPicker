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
@InstallIn(SingletonComponent::class) // Install in ViewModelComponent
abstract class BrowserPickerUseCaseModule { // Must be an abstract class for @Binds

    // InitializeAppDefaultsUseCase could arguably be Singleton if needed early,
    // but let's keep it ViewModelScoped for consistency with others here.
    // If it were Singleton, you'd use @InstallIn(SingletonComponent::class) and @Singleton
    @Binds // Bind the interface to the implementation
    @Singleton // Apply the scope annotation to the binding method
    abstract fun bindInitializeAppDefaultsUseCase(
        impl: InitializeAppDefaultsUseCaseImpl
    ): InitializeAppDefaultsUseCase


    // URI Handling & History
    @Binds
    @Singleton
    abstract fun bindHandleInterceptedUriUseCase(
        impl: HandleInterceptedUriUseCaseImpl
    ): HandleInterceptedUriUseCase

    @Binds
    @Singleton
    abstract fun bindGetPagedUriHistoryUseCase(
        impl: GetPagedUriHistoryUseCaseImpl
    ): GetPagedUriHistoryUseCase

    @Binds
    @Singleton
    abstract fun bindGetHistoryOverviewUseCase(
        impl: GetHistoryOverviewUseCaseImpl
    ): GetHistoryOverviewUseCase

    @Binds
    @Singleton
    abstract fun bindRecordUriInteractionUseCase(
        impl: RecordUriInteractionUseCaseImpl
    ): RecordUriInteractionUseCase

    @Binds
    @Singleton
    abstract fun bindGetHistoryFilterOptionsUseCase(
        impl: GetHistoryFilterOptionsUseCaseImpl
    ): GetHistoryFilterOptionsUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteUriRecordUseCase(
        impl: DeleteUriRecordUseCaseImpl
    ): DeleteUriRecordUseCase

    @Binds
    @Singleton
    abstract fun bindClearUriHistoryUseCase(
        impl: ClearUriHistoryUseCaseImpl
    ): ClearUriHistoryUseCase


    // Host Rules
    @Binds
    @Singleton
    abstract fun bindGetHostRuleUseCase(
        impl: GetHostRuleUseCaseImpl
    ): GetHostRuleUseCase

    @Binds
    @Singleton
    abstract fun bindGetHostRulesUseCase(
        impl: GetHostRulesUseCaseImpl
    ): GetHostRulesUseCase

    @Binds
    @Singleton
    abstract fun bindSaveHostRuleUseCase(
        impl: SaveHostRuleUseCaseImpl
    ): SaveHostRuleUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteHostRuleUseCase(
        impl: DeleteHostRuleUseCaseImpl
    ): DeleteHostRuleUseCase


    // Folders
    @Binds
    @Singleton
    abstract fun bindGetFoldersUseCase(
        impl: GetFoldersUseCaseImpl
    ): GetFoldersUseCase

    @Binds
    @Singleton
    abstract fun bindCreateFolderUseCase(
        impl: CreateFolderUseCaseImpl
    ): CreateFolderUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateFolderUseCase(
        impl: UpdateFolderUseCaseImpl
    ): UpdateFolderUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteFolderUseCase(
        impl: DeleteFolderUseCaseImpl
    ): DeleteFolderUseCase


    // Browser Stats
    @Binds
    @Singleton
    abstract fun bindGetBrowserStatsUseCase(
        impl: GetBrowserStatsUseCaseImpl
    ): GetBrowserStatsUseCase

    @Binds
    @Singleton
    abstract fun bindClearBrowserStatsUseCase(
        impl: ClearBrowserStatsUseCaseImpl
    ): ClearBrowserStatsUseCase

}