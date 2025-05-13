plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.serialization)
}

android {
    namespace = "com.dinesh.playground"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        applicationId = "com.dinesh.playground"
        minSdk = rootProject.extra["minSdk"] as Int
        targetSdk = rootProject.extra["targetSdk"] as Int
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
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
        compose = true
    }
}

dependencies {
    implementation(libs.timeago)
    implementation(libs.kotlinx.datetime)
    implementation(libs.timber)

    implementation(libs.bundles.android)
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    //  ViewModel & LiveData
    implementation(libs.bundles.lifecycle)

    //  Room components
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    implementation(libs.bundles.paging)
    // Serialization
    implementation(libs.bundles.serialization)

    // HTTP
    implementation(libs.bundles.okhttp)

    androidTestImplementation(libs.bundles.android.test)
    debugImplementation(libs.bundles.debug)
    testImplementation(libs.bundles.test)

    //  Hilt
    implementation(libs.bundles.hilt)
    ksp(libs.bundles.hilt.compiler)

    // WorkManager
    implementation(libs.work.manager)

    // DataStore
    implementation(libs.bundles.datastore.preferences)
    implementation(libs.splashscreen)

    debugImplementation(libs.bundles.debugImplementation)
    releaseImplementation(libs.bundles.releaseImplementation)
    androidTestImplementation(libs.bundles.androidTestImplementation)
    testImplementation(libs.bundles.testImplementation)


    implementation(libs.timeago)
    implementation(libs.kotlinx.datetime)
    implementation(libs.timber)

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

    debugImplementation(libs.bundles.debugImplementation)
    releaseImplementation(libs.bundles.releaseImplementation)
    androidTestImplementation(libs.bundles.androidTestImplementation)
    testImplementation(libs.bundles.testImplementation)
}