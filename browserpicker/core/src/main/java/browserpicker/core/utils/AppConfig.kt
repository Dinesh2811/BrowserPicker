package browserpicker.core.utils

import browserpicker.core.BuildConfig

object AppConfig {
    val isLoggingEnable: Boolean
        get() {
            return BuildConfig.DEBUG
//            return true
        }
}