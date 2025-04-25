//package browserpicker.playground.browserpicker.core.common.utils
//
//import android.content.pm.PackageManager
//
///**
// * A sealed interface representing specific errors that can occur within the application.
// * This allows for exhaustive handling of known error types.
// */
//sealed interface AppError {
//    val message: String
//    val cause: Throwable? // Optional underlying cause
//
//    /** Represents errors originating from data sources. */
//    sealed interface DataSourceError : AppError {
//        data class resolutionFailed(
//            override val message: String,
//            override val cause: Throwable? = null
//        ) : DataSourceError
//
//        data class PermissionDenied(
//            override val message: String = "Permission required to query packages is missing.",
//            override val cause: Throwable? = null
//        ) : DataSourceError
//
//        data class PackageInfoError(
//            val packageName: String?,
//            override val message: String,
//            override val cause: PackageManager.NameNotFoundException? = null
//        ) : DataSourceError
//    }
//
//    /** Represents errors originating from the repository layer (e.g., mapping issues). */
//    sealed interface RepositoryError : AppError {
//        data class MappingError(
//            override val message: String,
//            override val cause: Throwable? = null
//        ) : RepositoryError
//    }
//
//    /** Represents errors originating from the domain/use case layer. */
//    sealed interface DomainError : AppError {
//        data class NoBrowsersFound(
//            override val message: String = "No suitable browser applications were found installed on the device.",
//            override val cause: Throwable? = null
//        ) : DomainError
//    }
//
//    /** A generic error for unexpected situations. */
//    data class UnknownError(
//        override val message: String = "An unexpected error occurred.",
//        override val cause: Throwable? = null
//    ) : AppError
//}
//
//// Helper function to create a user-readable description
//fun AppError.getUserFriendlyMessage(): String {
//    return when (this) {
//        is AppError.DataSourceError.PermissionDenied -> "Cannot list browsers: Missing required permissions."
//        is AppError.DataSourceError.resolutionFailed -> "Failed to query installed applications."
//        is AppError.DataSourceError.PackageInfoError -> "Could not load details for a specific browser."
//        is AppError.RepositoryError.MappingError -> "Error processing browser information."
//        is AppError.DomainError.NoBrowsersFound -> this.message // Use the specific message
//        is AppError.UnknownError -> this.message
//    }
//}
