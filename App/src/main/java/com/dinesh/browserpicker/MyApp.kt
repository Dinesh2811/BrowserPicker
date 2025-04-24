package com.dinesh.browserpicker

import android.app.Application
import com.dinesh.m3theme.AppConfig
import com.dinesh.m3theme.CustomLogger
import com.dinesh.m3theme.LogLevel
import com.dinesh.m3theme.log
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
