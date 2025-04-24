package com.dinesh.m3theme.di

import com.dinesh.m3theme.data.datasource.local.ThemeLocalDataSource
import com.dinesh.m3theme.data.datasource.local.ThemeLocalDataSourceImpl
import com.dinesh.m3theme.data.repository.ThemeRepository
import com.dinesh.m3theme.data.repository.ThemeRepositoryImpl
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
    abstract fun bindThemeRepository(themeRepositoryImpl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindThemeLocalDataSource(themeLocalDataSourceImpl: ThemeLocalDataSourceImpl): ThemeLocalDataSource
}