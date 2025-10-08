plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.opencvfilterapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.opencvfilterapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Pass compiler flags to CMake (C++17)
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }

        // ✅ Include all major ABI architectures
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
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

    // ✅ Connect to native C++ build system
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // ✅ JNI native libraries directory
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // --- Android Core ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // --- Imaging & Metadata ---
    implementation("androidx.exifinterface:exifinterface:1.3.7") // EXIF metadata (filter info)
    implementation("com.github.bumptech.glide:glide:4.16.0")     // Efficient image loading
    implementation("com.github.chrisbanes:PhotoView:2.3.0")       // Zoomable image viewer

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}