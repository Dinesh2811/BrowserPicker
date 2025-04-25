package browserpicker.core.utils

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

fun LogLevel.log(message: String, TAG: String = getCallerClassName()) {
    val finalTag = if (TAG == getCallerClassName()) {
        if (TAG.startsWith("log_")) { TAG } else { "log_$TAG" }
    } else {
        "${TAG}_${getCallerClassName()}"
    }
    CustomLogger.getInstance()?.log(this, finalTag, message)
}

fun logDebug(message: String, TAG: String = getCallerClassName()) {
    LogLevel.Debug.log(message, TAG)
}

fun logInfo(message: String, TAG: String = getCallerClassName()) {
    LogLevel.Info.log(message, TAG)
}

fun logWarning(message: String, TAG: String = getCallerClassName()) {
    LogLevel.Warning.log(message, TAG)
}

fun logError(message: String, throwable: Throwable? = null, TAG: String = getCallerClassName()) {
    LogLevel.Error.log(message, TAG)
}

fun logVerbose(message: String, TAG: String = getCallerClassName()) {
    LogLevel.Verbose.log(message, TAG)
}

fun logAssert(message: String, TAG: String = getCallerClassName()) {
    LogLevel.Assert.log(message, TAG)
}

private fun getCallerClassName(defaultTag: String = "log_", callerFilter: (StackTraceElement) -> Boolean = ::defaultCallerFilter): String {
    return try {
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.firstOrNull { callerFilter(it) }
        caller?.let {
            val className = it.className.substringAfterLast('.')
            val methodName = it.methodName
            if (methodName.isNullOrEmpty()) className else "${className}_$methodName"
        } ?: defaultTag
    } catch (e: Exception) {
        Log.e(defaultTag, "getCallerClassName: Error getting caller class name", e)
        defaultTag
    }
}

private fun defaultCallerFilter(element: StackTraceElement): Boolean {
    return element.className !in listOf(
        Thread::class.java.name,
        CustomLogger::class.java.name,
    ) && !element.className.contains("LoggingExtensions") &&
            !element.className.contains("VMStack") &&
            !element.className.contains("kotlinx.coroutines") &&
            !element.className.startsWith("java.lang") &&
            !element.className.contains("CustomLogger")
}

@Singleton
class CustomLogger @Inject constructor() {
    companion object {
        private const val MAX_LOG_LENGTH = 4000

        // Singleton instance of the CustomLogger class. It's marked as volatile to ensure thread safety.
        @Volatile
        private var instance: CustomLogger? = null

        /**
         * Initializes the singleton instance of the CustomLogger class.
         * This method is used to provide an instance of CustomLogger.
         *
         * @param customLogger An instance of CustomLogger.
         */
        fun init(customLogger: CustomLogger?) {
            instance = customLogger
        }

        /**
         * Returns the singleton instance of the CustomLogger class.
         * If the instance is not initialized, it returns null.
         *
         * @return The singleton instance of the CustomLogger class or null if it's not initialized.
         */
        fun getInstance(): CustomLogger? {
            return instance
        }
    }

    fun log(logLevel: LogLevel, tag: String, message: String) {
        if (AppConfig.isLoggingEnable) {
            if (message.length > MAX_LOG_LENGTH) {
                logLongMessage(logLevel, tag, message)
            } else {
                logMessage(logLevel, tag, message)
            }
        }
    }

    private fun logLongMessage(logLevel: LogLevel, tag: String, message: String) {
        var start = 0
        while (start < message.length) {
            val end = start + MAX_LOG_LENGTH
            val chunk = message.substring(start, end.coerceAtMost(message.length))
            logMessage(logLevel, tag, chunk)
            start = end
        }
    }

    private fun logMessage(logLevel: LogLevel, tag: String, message: String) {
        when (logLevel) {
            LogLevel.Debug -> Log.d(tag, message)
            LogLevel.Info -> Log.i(tag, message)
            LogLevel.Warning -> Log.w(tag, message)
            LogLevel.Error -> Log.e(tag, message)
            LogLevel.Verbose -> Log.v(tag, message)
            LogLevel.Assert -> Log.wtf(tag, message)
        }
    }
}

sealed interface LogLevel {
    data object Debug: LogLevel
    data object Info: LogLevel
    data object Warning: LogLevel
    data object Error: LogLevel
    data object Verbose: LogLevel
    data object Assert: LogLevel
}
