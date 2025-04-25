package browserpicker.core

object AppConfig {
    val isLoggingEnable: Boolean
        get() {
            return browserpicker.core.BuildConfig.DEBUG
//            return true
        }
}