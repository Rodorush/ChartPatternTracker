import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "br.com.rodorush.chartpatterntracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.rodorush.chartpatterntracker"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        val webClientId: String? = properties.getProperty("WEB_CLIENT_ID")
        if (webClientId.isNullOrEmpty()) {
            throw GradleException("ERRO: WEB_CLIENT_ID não encontrado no local.properties!")
        }
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
        val brapiToken: String? = properties.getProperty("BRAPI_TOKEN")
        if (brapiToken.isNullOrEmpty()) {
            throw GradleException("ERRO: BRAPI_TOKEN não encontrado no local.properties!")
        }
        buildConfigField("String", "BRAPI_TOKEN", "\"$brapiToken\"")

        val firebaseAppCheckDebugToken: String? = properties.getProperty("FIREBASE_APP_CHECK_DEBUG_TOKEN")
        if (firebaseAppCheckDebugToken.isNullOrEmpty()) {
            throw GradleException("ERRO: FIREBASE_APP_CHECK_DEBUG_TOKEN não encontrado no local.properties!")
        }
        resValue("string", "firebase_app_check_debug_token", "\"$firebaseAppCheckDebugToken\"")
    }

    buildFeatures {
        buildConfig = true
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.firebase.auth.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.firebase.storage)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
}