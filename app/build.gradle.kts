plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.purplerat.emberbrowser"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.purplerat.emberbrowser"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Deliberate, and part of the product promise: NO third-party libraries at all.
// No analytics SDK, no ads SDK, no telemetry, no crash-reporting phone-home.
// The app is Kotlin stdlib (bundled by the Kotlin plugin) + the Android framework only.
dependencies {
}
