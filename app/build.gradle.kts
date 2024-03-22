plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id ("kotlin-android")
    id ("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.dev.mywebrtc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dev.mywebrtc"
        minSdk = 24
        targetSdk = 34
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    viewBinding{
        enable = true
    }

    kapt {
        correctErrorTypes = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


    implementation(platform ("com.google.firebase:firebase-bom:32.7.4"))
    implementation ("com.google.firebase:firebase-database-ktx:20.3.1")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.mesibo.api:webrtc:1.0.5")
    implementation ("com.google.dagger:hilt-android:2.48")
    kapt ("com.google.dagger:hilt-compiler:2.48")
    implementation ("com.guolindev.permissionx:permissionx:1.6.1")
}