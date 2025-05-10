package browserpicker.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal
import javax.inject.Named
import javax.inject.Singleton

//https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md#json-configuration
@Module
@InstallIn(SingletonComponent::class)
object JsonModule {
    const val KOTLIN_SERIALIZATION_JSON_CONFIG = "KotlinSerializationJsonConfig_Named1"

//    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
//    @Named(KOTLIN_SERIALIZATION_JSON_CONFIG)
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
            coerceInputValues = true
            explicitNulls = false
            allowStructuredMapKeys = true
            allowSpecialFloatingPointValues = true
//            serializersModule = SerializersModule {
//                contextual(BigDecimal::class, BigDecimalSerializer)
//                contextual(Any::class, AnySerializer)
//            }
        }
    }
}
