plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.wendy.face"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wendy.face"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // TODO: Please replace with your own VOLC_API_KEY
            buildConfigField("String", "VOLC_API_KEY", "\"\"")
            buildConfigField("String", "VOLC_SECRET_KEY", "\"\"")
        }
        debug {
            buildConfigField("String", "VOLC_API_KEY", "\"20f0c849-c533-47d4-8e8d-7aef5801647e\"")
            buildConfigField("String", "VOLC_SECRET_KEY", "\"YOUR_SECRET_KEY\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // ML Kit
    implementation("com.google.mlkit:face-mesh-detection:16.0.0-beta1")
    implementation("com.google.mlkit:face-detection:16.1.5")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    // OpenAI Kotlin client
    implementation("com.aallam.openai:openai-client:4.0.1")
    implementation("io.ktor:ktor-client-okhttp:3.0.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}