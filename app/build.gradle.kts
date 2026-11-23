plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.nexuscorner.app"
    compileSdk = 34
    defaultConfig { applicationId = "com.nexuscorner.app"; minSdk = 26; targetSdk = 34; versionCode = 11; versionName = "1.8.2" }
    signingConfigs {
        create("nexus") {
            storeFile = file("nexus.keystore"); storeType = "pkcs12"
            storePassword = "nexuscorner"; keyAlias = "nexus"; keyPassword = "nexuscorner"
        }
    }
    buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("nexus") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
