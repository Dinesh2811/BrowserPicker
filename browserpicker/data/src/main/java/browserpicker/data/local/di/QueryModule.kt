package browserpicker.data.local.di

import browserpicker.data.local.query.UriRecordQueryBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object QueryModule {
    @Provides
    @Singleton
    fun provideUriRecordQueryBuilder(): UriRecordQueryBuilder = UriRecordQueryBuilder()
}