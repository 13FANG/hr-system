// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // alias(libs.plugins.android.application) apply false // Use id("...") instead
    // alias(libs.plugins.kotlin.android) apply false    // Use id("...") instead
    // alias(libs.plugins.google.devtools.ksp) apply false // Use id("...") instead
    // alias(libs.plugins.hilt.android.gradle.plugin) apply false // Use id("...") instead

    id("com.android.application") version "8.9.1" apply false // Замените на вашу версию AGP, если отличается
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false // Замените на вашу версию Kotlin, если отличается
    id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false // Версия KSP должна соответствовать Kotlin
    id("com.google.dagger.hilt.android") version "2.51.1" apply false // Замените на актуальную версию Hilt
    id("androidx.navigation.safeargs.kotlin") version "2.4.1" apply false
}