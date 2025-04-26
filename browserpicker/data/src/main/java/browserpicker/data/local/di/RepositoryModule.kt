package browserpicker.data.local.di

import browserpicker.data.local.repository.BrowserPickerRepositoryImpl
import browserpicker.domain.repository.BrowserPickerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBrowserPickerRepository(impl: BrowserPickerRepositoryImpl): BrowserPickerRepository
}
