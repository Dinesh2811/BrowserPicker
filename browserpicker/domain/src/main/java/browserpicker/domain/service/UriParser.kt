package browserpicker.domain.service

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import browserpicker.core.results.MyResult
import browserpicker.core.results.UriValidationError
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
) {
    init {
        require(host.isNotBlank()) { "Host cannot be blank." }
        require(scheme.isNotBlank()) { "Scheme cannot be blank." }
    }
}

interface UriParser {
    fun parseAndValidateWebUri(uriString: String, supportedSchemes: Set<String> = DEFAULT_SUPPORTED_SCHEMES): MyResult<ParsedUri?, UriValidationError>

    companion object {
        val DEFAULT_SUPPORTED_SCHEMES: Set<String> = setOf("http", "https")
    }
}

@Singleton
class AndroidUriParser @Inject constructor(): UriParser {
    override fun parseAndValidateWebUri(uriString: String, supportedSchemes: Set<String>): MyResult<ParsedUri?, UriValidationError> {
        require(supportedSchemes.isNotEmpty()) { "At least one supported scheme must be provided." }

        if (uriString.isBlank()) {
            return MyResult.Error(UriValidationError.BlankOrEmpty(message = "URI string cannot be blank or empty."))
        }

        return try {
            val uri = uriString.toUri()
            val scheme = uri.scheme
            val host = uri.host
            when {
                host.isNullOrEmpty() -> MyResult.Error(UriValidationError.Invalid(message = "Host cannot be missing or blank in URI: $uriString"))
                !uri.isAbsolute -> MyResult.Error(UriValidationError.Invalid(message = "URI must be absolute: $uriString"))
                scheme == null || scheme !in supportedSchemes -> MyResult.Error(UriValidationError.Invalid(message = "Invalid or unsupported scheme '$scheme' in URI: $uriString. Only $supportedSchemes are supported."))
                else -> {
                    MyResult.Success(
                        data = ParsedUri(
                            originalString = uriString,
                            scheme = scheme,
                            host = host,
                            path = uri.path,
                            query = uri.query,
                            fragment = uri.fragment,
                            port = uri.port
                        ),
                        message = "Successfully parsed valid URI: $uriString"
                    )
                }
            }
        } catch (e: Exception) {
            MyResult.Error(UriValidationError.Invalid("Failed to parse URI: $uriString", e))
        }
    }

    private fun isValidHostFormat(host: String): Boolean {
        val hostRegex = Regex("^[a-zA-Z0-9.-]+$" )
        return host.isNotBlank() && hostRegex.matches(host)
    }
}
