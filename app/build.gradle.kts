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
val naverMapClientId: String = localProperties.getProperty("NAVER_MAP_CLIENT_ID", "YOUR_NCP_KEY_ID_HERE")
var apiBaseUrl: String = localProperties.getProperty("API_BASE_URL", "https://example.com/")
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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] = naverMapClientId
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

    implementation("com.naver.maps:map-sdk:3.24.0")
}