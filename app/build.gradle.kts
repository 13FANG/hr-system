plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.shah.hrsystem"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shah.hrsystem"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // ИЗМЕНЕНИЕ: Указываем Hilt Test Runner
        testInstrumentationRunner = "com.shah.hrsystem.CustomTestRunner" // Путь к нашему кастомному раннеру

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {}
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // --- Версии ---
    val roomVersion = "2.6.1"
    val navigationVersion = "2.7.7"
    // Версия Hilt должна совпадать с версией плагина в build.gradle.kts (Project:)
    val hiltVersion = "2.51.1" // Используйте вашу версию плагина
    val coroutinesVersion = "1.7.3"
    val lifecycleVersion = "2.7.0"
    val kotlinVersion = "1.9.23" // Используйте вашу версию плагина

    // Kotlin & Coroutines
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    // AndroidX Core & AppCompat & Activity & Fragment
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.7.0")

    // UI - Material Components & ConstraintLayout
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Lifecycle (ViewModel, LiveData/Flow)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:$navigationVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navigationVersion")

    // Room Persistence Library
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-compiler:$hiltVersion")

    // Security Crypto
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    // Password Hashing (BCrypt)
    implementation("at.favre.lib:bcrypt:0.10.2")

    // PDF Generation (iText 7 - Community)
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("org.slf4j:slf4j-android:1.7.36")

    // --- ТЕСТИРОВАНИЕ ---
    // Unit тесты (src/test)
    testImplementation("junit:junit:4.13.2")

    // Instrumentation (UI) тесты (src/androidTest)
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")

    // --- ДОБАВЛЕНО/ПРОВЕРЕНО: Hilt Testing ---
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion") // Зависимость для тестов Hilt
    kspAndroidTest("com.google.dagger:hilt-compiler:$hiltVersion") // KSP для Hilt в androidTest

    // Core Library Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}