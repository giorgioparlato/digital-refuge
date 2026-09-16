import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

/**
 * Throwaway diagnostic app: an accessibility service and nothing else. It is signed with the same
 * release key as digital refuge, so the only difference between the two apps is what they declare.
 */
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val releaseStore: String? = localProps.getProperty("RELEASE_STORE_FILE")

android {
    namespace = "app.refugeprobe"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.refugeprobe"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            if (releaseStore != null) {
                storeFile = file(releaseStore)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (releaseStore != null) "release" else "debug")
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
