package browserpicker.domain.di

import browserpicker.domain.service.AndroidUriParser
import browserpicker.domain.service.UriParser
import browserpicker.domain.usecase.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UtilModule {

    @Binds @Singleton
    abstract fun bindUriParser(impl: AndroidUriParser): UriParser
}
