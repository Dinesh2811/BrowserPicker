plugins {
    alias(libs.plugins.application) apply false
    alias(libs.plugins.library) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.room) apply false
//    id("com.google.dagger.hilt.android") version "2.51.1" apply false
//    id("androidx.room") version "2.7.0" apply false
}

val compileSdk by extra(35)
val targetSdk by extra(35)
val minSdk by extra(28)
val javaVersion by extra(JavaVersion.VERSION_21)
val jvmToolchain by extra(21)
