plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.stivengjekaj.zettastream"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.stivengjekaj.zettastream"
        minSdk = 26
        targetSdk = 37
        versionCode = 31
        versionName = "1.5.0"
        // Only the benchmark build starts with a source list. See the baselineprofile module.
        buildConfigField("String", "SEED_SOURCES", "\"\"")
    }

    // The release key comes from environment variables. GitHub Actions sets
    // them from secrets. A local release build without them is not signed.
    val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        // A release build for the baselineprofile module. It keeps the class names, so that
        // the profile it records matches the code. R8 maps the profile for the release build.
        create("benchmark") {
            initWith(getByName("release"))
            applicationIdSuffix = ".benchmark"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            proguardFiles("benchmark-rules.pro")
            buildConfigField("String", "SEED_SOURCES", "\"https://v3-cinemeta.strem.io/manifest.json\"")
        }
    }

    // Compress the native libraries, so that the download for Downloader stays small.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Installs the startup profile in src/main/baseline-prof.txt on devices that do not get it from a store.
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    // Colour filters of the player. They run only while the viewer chooses one.
    implementation(libs.media3.effect)
    implementation(libs.androidx.tvprovider)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.ui)
    implementation(libs.zxing.core)
    implementation(libs.libtorrent4j)
    // 32-bit ARM for TVs and 64-bit ARM for phones. Only the emulator needs x86_64.
    implementation(libs.libtorrent4j.arm)
    implementation(libs.libtorrent4j.arm64)
    debugImplementation(libs.libtorrent4j.x64)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kxml2)
}
