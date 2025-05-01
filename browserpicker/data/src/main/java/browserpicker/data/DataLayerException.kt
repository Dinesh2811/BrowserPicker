package browserpicker.data

import browserpicker.domain.model.FolderType

sealed class DataLayerException(message: String? = null, cause: Throwable? = null): RuntimeException(message, cause)

data class InvalidInputDataException(
    override val message: String,
    override val cause: Throwable? = null,
): DataLayerException(message, cause)

data class DataNotFoundException(
    override val message: String,
    override val cause: Throwable? = null,
): DataLayerException(message, cause)

data class DataIntegrityException(
    override val message: String,
    override val cause: Throwable? = null,
): DataLayerException(message, cause)

data class FolderNotEmptyException(
    val folderId: Long,
    override val message: String = "Folder with ID $folderId is not empty and cannot be deleted.",
    override val cause: Throwable? = null,
): DataLayerException(message, cause)

data class InvalidFolderTypeException(
    val folderId: Long,
    val expectedType: FolderType,
    val actualType: FolderType,
    override val message: String = "Folder with ID $folderId has unexpected type. Expected $expectedType but was $actualType.",
    override val cause: Throwable? = null,
): DataLayerException(message, cause)

// Add other specific data layer exceptions as needed based on my UseCase.