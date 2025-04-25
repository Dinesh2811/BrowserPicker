plugins {
    alias(libs.plugins.library)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.serialization)
    alias(libs.plugins.compose)
}

android {
    namespace = "browserpicker.presentation"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdk"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation(project(mapOf("path" to ":browserpicker:domain")))
    implementation(project(mapOf("path" to ":theme")))

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