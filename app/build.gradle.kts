// app/build.gradle

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.ncda"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.ncda"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    packagingOptions {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            // Keep existing excludes, add more if needed for other META-INF files
            // excludes += "META-INF/NOTICE"
            // excludes += "META-INF/LICENSE"
            // excludes += "META-INF/LICENSE.txt"
            // excludes += "META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    // --- Core AndroidX UI and Lifecycle dependencies ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")

    // --- ML Kit Face Detection ---
    implementation("com.google.mlkit:face-detection:16.1.7")

    // --- CameraX dependencies ---
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // --- Firebase (Using BOM for version management) ---
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-common")

    // --- Room Database ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // --- Other Existing Dependencies ---
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // UPDATE: Glide to latest stable (4.16.0)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("androidx.cardview:cardview:1.0.0")

    // NEW: Add RecyclerView dependency
    implementation("androidx.recyclerview:recyclerview:1.3.2") // Or the very latest stable version

    // --- OkHttp and Gson for REST API communication ---
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // --- Google Authentication Library (needed to generate access token for REST API) ---
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.play.services.auth)

    // --- Testing Dependencies ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}