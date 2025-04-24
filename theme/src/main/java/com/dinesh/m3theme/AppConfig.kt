package com.dinesh.m3theme

object AppConfig {
    val isLoggingEnable: Boolean
        get() {
            return com.dinesh.m3theme.BuildConfig.DEBUG
//            return true
        }
}