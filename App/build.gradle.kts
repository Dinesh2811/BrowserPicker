plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.serialization)
    alias(libs.plugins.compose)

//    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20-Beta2"
}

android {
    namespace = "com.dinesh.browserpicker"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        applicationId = "com.dinesh.browserpicker"
        minSdk = rootProject.extra["minSdk"] as Int
        targetSdk = rootProject.extra["targetSdk"] as Int
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = rootProject.extra["javaVersion"] as JavaVersion
        targetCompatibility = rootProject.extra["javaVersion"] as JavaVersion
    }
    kotlinOptions {
        jvmTarget = (rootProject.extra["javaVersion"] as JavaVersion).toString()
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        //  https://developer.android.com/studio/write/lint#snapshot
        baseline = file("lint-baseline.xml")

        disable += "TypographyFractions" + "TypographyQuotes"
        enable += "RtlHardcoded" + "RtlCompat" + "RtlEnabled"
        checkOnly += "NewApi" + "InlinedApi"
        // If set to true, turns off analysis progress reporting by lint.
        quiet = true
        // If set to true (default), stops the build if errors are found.
        abortOnError = false
        // If set to true, lint only reports errors.
        ignoreWarnings = true
        // If set to true, lint also checks all dependencies as part of its analysis.
        // Recommended for projects consisting of an app with library dependencies.
        checkDependencies = true
    }

}

dependencies {
    implementation(project(mapOf("path" to ":theme")))
//    implementation(project(mapOf("path" to ":openWith")))
//    implementation(project(mapOf("path" to ":coreLib")))
//    implementation(project(mapOf("path" to ":m3theme")))

    implementation(libs.timeago)
    implementation(libs.kotlinx.datetime)

    implementation(libs.bundles.android)
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    //  ViewModel & LiveData
    implementation(libs.bundles.lifecycle)

    //  Room components
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // Serialization
    implementation(libs.bundles.serialization)

    // HTTP
    implementation(libs.bundles.okhttp)

//    // Chucker
//    debugImplementation(libs.chucker.debug)
//    releaseImplementation(libs.chucker.release)

    androidTestImplementation(libs.bundles.android.test)
    debugImplementation(libs.bundles.debug)
    testImplementation(libs.bundles.test)

    // Navigation Component
    implementation(libs.bundles.navigation)

    // Paging
    implementation(libs.bundles.paging)

    //  Hilt
    implementation(libs.bundles.hilt)
    ksp(libs.bundles.hilt.compiler)

    // DataStore
    implementation(libs.bundles.datastore.preferences)

    implementation(libs.splashscreen)
//    implementation(libs.leakcanary)
//
//    // Pluto
//    debugImplementation(libs.bundles.debug.pluto)
//    releaseImplementation(libs.bundles.release.pluto)


    debugImplementation(libs.bundles.debugImplementation)
    releaseImplementation(libs.bundles.releaseImplementation)
    androidTestImplementation(libs.bundles.androidTestImplementation)
    testImplementation(libs.bundles.testImplementation)
}
