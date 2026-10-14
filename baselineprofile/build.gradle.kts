// Records the startup profile of the app on a device. See docs/decisions.md, "Startup profile".
plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "io.github.stivengjekaj.zettastream.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // A profile can be recorded without root from Android 13.
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
}

androidComponents {
    beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" }
}
