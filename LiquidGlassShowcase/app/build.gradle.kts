plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.liquidglass.showcase"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.liquidglass.showcase"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/liquidglass-demo.jks")
            storePassword = "liquidglass"
            keyAlias = "liquidglass"
            keyPassword = "liquidglass"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            vcsInfo.include = false
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += arrayOf(
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "kotlin/**",
                "META-INF/*.version",
                "META-INF/**/LICENSE.txt"
            )
        }
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.ripple)

    // Liquid Glass engine — the published Backdrop 2.0.0 artifact contains the
    // exact AGSL shaders / default parameters / render pipeline of Kyant0/AndroidLiquidGlass.
    implementation(libs.backdrop)

    // iOS G2-continuous corner shapes (transitive runtime dependency of backdrop 2.0.0,
    // used by every sample component of the upstream repository).
    implementation(libs.kyant.shapes)
}