import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Put secrets / machine-specific values in local.properties (do not commit them).
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val kakaoNativeAppKey: String = localProperties.getProperty("KAKAO_NATIVE_APP_KEY", "YOUR_KAKAO_NATIVE_APP_KEY_HERE")
var apiBaseUrl: String = localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:8080/")
if (!apiBaseUrl.endsWith("/")) {
    apiBaseUrl += "/"
}

android {
    namespace = "com.example.footprint"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.footprint"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
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
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    // Retrofit2 & Gson (서버 API 통신)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Location Services (GPS)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Coroutines (비동기 처리)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("com.kakao.maps.open:android:2.15.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
}