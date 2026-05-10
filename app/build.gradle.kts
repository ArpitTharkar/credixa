import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.arpit.myapplication"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.arpit.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Read keystore passwords from local.properties to avoid committing secrets
            val propsFile = rootProject.file("local.properties")
            val props = Properties()
            if (propsFile.exists()) {
                props.load(FileInputStream(propsFile))
            }
            val storePwd = props.getProperty("RELEASE_STORE_PASSWORD", "changeit")
            val keyPwd = props.getProperty("RELEASE_KEY_PASSWORD", "changeit")
            val keyAliasProp = props.getProperty("RELEASE_KEY_ALIAS", "mykey")

            storeFile = file("release-keystore.jks")
            storePassword = storePwd
            keyAlias = keyAliasProp
            keyPassword = keyPwd
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // Use the LAN IP of your dev PC for local testing without USB.
            // For production builds, replace with your public API URL.
            buildConfigField("String", "SERVER_BASE_URL", "\"http://192.168.1.14:8081/api/\"")
        }
        debug {
            // Debug builds: localhost on emulator (10.0.2.2) will be used automatically;
            // set SERVER_BASE_URL for physical device testing over LAN.
            buildConfigField("String", "SERVER_BASE_URL", "\"http://192.168.1.14:8081/api/\"")
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
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}