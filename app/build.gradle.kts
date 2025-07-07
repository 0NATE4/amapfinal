plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.amap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.amap"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ----> ADD THIS BLOCK <----
        ndk {
            // Specifies the CPU architectures to support.
            // Amap recommends these two for modern devices.
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // This is the new line you need to add
    implementation("androidx.activity:activity-ktx:1.9.0") // Use the latest stable version
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // implementation(libs.androidx.activity) <-- The new dependency replaces this one, you can remove the old one.
    implementation(libs.androidx.constraintlayout)
    implementation("com.amap.api:3dmap-location-search:latest.integration") // Amap dependency
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}