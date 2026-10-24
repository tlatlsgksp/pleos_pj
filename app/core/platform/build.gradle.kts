// Gateway implementations: Pleos SDK, AAOS Car API, Android fallbacks, and the Fake/demo sources.
// This is the only module allowed to import ai.pleos.playground.* or android.car.*.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.pleos.platform"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // AAOS car library (CarPropertyManager, CarUxRestrictionsManager).
    useLibrary("android.car")
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
