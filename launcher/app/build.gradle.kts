plugins {
    alias(libs.plugins.android.application)
}

val releaseKeystore: String? = System.getenv("QUIETV_KEYSTORE")

android {
    namespace = "io.github.nowaaak.quietv"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.nowaaak.quietv"
        minSdk = 26
        targetSdk = 34
        versionCode = 9
        versionName = "0.6.0"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("QUIETV_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("QUIETV_KEY_ALIAS")
                keyPassword = System.getenv("QUIETV_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        disable += setOf("ExpiredTargetSdkVersion", "OldTargetApi", "GradleDependency", "AndroidGradlePluginVersion")
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.dynamicanimation)
    testImplementation(libs.junit)
    testImplementation(libs.json)
}
