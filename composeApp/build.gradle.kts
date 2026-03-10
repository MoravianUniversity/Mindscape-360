plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "1.9.21"
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
        iosTarget.compilations["main"].cinterops { // **ADDED**
            val cardboard by creating {
                definitionFile = file("src/nativeInterop/cinterop/cardboard.def")
                packageName = "cardboard.native"
            }
            val nskvoprotocol by creating {
                definitionFile = file("src/nativeInterop/cinterop/nskvoprotocol.def")
                packageName = "platform.FoundationCompat"
            }
            val vcimmersionprotocol by creating {
                definitionFile = file("src/nativeInterop/cinterop/vcimmersionprotocol.def")
                packageName = "platform.UIKitCompat"
            }
        }
    }

    sourceSets {

        val ktorVersion = "3.3.3"

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            // ExoPlayer dependencies
            implementation("androidx.media3:media3-exoplayer:1.2.0")
            implementation("androidx.media3:media3-ui:1.2.0")
            implementation("androidx.media3:media3-common:1.2.0")
            implementation(project(":cardboard:sdk"))  // **ADDED**
            implementation("io.ktor:ktor-client-android:${ktorVersion}")  // **ADDED**
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation)
            // Add Okio for file operations
            implementation(compose.components.resources)  // **ADDED**
            implementation(compose.components.uiToolingPreview)
            implementation(libs.okio)  // **ADDED**


            // Ktor dependencies. // **ADDED**
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-cio:${ktorVersion}")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            implementation("io.ktor:ktor-client-encoding:$ktorVersion")
            implementation("io.ktor:ktor-client-logging:${ktorVersion}")

            // Kamel for image loading    // **ADDED**
            //implementation("media.kamel:kamel-image:0.9.1")

            // Serialization    // **ADDED**
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

            // KStore for key-value storage    // **ADDED**
            implementation("io.github.xxfast:kstore:0.8.0")
            implementation("io.github.xxfast:kstore-file:0.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.6.2")
        }

        // **ADDED**
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:$ktorVersion")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "edu.moravian.mindscape360"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "edu.moravian.mindscape360"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 2  // has to be increased every time you upload a new version to the Play Store
        versionName = "1.0"
        ndk {  // **ADDED**
            //noinspection ChromeOsAbiSupport
            abiFilters += "armeabi-v7a"
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
            ndkVersion = "29.0.13846066"
        }
    }

    sourceSets["main"].apply {
        res.srcDirs("src/androidMain/res")
        assets.srcDirs("src/commonMain/resources/assets")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }

    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    externalNativeBuild {  // **ADDED**
        cmake {
            path("src/androidMain/CMakeLists.txt")
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

