plugins {
    id("com.android.application")
}

android {
    namespace = "com.pharaoh.tvplay"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pharaoh.tvplay"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        //testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation("org.mozilla.geckoview:geckoview:120.0.20231208211905")
    // 133.0.20241204213141
    // 120.0.20231208211905
    //implementation("androidx.webkit:webkit:1.8.0") // 使用最新稳定版本
    // 135.0.20250216192613
//    implementation("org.mozilla.geckoview:geckoview:135.0.20250216192613")
// implementation("com.google.android.material:material:1.10.0")
//    implementation("androidx.appcompat:appcompat:1.6.1")
//    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//    implementation("androidx.navigation:navigation-fragment:2.6.0")
//    implementation("androidx.navigation:navigation-ui:2.6.0")
    //    implementation("androidx.appcompat:appcompat:1.6.1")
//    implementation("com.google.android.material:material:1.10.0")
//    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("androidx.test.ext:junit:1.1.5")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}