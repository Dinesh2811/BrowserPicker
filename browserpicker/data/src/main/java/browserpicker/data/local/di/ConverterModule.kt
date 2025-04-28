//package browserpicker.data.local.di
//
//import browserpicker.data.local.db.FolderTypeConverter
//import browserpicker.data.local.db.InstantConverter
//import browserpicker.data.local.db.InteractionActionConverter
//import browserpicker.data.local.db.UriSourceConverter
//import browserpicker.data.local.db.UriStatusConverter
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object BrowserPickerConverterModule {
//    @Provides @Singleton
//    fun provideInstantConverter(): InstantConverter = InstantConverter()
//
//    @Provides @Singleton
//    fun provideUriSourceConverter() = InstantConverter()
//
//    @Provides @Singleton
//    fun provideInteractionActionConverter() = InstantConverter()
//
//    @Provides @Singleton
//    fun provideUriStatusConverter() = InstantConverter()
//
//    @Provides @Singleton
//    fun provideFolderTypeConverter() = InstantConverter()
//}
