plugins {
    id("com.android.application")
}

// Release signing comes from ~/.gradle/gradle.properties (never from this repo).
// Without these properties only the debug build can be signed.
val storeFilePath = providers.gradleProperty("GWB_STORE_FILE").orNull

android {
    // Code namespace. The applicationId below must stay com.heytap.wallet:
    // it is the package ColorOS launches on a power-button double press.
    namespace = "io.github.ricros.gwalletbridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.heytap.wallet"
        minSdk = 31
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.0"

        manifestPlaceholders["walletOpenAction"] = "heytap.wallet.intent.action.OPEN"
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
