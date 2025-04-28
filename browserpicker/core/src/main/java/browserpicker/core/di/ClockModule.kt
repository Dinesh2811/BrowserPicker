package browserpicker.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock
import javax.inject.Singleton
import kotlinx.datetime.Instant
import javax.inject.Inject

interface InstantProvider {
    fun now(): Instant
}

@Singleton
class SystemClockInstantProvider @Inject constructor() : InstantProvider {
    override fun now(): Instant = Clock.System.now()
}

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
}

@Module
@InstallIn(SingletonComponent::class)
abstract class InstantProviderModule {
    @Binds
    @Singleton
    abstract fun bindInstantProvider(impl: SystemClockInstantProvider): InstantProvider
}