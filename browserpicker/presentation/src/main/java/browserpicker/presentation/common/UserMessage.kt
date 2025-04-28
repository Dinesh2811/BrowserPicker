package browserpicker.presentation.common

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * Represents a message to be shown temporarily to the user (e.g., Snackbar, Toast).
 * @param id Unique ID to manage messages in lists.
 * @param message The text content of the message.
 * @param type Indicates if it's an error or success message for potential styling.
 */
@Immutable
data class UserMessage(
    val id: Long = UUID.randomUUID().mostSignificantBits,
    val message: String,
    val type: MessageType = MessageType.INFO
)

enum class MessageType { INFO, SUCCESS, ERROR }

/** Generic loading state enum */
enum class LoadingStatus { IDLE, LOADING, SUCCESS, ERROR }
