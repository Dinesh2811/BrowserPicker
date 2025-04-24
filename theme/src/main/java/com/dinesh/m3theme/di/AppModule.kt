package com.dinesh.m3theme.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import javax.inject.Qualifier

//@Retention(AnnotationRetention.BINARY)
//@Qualifier
//annotation class IoDispatcher
//
//@Retention(AnnotationRetention.BINARY)
//@Qualifier
//annotation class DefaultDispatcher
//
//@Module
//@InstallIn(SingletonComponent::class)
//object Dispatchers {
//    @Provides
//    @Singleton // Dispatchers are singletons
//    @IoDispatcher // Use the qualifier
//    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
//
//    @Provides
//    @Singleton // Dispatchers are singletons
//    @DefaultDispatcher // Use the qualifier
//    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
//}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}
