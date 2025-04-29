package com.dinesh.browserpicker.mock

import browserpicker.domain.model.*

data class MockDatabaseData(
    val folders: List<Folder>,
    val browserStats: List<BrowserUsageStat>,
    val hostRules: List<HostRule>,
    val uriRecords: List<UriRecord>
)
