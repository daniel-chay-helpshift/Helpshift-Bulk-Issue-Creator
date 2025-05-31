apply(from = "installCreds.gradle") // Apply the credentials file

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.helpshiftbulkissuecreator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.helpshiftbulkissuecreator"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        buildConfigField("String", "HELPSHIFT_DOMAIN_NAME", "\"${project.extra["helpshiftDomainName"]}\"")
        buildConfigField("String", "HELPSHIFT_PLATFORM_ID", "\"${project.extra["helpshiftPlatformId"]}\"")
        buildConfigField("String", "HELPSHIFT_API_KEY", "\"${project.extra["helpshiftApiKey"]}\"")

    }
    buildFeatures.buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    implementation(libs.volley)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}