package browserpicker.domain.service

import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import androidx.paging.PagingConfig
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import timber.log.Timber

@Immutable // Indicate this is stable for Compose
@Serializable // Optional, but good practice if passing around (e.g. Nav Args)
data class ParsedUri(
    val originalString: String,
    val scheme: String, // e.g., "http", "https"
    val host: String, // The host part, guaranteed non-blank for web URIs
    val path: String?, // Path including leading slash, if present
    val query: String?, // Raw query string, if present
    val fragment: String?, // Fragment, if present
    val port: Int = -1 // Port, -1 if not specified
)

interface UriParser {
    /**
     * Parses and validates a URI string.
     * @param uriString The raw URI string.
     * @return A [Result] containing a [ParsedUri] if valid and parsable as a web URI,
     *         or a [DomainError] if invalid or not a supported web URI (http/https).
     */
    fun parseAndValidateWebUri(uriString: String): Result<ParsedUri?>
}

@Singleton
class AndroidUriParser @Inject constructor() : UriParser {
    override fun parseAndValidateWebUri(uriString: String): Result<ParsedUri?> {
        if (uriString.isBlank()) {
//            return Result.failure(DomainError.Validation("URI string is empty."))
        }

        return try {
            val uri = uriString.toUri()

            val scheme = uri.scheme
            if (scheme == null || (scheme != "http" && scheme != "https")) {
                return Result.failure(exception = Exception("Invalid scheme. Only 'http' and 'https' are supported."))
            }

            val host = uri.host
            if (host.isNullOrBlank()) {
                return Result.failure(exception = Exception("Host is missing or blank."))
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

sealed interface DomainError {
    data class Validation(val message: String) : DomainError
    data class NotFound(val entityType: String, val identifier: String) : DomainError
    data class Conflict(val message: String) : DomainError // e.g., unique constraint violation
    data class Database(val underlyingCause: Throwable?) : DomainError
    data class Unexpected(val message: String, val cause: Throwable? = null) : DomainError
    data class Custom(val message: String) : DomainError // For specific business rule failures
}

// Helper to convert Throwable to DomainError within Use Cases
fun Throwable.toDomainError(defaultMessage: String = "An unexpected error occurred"): DomainError {
    return when (this) {
        is IllegalArgumentException -> DomainError.Validation(this.message ?: "Invalid input.")
        is IllegalStateException -> DomainError.Conflict(this.message ?: "Operation conflict.")
        // Add more specific exception mappings if repositories throw custom exceptions
        else -> DomainError.Unexpected(this.message ?: defaultMessage, this)
    }
}

// Common Paging Configuration (can be defined here or passed from Presentation)
// Defining defaults here provides consistency.
object PagingDefaults {
    val DEFAULT_PAGING_CONFIG = PagingConfig(
        pageSize = 30, // Sensible default page size
        prefetchDistance = 10, // Load items 10 away from edge
        enablePlaceholders = false, // Usually false for network/db PagingSources
        initialLoadSize = 60 // Load more initially for better perceived performance
    )
}