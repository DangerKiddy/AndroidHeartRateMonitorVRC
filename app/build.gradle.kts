plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.androidheartratemonitorvrc"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.androidheartratemonitorvrc"
        minSdk = 33
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 33
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
}

dependencies {
    implementation("androidx.work:work-runtime:2.7.0")
    implementation("com.illposed.osc:javaosc-core:0.8")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}