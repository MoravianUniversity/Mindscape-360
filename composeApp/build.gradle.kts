import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

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
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            // ExoPlayer dependencies
            implementation("androidx.media3:media3-exoplayer:1.2.0")
            implementation("androidx.media3:media3-ui:1.2.0")
            implementation("androidx.media3:media3-common:1.2.0")
            implementation(project(":cardboard:sdk"))  // **ADDED**
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
            implementation("com.squareup.okio:okio:3.6.0")
            implementation(compose.components.resources)  // **ADDED**
            implementation(compose.components.uiToolingPreview)
            implementation(libs.okio)  // **ADDED**
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "vr.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "vr.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        ndk {  // **ADDED**
            //noinspection ChromeOsAbiSupport
            abiFilters += "armeabi-v7a"
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
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

