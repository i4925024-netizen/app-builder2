plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.future.htmlapkstudio"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.future.htmlapkstudio"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "3.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = false
            val ks = project.findProperty("studioKeystoreFile") as String?
            val kp = project.findProperty("studioKeystorePassword") as String?
            val ka = project.findProperty("studioKeyAlias") as String?
            val kpp = project.findProperty("studioKeyPassword") as String?
            if (ks != null && kp != null && ka != null && kpp != null) {
                signingConfig = signingConfigs.create("studioRelease") {
                    storeFile = file(ks)
                    storePassword = kp
                    keyAlias = ka
                    keyPassword = kpp
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.google.android.material:material:1.12.0")
}
