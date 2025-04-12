plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.litera"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.litera"
        minSdk = 24
        targetSdk = 35
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
            manifestPlaceholders["firebaseAnalyticsEnabled"] = "false"
        }
        debug {
            manifestPlaceholders["firebaseAnalyticsEnabled"] = "false"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.google.firebase:firebase-firestore:24.4.1")
    implementation ("com.google.firebase:firebase-auth:22.0.2")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}