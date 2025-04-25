package browserpicker.domain

import android.graphics.Bitmap
import kotlin.random.Random


data class BrowserAppInfo(
    val appName: String,
    val packageName: String,
    val appIcon: Bitmap?,
    val isDefaultBrowser: Boolean,

//    val browserApp: BrowserApp = BrowserApp(packageName, appName, Clock.System.now().toEpochMilliseconds()),
//    val lastUsedTimestamp: Long? = null
) {
    companion object {
        fun generateRandomBrowserApps(count: Int = 30): List<BrowserAppInfo> {
            val appNames = listOf("Chrome", "1DM+", "Safari", "Soul Browser", "Edge", "Brave", "Samsung Internet Browser")
            val packageNames = listOf(
                "com.android.chrome",
                "idm.internet.download.manager.plus",
                "com.apple.safari",
                "com.mycompany.app.soulbrowser",
                "com.microsoft.emmx",
                "com.brave.browser",
                "com.sec.android.app.sbrowser",
            )

            return List(count) {
                val randomIndex = Random.Default.nextInt(appNames.size)
                BrowserAppInfo(
                    appName = appNames[randomIndex],
                    packageName = "${packageNames[randomIndex]}_$it",
//                    versionName = "${Random.Default.nextInt(1, 10)}.${Random.Default.nextInt(0, 10)}.${Random.Default.nextInt(0, 100)}",
//                    versionCode = Random.Default.nextLong(1, 1000),
                    isDefaultBrowser = Random.Default.nextBoolean(),
                    appIcon = null,
                )
            }
        }
    }
}
