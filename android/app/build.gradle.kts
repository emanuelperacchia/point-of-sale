import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pos.android"
    compileSdk = 35

    // ── Signing configuration ──
    val signingProperties = project.file("signing.properties")
    val signingProps = Properties()

    var releaseStoreFile = ""
    var releaseStorePassword = ""
    var releaseKeyAlias = ""
    var releaseKeyPassword = ""

    if (signingProperties.exists()) {
        signingProps.load(FileInputStream(signingProperties))
        releaseStoreFile = signingProps.getProperty("nivex.release.storeFile", "")
        releaseStorePassword = signingProps.getProperty("nivex.release.storePassword", "")
        releaseKeyAlias = signingProps.getProperty("nivex.release.keyAlias", "")
        releaseKeyPassword = signingProps.getProperty("nivex.release.keyPassword", "")
    }

    signingConfigs {
        create("release") {
            storeFile = if (releaseStoreFile.isNotEmpty()) file(releaseStoreFile) else file("debug.keystore")
            storePassword = if (releaseStorePassword.isNotEmpty()) releaseStorePassword else "android"
            keyAlias = if (releaseKeyAlias.isNotEmpty()) releaseKeyAlias else "androiddebugkey"
            keyPassword = if (releaseKeyPassword.isNotEmpty()) releaseKeyPassword else "android"
        }
        // NO crear "debug" - Android ya lo crea automáticamente
    }

    defaultConfig {
        applicationId = "com.pos.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/api/\"")
        buildConfigField("String", "PUBLIC_URL", "\"http://10.0.2.2:8080/public/v1/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
            buildConfigField("String", "BASE_URL", "\"https://api.nivex.com/api/\"")
            buildConfigField("String", "PUBLIC_URL", "\"https://api.nivex.com/public/v1/\"")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/api/\"")
            buildConfigField("String", "PUBLIC_URL", "\"http://10.0.2.2:8080/public/v1/\"")
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.activity)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.mlkit.barcode)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.coil.compose)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)
    implementation(libs.security.crypto)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}

kapt {
    correctErrorTypes = true
}