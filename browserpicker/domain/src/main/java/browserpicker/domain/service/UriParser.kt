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

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult

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
    fun parseAndValidateWebUri(uriString: String, supportedSchemes: Set<String> = DEFAULT_SUPPORTED_SCHEMES): DomainResult<ParsedUri?, UriValidationError>

    companion object {
        val DEFAULT_SUPPORTED_SCHEMES: Set<String> = setOf("http", "https")
    }
}

@Singleton
class AndroidUriParser @Inject constructor(): UriParser {
    override fun parseAndValidateWebUri(uriString: String, supportedSchemes: Set<String>): DomainResult<ParsedUri?, UriValidationError> {
        require(supportedSchemes.isNotEmpty()) { "At least one supported scheme must be provided." }

        if (uriString.isBlank()) {
            return DomainResult.Failure(UriValidationError.BlankOrEmpty(message = "URI string cannot be blank or empty."))
        }

        return try {
            val uri = uriString.toUri()
            val scheme = uri.scheme
            val host = uri.host
            when {
                host.isNullOrEmpty() -> DomainResult.Failure(UriValidationError.Invalid(message = "Host cannot be missing or blank in URI: $uriString"))
                !uri.isAbsolute -> DomainResult.Failure(UriValidationError.Invalid(message = "URI must be absolute: $uriString"))
                scheme == null || scheme !in supportedSchemes -> DomainResult.Failure(UriValidationError.Invalid(message = "Invalid or unsupported scheme '$scheme' in URI: $uriString. Only $supportedSchemes are supported."))
                else -> {
                    DomainResult.Success(
                        data = ParsedUri(
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
            }
        } catch (e: Exception) {
            DomainResult.Failure(UriValidationError.Invalid("Failed to parse URI: $uriString", e))
        }
    }

    private fun isValidHostFormat(host: String): Boolean {
        val hostRegex = Regex("^[a-zA-Z0-9.-]+$" )
        return host.isNotBlank() && hostRegex.matches(host)
    }
}
