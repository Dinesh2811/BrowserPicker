package browserpicker.domain.service

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import browserpicker.domain.model.UriRecord

@Immutable
@Serializable
@Keep
data class ParsedUri(
    val originalString: String,
    val scheme: String,
    val host: String,
    val path: String?,
    val query: String?,
    val fragment: String?,
    val port: Int = -1,
)

interface UriParser {
    fun parseAndValidateWebUri(uriString: String, supportedSchemes: List<String> = listOf("http", "https")): Result<ParsedUri?>
}

@Singleton
class AndroidUriParser @Inject constructor(): UriParser {
    override fun parseAndValidateWebUri(uriString: String, supportedSchemes: List<String>): Result<ParsedUri?> {
        if (uriString.isBlank()) {
            return Result.success(null)
        }

        if (!UriRecord.isValidUri(uriString)) {
            return Result.failure(Exception("Invalid URI format."))
        }

        return try {
            val uri = uriString.toUri()

            val scheme = uri.scheme
            if (scheme == null || scheme !in supportedSchemes) {
                return Result.failure(Exception("Invalid scheme. Only $supportedSchemes are supported."))
            }

            val host = uri.host
            if (host.isNullOrBlank()) {
                return Result.failure(Exception("Host is missing or blank."))
            }

            Result.success(
                ParsedUri(
                    originalString = uriString,
                    scheme = scheme,
                    host = host,
                    path = uri.path,
                    query = uri.query,
                    fragment = uri.fragment,
                    port = uri.port
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse URI: $uriString")
            Result.failure(e)
        }
    }
}
