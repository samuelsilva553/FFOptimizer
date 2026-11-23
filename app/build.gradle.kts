plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.nexuscorner.app"
    compileSdk = 34
    defaultConfig { applicationId = "com.nexuscorner.app"; minSdk = 26; targetSdk = 34; versionCode = 9; versionName = "1.8.0" }
    buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
