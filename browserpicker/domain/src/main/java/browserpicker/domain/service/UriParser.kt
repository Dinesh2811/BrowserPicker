package browserpicker.domain.service

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.core.net.toUri
import browserpicker.core.results.DomainResult
import browserpicker.core.results.UriValidationError
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.to

data class ParsedUri(
    val originalString: String,
    val originalUri: Uri,
    val scheme: String,
    val host: String,
) {
    init {
        require(host.isNotBlank()) { "Host cannot be blank." }
        require(scheme.isNotBlank()) { "Scheme cannot be blank." }
    }

    companion object {
        val ParsedUri.isSecure
            get() = this.scheme == "https"

        val ParsedUri?.uriInfoBar get() = when {
            this?.originalUri == null -> Icons.AutoMirrored.Filled.HelpOutline to "No URL provided"
            this.scheme == "https" -> Icons.Filled.Lock to "Secure connection (HTTPS)"
            this.scheme == "http" -> Icons.Filled.Warning to "Insecure connection (HTTP)"
            else -> Icons.Filled.Link to "Connection type unknown"
        }
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
            //val path = uri.path?.takeIf { it.isNotEmpty() && it != "/" }
            //val displayText = host?.let { h -> path?.let { p -> "$h$p" }?: h }?: uri.toString()

            when {
                host.isNullOrEmpty() -> DomainResult.Failure(UriValidationError.Invalid(message = "Host cannot be missing or blank in URI: $uriString"))
                !uri.isAbsolute -> DomainResult.Failure(UriValidationError.Invalid(message = "URI must be absolute: $uriString"))
                scheme == null || scheme !in supportedSchemes -> DomainResult.Failure(UriValidationError.Invalid(message = "Invalid or unsupported scheme '$scheme' in URI: $uriString. Only $supportedSchemes are supported."))
                else -> {
                    DomainResult.Success(
                        data = ParsedUri(
                            originalString = uriString,
                            originalUri = uriString.toUri(),
                            scheme = scheme,
                            host = host,
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
