package com.dinesh.browserpicker.v1.domain

import androidx.compose.runtime.Immutable

@Immutable
data class BrowserAppInfo(
    val appName: String,
    val packageName: String,
)