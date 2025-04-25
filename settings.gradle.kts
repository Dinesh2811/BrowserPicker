pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BrowserPicker"
include(":theme")
include(":theme:coreLib")
include(":browserpicker:data")
include(":browserpicker:domain")
include(":browserpicker:presentation")
include(":browserpicker:core")
include(":browserpicker:playground")
include(":Playground")
include(":App")
