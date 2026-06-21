plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)

}

android {
    namespace = "com.example.test0512.mobile"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.test0512"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
    }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.play.services.wearable)
}
