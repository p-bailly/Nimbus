plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    // Captures d'écran JVM (Robolectric) : ./gradlew recordRoborazziDebug
    id("io.github.takahirom.roborazzi")
}

// Clé de signature des releases, fournie par la CI (secrets GitHub, voir .github/workflows/android.yml).
// Sans elle (compilation locale), la release est signée avec la clé de debug.
val releaseKeystore: String? = System.getenv("NIMBUS_KEYSTORE_FILE")

// Numéro de version = nombre de commits : le même en local et sur GitHub, et toujours croissant
// (une mise à jour doit avoir un numéro plus grand que la version installée).
val commitCount: Int = runCatching {
    providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().toInt()
}.getOrDefault(1)

android {
    namespace = "fr.perso.nimbus"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.perso.nimbus"
        minSdk = 28
        targetSdk = 35
        versionCode = commitCount
        versionName = "1.0.$commitCount"
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

    // Captures d'écran des écrans Compose sur la JVM (Roborazzi sur Robolectric).
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.43.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.43.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.43.1")
}
