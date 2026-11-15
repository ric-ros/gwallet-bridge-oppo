plugins {
    id("com.android.application")
}

// Release signing comes from ~/.gradle/gradle.properties (never from this repo).
// Without these properties only the debug build can be signed.
val storeFilePath = providers.gradleProperty("GWB_STORE_FILE").orNull

android {
    // Shared code; each flavor occupies a different firmware shortcut slot.
    namespace = "io.github.ricros.gwalletbridge"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
        targetSdk = 36
        versionCode = 7
        versionName = "1.1.0"
    }

    flavorDimensions += "shortcutSlot"
    productFlavors {
        create("fineasy") {
            dimension = "shortcutSlot"
            applicationId = "com.fintech.life"
            manifestPlaceholders["walletOpenAction"] = "fintech.life.intent.action.OPEN"
        }
        create("oppoWallet") {
            dimension = "shortcutSlot"
            applicationId = "com.heytap.wallet"
            manifestPlaceholders["walletOpenAction"] = "heytap.wallet.intent.action.OPEN"
        }
    }

    signingConfigs {
        if (storeFilePath != null) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = providers.gradleProperty("GWB_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("GWB_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("GWB_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (storeFilePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
