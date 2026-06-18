import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 读取签名配置：私有密钥优先，回退到公开测试密钥
val privatePropsFile = rootProject.file("private-keystore.properties")
val publicPropsFile = rootProject.file("keystore.properties")
val keystorePropertiesFile = if (privatePropsFile.exists()) privatePropsFile else publicPropsFile
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "cn.debubu.signalinsight"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "cn.debubu.signalinsight"
        minSdk = 31
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // 使用与 release 相同的签名，确保 test APK 兼容已安装的 release 版
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.create("debugCustom") {
                    keyAlias = keystoreProperties["keyAlias"].toString()
                    keyPassword = keystoreProperties["keyPassword"].toString()
                    storeFile = file(keystoreProperties["storeFile"].toString())
                    storePassword = keystoreProperties["storePassword"].toString()
                }
            }
        }
        release {
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.create("release") {
                    keyAlias = keystoreProperties["keyAlias"].toString()
                    keyPassword = keystoreProperties["keyPassword"].toString()
                    storeFile = file(keystoreProperties["storeFile"].toString())
                    storePassword = keystoreProperties["storePassword"].toString()
                }
            } else {
                signingConfigs["debug"]
            }
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.compose.animation:animation")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}