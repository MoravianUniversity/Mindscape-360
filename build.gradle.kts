plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    id("java")  //   // **ADDED**  -- but should not be needed because "android" is applied and supposed to be allowed
    id("com.google.protobuf") version "0.9.5"  //   // **ADDED**
}