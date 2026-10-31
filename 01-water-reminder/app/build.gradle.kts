plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.waterbud.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.waterbud.app"
        minSdk = 26        // Android 8.0+. Required by Health Connect; covers ~98% of devices.
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // Room (local database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager (reminders)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // AdMob (monetization) — uses test IDs until you add your own
    implementation("com.google.android.gms:play-services-ads:23.3.0")
    // Google User Messaging Platform — GDPR/consent before serving ads (EEA)
    implementation("com.google.android.ump:user-messaging-platform:3.0.0")

    // Google Play Billing (premium)
    implementation("com.android.billingclient:billing-ktx:7.0.0")

    // Health Connect (write hydration to the phone's health hub).
    // If Gradle can't resolve this exact version, bump it to the latest in Android Studio.
    implementation("androidx.health.connect:connect-client:1.1.0-rc03")

    // Wear OS Data Layer (phone <-> watch sync)
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
