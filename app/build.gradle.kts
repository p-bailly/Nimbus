plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Clé de signature des releases, fournie par la CI (secrets GitHub, voir .github/workflows/release.yml).
// Sans elle (compilation locale), la release est signée avec la clé de debug.
val releaseKeystore: String? = System.getenv("NIMBUS_KEYSTORE_FILE")

android {
    namespace = "fr.perso.nimbus"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.perso.nimbus"
        minSdk = 28
        targetSdk = 35
        // Numéros donnés par la CI à chaque release ; une mise à jour doit avoir un numéro plus grand.
        versionCode = System.getenv("NIMBUS_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("NIMBUS_VERSION_NAME") ?: "1.0"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("NIMBUS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NIMBUS_KEY_ALIAS") ?: "nimbus"
                keyPassword = System.getenv("NIMBUS_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Tests JVM avec Robolectric : vraie base SQLite et vrai parseur XML d'Android.
        unitTests.isIncludeAndroidResources = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
