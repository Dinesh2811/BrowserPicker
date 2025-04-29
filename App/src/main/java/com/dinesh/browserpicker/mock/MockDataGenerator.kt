package com.dinesh.browserpicker.mock

import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.data.local.dao.*
import browserpicker.data.local.entity.*
import browserpicker.domain.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Singleton
class MockDataGenerator @Inject constructor(
    private val mockBrowserPickerDatabaseDao: MockBrowserPickerDatabaseDao,
    private val instantProvider: InstantProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val schemes = listOf("https", "http")
    private val commonHosts = listOf(
        "google.com", "youtube.com", "facebook.com", "amazon.com", "wikipedia.org",
        "twitter.com", "instagram.com", "reddit.com", "linkedin.com", "netflix.com",
        "microsoft.com", "apple.com", "github.com", "stackoverflow.com", "yahoo.com",
        "bing.com", "live.com", "office.com", "nytimes.com", "cnn.com", "bbc.com",
        "ebay.com", "pinterest.com", "tumblr.com", "wordpress.org", "blogger.com",
        "mozilla.org", "duckduckgo.com", "slack.com", "discord.com", "zoom.us"
    )
    private val commonTlds = listOf(".com", ".org", ".net", ".co", ".io", ".dev", ".app", ".uk", ".de", ".jp")
    private val paths = listOf(
        "/", "/search", "/watch", "/posts", "/products", "/wiki", "/status", "/explore",
        "/jobs", "/login", "/settings", "/profile", "/news", "/article", "/questions",
        "/repositories", "/issues", "/blog", "/docs", "/download", "/images", "/videos",
        "/users", "/groups", "/events"
    )
    private val queryParams = listOf(
        "q=", "v=", "id=", "ref=", "page=", "user=", "query=", "lang=", "category=", "tag=", "sort="
    )
    private val browsers = listOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.microsoft.emmx", // Edge
        "com.vivaldi.browser"
    )
    private val folderNames = listOf(
        "Work", "Personal", "Shopping", "News", "Social Media", "Tech", "Travel",
        "Recipes", "Finance", "Urgent", "Later", "Research", "Blocked Sites",
        "Competitors", "Internal Tools", "Archived"
    )
    private val randomWords = listOf(
        "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa",
        "lambda", "mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon",
        "phi", "chi", "psi", "omega", "red", "blue", "green", "yellow", "cyan", "magenta",
        "apple", "banana", "cherry", "date", "elderberry", "fig", "grape", "honeydew", "kiwi"
    )

    // --- State Tracking ---
    private val generatedFolderIds = mutableMapOf<Long, FolderType>() // ID -> Type
    private val generatedHostRuleIds = mutableMapOf<String, Long>() // Host -> ID
    private val hostRuleDetails = mutableMapOf<Long, HostRuleEntity>() // Rule ID -> Full Rule

    /**
     * Clears all data from the relevant tables.
     */
    suspend fun clearAllData() = withContext(ioDispatcher) {
        Timber.d("Clearing all mock data...")
        // Delete in reverse order of dependencies (or let Room handle cascade if configured)
        mockBrowserPickerDatabaseDao.deleteAllUriRecords()
        // Cannot directly delete folders if rules point to them (SET NULL).
        // Need to clear folder IDs from rules first if we wanted to delete folders entirely
        // but rules might still exist pointing to null.
        // Clearing rules first is safer if we also want to clear folders.
        mockBrowserPickerDatabaseDao.deleteAllHostRules() // Custom query needed in DAO
        mockBrowserPickerDatabaseDao.deleteAllFolders() // Custom query needed in DAO
        mockBrowserPickerDatabaseDao.deleteAllStats()
        generatedFolderIds.clear()
        generatedHostRuleIds.clear()
        hostRuleDetails.clear()
        Timber.d("Mock data cleared.")
    }

    /**
     * Generates mock data for all entities.
     * @param uriRecordCount The approximate number of history records to generate.
     * @param hostRuleCount The number of specific host rules to generate.
     * @param folderCount The number of additional folders (beyond defaults) to generate.
     */
    suspend fun generate(
        uriRecordCount: Int = 1000,
        hostRuleCount: Int = 50,
        folderCount: Int = 10
    ) = withContext(ioDispatcher) {
        Timber.d("Starting mock data generation...")
        clearAllData() // Start fresh

        val startTime = instantProvider.now()

        // 1. Generate Folders
        generateFolders(folderCount)
        Timber.d("Generated ${generatedFolderIds.size} folders.")

        // 2. Generate Host Rules
        generateHostRules(hostRuleCount)
        Timber.d("Generated ${generatedHostRuleIds.size} host rules.")

        // 3. Generate URI Records (History)
        val generatedRecords = generateUriRecords(uriRecordCount)
        Timber.d("Generated ${generatedRecords.size} URI records.")

        // 4. Generate Browser Stats (Derived from URI Records)
        generateBrowserStats(generatedRecords)
        Timber.d("Generated browser stats.")

        val endTime = instantProvider.now()
        Timber.d("Mock data generation finished in ${endTime - startTime}.")
    }

    // --- Helper Methods ---

    private suspend fun generateFolders(count: Int) {
        val now = instantProvider.now()
        val defaultBookmarkFolder = FolderEntity(
            id = Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID,
            name = Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_NAME,
            parentFolderId = null,
            folderType = FolderType.BOOKMARK,
            createdAt = now.minus(30.days), // Make defaults older
            updatedAt = now.minus(29.days)
        )
        val defaultBlockedFolder = FolderEntity(
            id = Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID,
            name = Folder.DEFAULT_BLOCKED_ROOT_FOLDER_NAME,
            parentFolderId = null,
            folderType = FolderType.BLOCK,
            createdAt = now.minus(30.days),
            updatedAt = now.minus(29.days)
        )

        mockBrowserPickerDatabaseDao.insertFoldersIgnoreConflict(listOf(defaultBookmarkFolder, defaultBlockedFolder))
        generatedFolderIds[defaultBookmarkFolder.id] = defaultBookmarkFolder.folderType
        generatedFolderIds[defaultBlockedFolder.id] = defaultBlockedFolder.folderType

        val generatedFolders = mutableListOf<FolderEntity>()
        val existingNames = mutableMapOf<Pair<Long?, FolderType>, MutableSet<String>>()
        existingNames[null to FolderType.BOOKMARK] = mutableSetOf(defaultBookmarkFolder.name.lowercase())
        existingNames[null to FolderType.BLOCK] = mutableSetOf(defaultBlockedFolder.name.lowercase())


        for (i in 0 until count) {
            val type = if (Random.nextFloat() < 0.7) FolderType.BOOKMARK else FolderType.BLOCK // More bookmark folders
            var parentId: Long? = null
            var level = 0

            // ~30% chance of being nested, if suitable parents exist
            if (Random.nextFloat() < 0.3) {
                val potentialParents = generatedFolderIds.filter { it.value == type }.keys.toList()
                if (potentialParents.isNotEmpty()) {
                    parentId = potentialParents.random()
                    // Simple level tracking (doesn't prevent deep nesting but helps)
                    // A real implementation might query depth if needed.
                    level = 1 // Assume level 1 for now if parent is known
                }
            }

            var folderName = folderNames.random()
            val namesInScope = existingNames.getOrPut(parentId to type) { mutableSetOf() }

            var suffix = 1
            var uniqueName = folderName
            while (namesInScope.contains(uniqueName.lowercase())) {
                uniqueName = "$folderName ${suffix++}"
            }
            namesInScope.add(uniqueName.lowercase())


            val createdAt = now.minus(Random.nextInt(1, 200).days).minus(Random.nextInt(1, 24).hours)
            val updatedAt = createdAt.plus(Random.nextInt(0, 24).hours).plus(Random.nextInt(1, 60).minutes)

            generatedFolders.add(
                FolderEntity(
                    // ID auto-generated
                    name = uniqueName,
                    parentFolderId = parentId,
                    folderType = type,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            )
        }
        val insertedIds = mockBrowserPickerDatabaseDao.insertFoldersIgnoreConflict(generatedFolders) // Need custom DAO method
        insertedIds.forEachIndexed { index, id ->
            if(id > 0) { // Ignore conflicts (-1)
                val folder = generatedFolders[index]
                generatedFolderIds[id] = folder.folderType
            }
        }
    }


    private suspend fun generateHostRules(count: Int) {
        val hostsToUse = commonHosts.shuffled().take(count).toMutableSet()
        // Add some random-ish hosts
        repeat(count / 4) {
            hostsToUse.add("${randomWords.random()}${randomWords.random()}${commonTlds.random()}")
        }

        val rulesToInsert = mutableListOf<HostRuleEntity>()
        val now = instantProvider.now()

        for (host in hostsToUse) {
            if (generatedHostRuleIds.containsKey(host)) continue // Should not happen with set

            var status = UriStatus.NONE
            var folderId: Long? = null
            var preferredBrowser: String? = null
            var isEnabled = true

            val random = Random.nextFloat()
            when {
                // ~15% Blocked
                random < 0.15 -> {
                    status = UriStatus.BLOCKED
                    preferredBrowser = null // Enforced
                    isEnabled = false       // Enforced
                    val potentialFolders = generatedFolderIds.filter { it.value == FolderType.BLOCK }.keys.toList()
                    // ~50% chance of assigning to a specific block folder
                    if (potentialFolders.isNotEmpty() && Random.nextFloat() < 0.5) {
                        folderId = potentialFolders.random()
                    } else {
                        folderId = null // Enforced if no block folders exist or by chance
                    }
                }
                // ~35% Bookmarked
                random < 0.50 -> {
                    status = UriStatus.BOOKMARKED
                    val potentialFolders = generatedFolderIds.filter { it.value == FolderType.BOOKMARK }.keys.toList()
                    // ~60% chance of assigning to a specific bookmark folder
                    if (potentialFolders.isNotEmpty() && Random.nextFloat() < 0.6) {
                        folderId = potentialFolders.random()
                    } else {
                        folderId = null
                    }
                    // ~40% chance of having a preferred browser for bookmarks
                    if (Random.nextFloat() < 0.4) {
                        preferredBrowser = browsers.random()
                        // ~10% chance preference is disabled
                        if (Random.nextFloat() < 0.1) {
                            isEnabled = false
                        }
                    } else {
                        preferredBrowser = null
                        isEnabled = true // Or false? Let's default to true if no browser set
                    }
                }
                // ~50% None (Default)
                else -> {
                    status = UriStatus.NONE
                    folderId = null // Enforced
                    // ~50% chance of having a preferred browser for non-bookmarked/blocked
                    if (Random.nextFloat() < 0.5) {
                        preferredBrowser = browsers.random()
                        // ~15% chance preference is disabled
                        if (Random.nextFloat() < 0.15) {
                            isEnabled = false
                        }
                    } else {
                        preferredBrowser = null
                        isEnabled = true
                    }
                }
            }


            val createdAt = now.minus(Random.nextInt(1, 180).days).minus(Random.nextInt(1, 24).hours)
            val updatedAt = createdAt.plus(Random.nextInt(0, 24).hours).plus(Random.nextInt(1, 60).minutes)

            val rule = HostRuleEntity(
                host = host,
                uriStatus = status,
                folderId = folderId,
                preferredBrowserPackage = preferredBrowser,
                isPreferenceEnabled = isEnabled,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
            rulesToInsert.add(rule)
        }

        // Use upsert to handle potential (though unlikely here) host conflicts
        val insertedIds = mockBrowserPickerDatabaseDao.upsertHostRulesReturnIds(rulesToInsert) // Need custom DAO method
        insertedIds.forEachIndexed { index, id ->
            if (id > 0) {
                val rule = rulesToInsert[index]
                generatedHostRuleIds[rule.host] = id
                hostRuleDetails[id] = rule
            }
        }
    }

    private suspend fun generateUriRecords(count: Int): List<UriRecordEntity> {
        val records = mutableListOf<UriRecordEntity>()
        val startTime = instantProvider.now().minus(count.days / 10) // Start history ~ N/10 days ago
        var currentTime = startTime

        val hostPool = commonHosts.take(20) + generatedHostRuleIds.keys.take(50) // Mix known, ruled, and random hosts

        for (i in 0 until count) {
            val host = hostPool.random() // Pick a host
            val scheme = schemes.random()
            val path = paths.random()
            val query = if (Random.nextBoolean()) "?${queryParams.random()}${randomWords.random()}" else ""
            val uriString = "$scheme://$host$path$query"

            val source = UriSource.entries.random()

            // Determine interaction based on potential rule
            val ruleId = generatedHostRuleIds[host]
            val rule = ruleId?.let { hostRuleDetails[it] }

            var action: InteractionAction
            var chosenBrowser: String? = null

            if (rule != null) {
                when {
                    // Rule is Blocked
                    rule.uriStatus == UriStatus.BLOCKED -> {
                        action = InteractionAction.BLOCKED_URI_ENFORCED
                        chosenBrowser = null
                    }
                    // Rule has Enabled Preference
                    rule.isPreferenceEnabled && !rule.preferredBrowserPackage.isNullOrBlank() -> {
                        // High chance it was opened by preference
                        action = if (Random.nextFloat() < 0.9) InteractionAction.OPENED_BY_PREFERENCE else InteractionAction.OPENED_ONCE // User might override sometimes?
                        chosenBrowser = rule.preferredBrowserPackage
                    }
                    // Rule exists but no preference / disabled / not blocked
                    else -> {
                        action = if (Random.nextFloat() < 0.8) InteractionAction.OPENED_ONCE else InteractionAction.DISMISSED
                        chosenBrowser = if (action == InteractionAction.OPENED_ONCE) browsers.random() else null
                    }
                }
            } else {
                // No rule for this host
                action = if (Random.nextFloat() < 0.85) InteractionAction.OPENED_ONCE else InteractionAction.DISMISSED
                chosenBrowser = if (action == InteractionAction.OPENED_ONCE) browsers.random() else null
            }

            // Advance time slightly, with some randomness
            currentTime = currentTime.plus(Random.nextInt(1, 300).seconds).plus(Random.nextInt(0,1000).toLong(), DateTimeUnit.MILLISECOND)
            if (currentTime > instantProvider.now()) currentTime = instantProvider.now() // Don't go into future


            records.add(
                UriRecordEntity(
                    uriString = uriString,
                    host = host,
                    timestamp = currentTime,
                    uriSource = source,
                    interactionAction = action,
                    chosenBrowserPackage = chosenBrowser,
                    associatedHostRuleId = ruleId
                )
            )

            // Batch insert periodically
            if (records.size % 500 == 0) {
                mockBrowserPickerDatabaseDao.insertUriRecords(records.toList()) // Insert current batch
                records.clear()
                Timber.d("Inserted batch of 500 URI records...")
            }
        }
        // Insert any remaining records
        if (records.isNotEmpty()) {
            mockBrowserPickerDatabaseDao.insertUriRecords(records)
        }

        // Return all generated records for stat calculation (fetch might be needed if batching)
        // For simplicity, assume we have them all, otherwise query after insert.
        // Let's query them back to be safe if batching was used.
        return mockBrowserPickerDatabaseDao.getAllUriRecordsDebug() // Need a simple SELECT * DAO method
    }


    private suspend fun generateBrowserStats(uriRecords: List<UriRecordEntity>) {
        Timber.d("Calculating browser stats from ${uriRecords.size} records...")
        val stats = uriRecords
            .filter { !it.chosenBrowserPackage.isNullOrBlank() }
            .groupBy { it.chosenBrowserPackage!! }
            .map { (packageName, records) ->
                BrowserUsageStatEntity(
                    browserPackageName = packageName,
                    usageCount = records.size.toLong(),
                    lastUsedTimestamp = records.maxOf { it.timestamp }
                )
            }

        if(stats.isNotEmpty()) {
            mockBrowserPickerDatabaseDao.upsertBrowserUsageStats(stats) // Need DAO method for List upsert
            Timber.d("Upserted ${stats.size} browser stat entries.")
        } else {
            Timber.d("No browser usage stats to generate.")
        }
    }
}

