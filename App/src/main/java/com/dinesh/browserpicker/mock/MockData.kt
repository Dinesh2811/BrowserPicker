package com.dinesh.browserpicker.mock

import browserpicker.core.di.InstantProvider
import browserpicker.domain.model.*
import browserpicker.presentation.picker.BrowserAppInfo
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object MockData {

    // Injecting InstantProvider here would be ideal, but a singleton object is tricky.
    // We'll pass InstantProvider as a parameter to generator functions or use Clock.System directly.
    // Using Clock.System is acceptable for mock data.
    private val instantProvider: InstantProvider = object : InstantProvider {
        override fun now(): Instant = Clock.System.now()
    }

    private val random = Random(System.currentTimeMillis()) // Seed for randomness

    // --- Helper Data for Realistic Generation ---
    private val SCHEMES = listOf("http", "https")
    private val TOP_LEVEL_DOMAINS = listOf("com", "org", "net", "io", "dev", "co.uk", "gouv.fr")
    private val SUBDOMAINS = listOf("www", "blog", "shop", "app", "mail", "docs", "api", "secure", null) // null represents no subdomain
    private val PATH_SEGMENTS = listOf("about", "contact", "pricing", "features", "blog", "articles", "products", "categories", "users", "settings", "dashboard", "login", "signup", "help", "faq", "privacy")
    private val QUERY_PARAMS = listOf("utm_source", "ref", "id", "category", "search", "page")
    private val FRAGMENTS = listOf("section1", "details", "overview", "comments")

    private val REALISTIC_BROWSERS = listOf(
        BrowserAppInfo("Chrome", "com.android.chrome"),
        BrowserAppInfo("Firefox", "org.mozilla.firefox"),
        BrowserAppInfo("Brave Browser", "com.brave.browser"),
        BrowserAppInfo("Edge", "com.microsoft.emmx"),
        BrowserAppInfo("Opera", "com.opera.browser"),
        BrowserAppInfo("DuckDuckGo Privacy Browser", "com.duckduckgo.mobile.android"),
        BrowserAppInfo("Samsung Internet Browser", "com.sec.android.app.sbrowser"),
        BrowserAppInfo("Vivaldi Browser", "com.vivaldi.browser"),
        BrowserAppInfo("Kiwi Browser", "com.kiwibrowser.browser")
    )

    private val REALISTIC_URI_SOURCES = UriSource.entries
    private val REALISTIC_INTERACTION_ACTIONS = listOf(
        InteractionAction.OPENED_ONCE,
        InteractionAction.DISMISSED
        // BLOCKED_URI_ENFORCED and PREFERENCE_SET should be linked to rules,
        // so we generate them based on generated rules later.
    )

    // --- Generator Functions ---

    /** Generates a list of realistic-looking hosts. */
    private fun generateHosts(count: Int): List<String> {
        return (1..count).map {
            val subdomain = if (random.nextBoolean()) SUBDOMAINS.random() else null
            val domain = "example${random.nextInt(1, 50)}" // e.g., example1, example2...
            val tld = TOP_LEVEL_DOMAINS.random()
            if (subdomain != null) "$subdomain.$domain.$tld" else "$domain.$tld"
        }.distinct() // Ensure unique hosts
    }

    /** Generates a realistic URI string for a given host. */
    private fun generateUriString(host: String): String {
        val scheme = SCHEMES.random()
        val path = if (random.nextBoolean()) {
            val segments = random.nextInt(0, 4) // 0 to 3 segments
            (1..segments).joinToString("/", prefix = "/") { (PATH_SEGMENTS).random() }
        } else null

        val query = if (random.nextDouble() < 0.3) { // 30% chance of query
            val numParams = random.nextInt(1, 3) // 1 or 2 params
            (1..numParams).joinToString("&", prefix = "?") {
                "${QUERY_PARAMS.random()}=${random.nextInt(100, 999)}"
            }
        } else null

        val fragment = if (random.nextDouble() < 0.1) { // 10% chance of fragment
            "#${FRAGMENTS.random()}"
        } else null

        return "$scheme://$host${path.orEmpty()}${query.orEmpty()}${fragment.orEmpty()}"
    }

    /** Generates a list of realistic browser usage stats. */
    fun generateBrowserStats(count: Int): List<BrowserUsageStat> {
        val availableBrowsers = REALISTIC_BROWSERS.shuffled(random).take(count)
        val now = instantProvider.now()

        return availableBrowsers.mapIndexed { index, browserInfo ->
            val usageCount = random.nextLong(5, 5000) + (index * 100) // More popular browsers get more usage
            val lastUsedDelta = random.nextLong(0, 365) // Last used within the last year
            val lastUsedTimestamp = now.minus(lastUsedDelta.days).minus(random.nextLong(0, 24).hours).minus(random.nextLong(0, 60).minutes).minus(random.nextLong(0, 60).seconds)

            BrowserUsageStat(
                browserPackageName = browserInfo.packageName,
                usageCount = usageCount,
                lastUsedTimestamp = lastUsedTimestamp
            )
        }
    }

    /** Generates a hierarchy of folders. */
    fun generateFolders(count: Int, type: FolderType): List<Folder> {
        val folders = mutableListOf<Folder>()
        val now = instantProvider.now()

        // Add default root folder first (handled by ensureDefaultFoldersExist, but needed for parent IDs)
        // We rely on the Use Case/Repo to ensure these specific IDs exist and aren't duplicated.
        // Mock data generation assumes the repository will handle the upsert/existence check.
        // For simplicity in generation, we don't strictly need to create the defaults here
        // if the population use case starts with ensureDefaultFoldersExist.
        // Let's just generate *additional* folders.

        // Generate flat list first
        val generatedFolders = (1..count).map { index ->
            Folder(
                id = 0, // ID will be assigned by DB
                parentFolderId = null, // Will assign parents later
                name = "${type.name.capitalize()} Folder ${index + 1}",
                type = type,
                createdAt = now.minus(random.nextLong(0, 365).days),
                updatedAt = now.minus(random.nextLong(0, 365).days).plus(random.nextLong(0, 30).minutes) // Ensure updated > created
            )
        }.toMutableList()

        // Build a simple hierarchy: random folders become children of random others
        val folderIds = (Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID..Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID + count.toLong() + 100).toList() // Potential pool of IDs including defaults and generated
        // This is tricky - we don't know the IDs until after insertion.
        // Let's rethink: generate a flat list of *names* and *parent names*, then build structure.

        val baseNames = (1..count).map { "${type.name.capitalize()} SubFolder $it" }
        val currentFolders = mutableListOf<Folder>()
        val nameToFolderMap = mutableMapOf<String, Folder>()

        // Add default root folders conceptually (repo handles actual creation)
        val rootBookmark = Folder(Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID, null, Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_NAME, FolderType.BOOKMARK, now, now)
        val rootBlocked = Folder(Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID, null, Folder.DEFAULT_BLOCKED_ROOT_FOLDER_NAME, FolderType.BLOCK, now, now)

        if (type == FolderType.BOOKMARK) {
            currentFolders.add(rootBookmark)
            nameToFolderMap[rootBookmark.name] = rootBookmark
        } else {
            currentFolders.add(rootBlocked)
            nameToFolderMap[rootBlocked.name] = rootBlocked
        }

        // Create additional folders, linking randomly to existing ones
        for (i in 0 until count) {
            val parentFolder = currentFolders.random(random) // Pick a random existing folder as parent
            val newFolderName = "${baseNames.getOrElse(i) { "${type.name} Extra ${i+1}" }}" // Use generated name or fallback
            val newFolder = Folder(
                id = 0, // ID will be assigned by DB
                parentFolderId = parentFolder.id.takeIf { it > 0 }, // Use ID if not a default root (defaults have ID 1, 2) or if parent is not a default.
                name = newFolderName,
                type = type,
                createdAt = now.minus(random.nextLong(0, 300).days),
                updatedAt = now.minus(random.nextLong(0, 300).days).plus(random.nextLong(0, 20).minutes)
            )
            currentFolders.add(newFolder)
            nameToFolderMap[newFolderName] = newFolder
        }

        // Filter to include only the folders of the requested type, excluding the default root if it wasn't the requested type
        return currentFolders.filter { it.type == type }
    }

    /** Generates a list of host rules, linking to folders and browsers. */
    fun generateHostRules(count: Int, allFolders: List<Folder>, allBrowsers: List<BrowserAppInfo>, allHosts: List<String>): List<HostRule> {
        val now = instantProvider.now()

        val bookmarkFolderIds = allFolders.filter { it.type == FolderType.BOOKMARK }.map { it.id }.filter { it > 0 } // Exclude ID 0
        val blockedFolderIds = allFolders.filter { it.type == FolderType.BLOCK }.map { it.id }.filter { it > 0 }
        val browserPackages = allBrowsers.map { it.packageName }

        // Ensure we have enough hosts
        val hostsToUse = if (allHosts.size < count) {
            (allHosts + generateHosts(count - allHosts.size))
        } else {
            allHosts.shuffled(random).take(count)
        }

        return hostsToUse.map { host ->
            val status = (UriStatus.entries.filter { it != UriStatus.UNKNOWN }).random() // Pick a valid status
            val folderId = when (status) {
                UriStatus.BOOKMARKED -> bookmarkFolderIds.randomOrNull(random) ?: Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID.takeIf { bookmarkFolderIds.isNotEmpty() } // Default if no other bookmark folders
                UriStatus.BLOCKED -> blockedFolderIds.randomOrNull(random) ?: Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID.takeIf { blockedFolderIds.isNotEmpty() } // Default if no other blocked folders
                UriStatus.NONE -> null // No folder for NONE
                UriStatus.UNKNOWN -> null
            }

            val preferredBrowserPackage = if (status != UriStatus.BLOCKED && browserPackages.isNotEmpty() && random.nextDouble() < 0.6) { // 60% chance of preference if not blocked
                browserPackages.random(random)
            } else null

            val isPreferenceEnabled = preferredBrowserPackage != null && random.nextBoolean() // If preference set, 50% chance of being enabled

            val createdAt = now.minus(random.nextLong(0, 365).days)
            val updatedAt = createdAt.plus(random.nextLong(0L, ((now - createdAt).inWholeDays.toInt() + 1).toLong()).days) // Updated after creation

            HostRule(
                id = 0, // Assigned by DB
                host = host,
                uriStatus = status,
                folderId = folderId,
                preferredBrowserPackage = preferredBrowserPackage,
                isPreferenceEnabled = isPreferenceEnabled,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }

    /** Generates a list of URI records, linking to rules and browsers. */
    fun generateUriRecords(count: Int, allHostRules: List<HostRule>, allBrowsers: List<BrowserAppInfo>): List<UriRecord> {
        val now = instantProvider.now()
        val browserPackages = allBrowsers.map { it.packageName }
        val hostRuleMap = allHostRules.associateBy { it.host.lowercase() } // Case-insensitive host lookup

        return (1..count).map {
            val timestamp = now.minus(random.nextLong(0, 365).days)
                .minus(random.nextLong(0, 24).hours)
                .minus(random.nextLong(0, 60).minutes)
                .minus(random.nextLong(0, 60).seconds)

            val source = REALISTIC_URI_SOURCES.random()

            // Strategy: Sometimes pick a host that has a rule, sometimes pick a new host
            val host = if (random.nextDouble() < 0.4 && allHostRules.isNotEmpty()) { // 40% chance to pick a rule host
                allHostRules.random(random).host
            } else {
                generateHosts(1).first() // Generate a new host
            }

            val uriString = generateUriString(host)
            val associatedRule = hostRuleMap[host.lowercase()]

            val interactionAction = when {
                associatedRule?.uriStatus == UriStatus.BLOCKED && random.nextBoolean() -> InteractionAction.BLOCKED_URI_ENFORCED // Simulate direct block enforcement
                associatedRule?.isPreferenceEnabled == true && associatedRule.preferredBrowserPackage != null && random.nextBoolean() -> InteractionAction.OPENED_BY_PREFERENCE // Simulate opening by pref
                else -> REALISTIC_INTERACTION_ACTIONS.random() // Simulate picker interaction
            }

            val chosenBrowserPackage = if (interactionAction == InteractionAction.OPENED_ONCE || interactionAction == InteractionAction.OPENED_BY_PREFERENCE) {
                browserPackages.randomOrNull(random)
            } else null // No browser chosen for DISMISSED or BLOCKED

            val associatedHostRuleId = if (interactionAction == InteractionAction.BLOCKED_URI_ENFORCED || interactionAction == InteractionAction.PREFERENCE_SET) {
                associatedRule?.id
            } else null


            UriRecord(
                id = 0, // Assigned by DB
                uriString = uriString,
                host = host,
                timestamp = timestamp,
                uriSource = source,
                interactionAction = interactionAction,
                chosenBrowserPackage = chosenBrowserPackage,
                associatedHostRuleId = associatedHostRuleId
            )
        }
    }

    /**
     * Generates a complete set of mock data for the database.
     * Prioritizes UriRecords count, scales others proportionally.
     */
    fun generateAllData(uriRecordCount: Int): MockDatabaseData {
        // Scale counts for other entities
        val folderCount = (uriRecordCount * 0.05).toInt().coerceAtLeast(5) // 5% of records count, min 5
        val browserCount = REALISTIC_BROWSERS.size // Use all realistic browsers
        val hostRuleCount = (uriRecordCount * 0.1).toInt().coerceAtLeast(10) // 10% of records count, min 10
        val hostsToGenerate = (uriRecordCount * 0.2).toInt().coerceAtLeast(20) // Generate 20% unique hosts, min 20


        val browsers = generateBrowserStats(browserCount)
        val bookmarkFolders = generateFolders(folderCount / 2, FolderType.BOOKMARK) // Half bookmark
        val blockedFolders = generateFolders(folderCount - folderCount / 2, FolderType.BLOCK) // Half blocked
        val allFolders = bookmarkFolders + blockedFolders // Combine generated folders

        // Generate hosts *before* rules and records
        val initialHosts = generateHosts(hostsToGenerate)

        // Generate rules using a subset of hosts, available folders, and browsers
        val rules = generateHostRules(hostRuleCount, allFolders, browsers.map { BrowserAppInfo(it.browserPackageName, it.browserPackageName) }, initialHosts)

        // Generate records using the rule hosts, other generated hosts, and browsers
        val records = generateUriRecords(uriRecordCount, rules, browsers.map { BrowserAppInfo(it.browserPackageName, it.browserPackageName) })

        return MockDatabaseData(
            folders = allFolders,
            browserStats = browsers,
            hostRules = rules,
            uriRecords = records
        )
    }

    // Helper to get random element safely
    private fun <T> List<T>.randomOrNull(random: Random): T? = if (isEmpty()) null else random()
    private fun <T> List<T>.random(random: Random): T = random(random) // Use provided random
    private fun <T> List<T>.nextElement(random: Random): T = random(random) // Alias for clarity

    private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

}
