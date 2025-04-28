package browserpicker.domain.di

import browserpicker.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


// --- Use Cases can often be scoped shorter than Singleton ---
// ViewModelScoped is common if UseCases hold no state and are used by ViewModels.
// Use Singleton if they truly need to be single instances across the app lifecycle.
// Let's start with ViewModelScoped as a reasonable default. Adjust if needed.

@Module
@InstallIn(ViewModelComponent::class) // Scope to ViewModel lifecycle
object UseCaseModule {

    // Initialization (Could be Singleton if needed early)
    @Provides
    @ViewModelScoped
    fun provideInitializeAppDefaultsUseCase(impl: InitializeAppDefaultsUseCaseImpl): InitializeAppDefaultsUseCase = impl

    // URI Handling & History
    @Provides
    @ViewModelScoped
    fun provideHandleInterceptedUriUseCase(impl: HandleInterceptedUriUseCaseImpl): HandleInterceptedUriUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideGetPagedUriHistoryUseCase(impl: GetPagedUriHistoryUseCaseImpl): GetPagedUriHistoryUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideGetHistoryOverviewUseCase(impl: GetHistoryOverviewUseCaseImpl): GetHistoryOverviewUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideRecordUriInteractionUseCase(impl: RecordUriInteractionUseCaseImpl): RecordUriInteractionUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideGetHistoryFilterOptionsUseCase(impl: GetHistoryFilterOptionsUseCaseImpl): GetHistoryFilterOptionsUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideDeleteUriRecordUseCase(impl: DeleteUriRecordUseCaseImpl): DeleteUriRecordUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideClearUriHistoryUseCase(impl: ClearUriHistoryUseCaseImpl): ClearUriHistoryUseCase = impl

    // Host Rules
    @Provides
    @ViewModelScoped
    fun provideGetHostRuleUseCase(impl: GetHostRuleUseCaseImpl): GetHostRuleUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideGetHostRulesUseCase(impl: GetHostRulesUseCaseImpl): GetHostRulesUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideSaveHostRuleUseCase(impl: SaveHostRuleUseCaseImpl): SaveHostRuleUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideDeleteHostRuleUseCase(impl: DeleteHostRuleUseCaseImpl): DeleteHostRuleUseCase = impl

    // Folders
    @Provides
    @ViewModelScoped
    fun provideGetFoldersUseCase(impl: GetFoldersUseCaseImpl): GetFoldersUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideCreateFolderUseCase(impl: CreateFolderUseCaseImpl): CreateFolderUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideUpdateFolderUseCase(impl: UpdateFolderUseCaseImpl): UpdateFolderUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideDeleteFolderUseCase(impl: DeleteFolderUseCaseImpl): DeleteFolderUseCase = impl

    // Browser Stats
    @Provides
    @ViewModelScoped
    fun provideGetBrowserStatsUseCase(impl: GetBrowserStatsUseCaseImpl): GetBrowserStatsUseCase = impl

    @Provides
    @ViewModelScoped
    fun provideClearBrowserStatsUseCase(impl: ClearBrowserStatsUseCaseImpl): ClearBrowserStatsUseCase = impl

}

// NOTE: If InitializeAppDefaultsUseCase needs to run very early, maybe from Application#onCreate,
// then IT should be @Singleton and installed in SingletonComponent.
// The rest are typically fine as @ViewModelScoped.