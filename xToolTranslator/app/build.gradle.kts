plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xtool.translator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xtool.translator"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        ndk {
            // 32-ביט בלבד — עובד על כל מעבד ARM (גם 64-ביט), וקובץ קטן
            abiFilters += listOf("armeabi-v7a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:translate:17.0.3")
}
