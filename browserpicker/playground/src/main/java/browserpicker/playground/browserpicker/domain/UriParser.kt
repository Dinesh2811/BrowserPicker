package browserpicker.playground.browserpicker.domain

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.*

@Immutable @Serializable @Keep
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
    fun parseAndValidateWebUri(uriString: String, supportedSchemes: Set<String> = DEFAULT_SUPPORTED_SCHEMES): Result<ParsedUri?>

    companion object {
        val DEFAULT_SUPPORTED_SCHEMES: Set<String> = setOf("http", "https")
    }
}

@Singleton
class AndroidUriParser @Inject constructor() : UriParser {
    override fun parseAndValidateWebUri(uriString: String, supportedSchemes: Set<String>): Result<ParsedUri?> {
        require(supportedSchemes.isNotEmpty()) { "At least one supported scheme must be provided." }
        if (uriString.isBlank()) return Result.success(null)

        return try {
            val uri = uriString.toUri()
            val scheme = uri.scheme
            val host = uri.host
            when {
                host.isNullOrEmpty() -> Result.failure(IllegalArgumentException("Host cannot be missing or blank in URI: $uriString"))
                !uri.isAbsolute -> Result.failure(IllegalArgumentException("URI must be absolute: $uriString"))
                scheme == null || scheme !in supportedSchemes -> Result.failure(IllegalArgumentException("Invalid or unsupported scheme '$scheme' in URI: $uriString. Only $supportedSchemes are supported."))
                else -> Result.success(
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
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse URI: $uriString")
            Result.failure(e)
        }
    }
}
