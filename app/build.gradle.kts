plugins {
    id("com.android.application")
}

android {
    namespace = "com.beachgrooves.tv"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.beachgrooves.tv"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Sideload build: sign with the debug key so it installs without a keystore.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")
}
