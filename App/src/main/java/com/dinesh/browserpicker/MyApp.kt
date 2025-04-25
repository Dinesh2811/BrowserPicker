package com.dinesh.browserpicker

import android.app.Application
import browserpicker.core.AppConfig
import browserpicker.core.CustomLogger
import browserpicker.core.LogLevel
import browserpicker.core.log
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp: Application() {
    @Inject lateinit var customLogger: CustomLogger

    override fun onCreate() {
        super.onCreate()
        LogLevel.Debug.log("MyApp initialized")
        if (AppConfig.isLoggingEnable) {
            CustomLogger.init(customLogger)
        } else {
            CustomLogger.init(null)
        }
    }
}
