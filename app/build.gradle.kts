import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 读取签名配置：私有密钥优先，回退到公开测试密钥
//
// 密钥库路径解析规则（兼容两种放置方式，避免因「文件放哪」导致构建失败）：
//   1) storeFile 是绝对路径            → 直接使用
//   2) storeFile 相对「项目根目录」存在 → 使用根目录下的该文件
//   3) 否则                            → 按 Gradle 默认语义相对「app 模块目录」解析
// 说明：原先直接用 file(...)，其基准是 app 模块目录；若把 .jks 放在项目根目录
//       （与 private-keystore.properties 同级），会解析成 app/xxx.jks 而报文件不存在。
val privatePropsFile = rootProject.file("private-keystore.properties")
val publicPropsFile = rootProject.file("keystore.properties")
val keystorePropertiesFile = if (privatePropsFile.exists()) privatePropsFile else publicPropsFile
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

/** 将 storeFile 解析为实际存在的密钥库文件；都不存在时返回 null（由调用方决定回退策略） */
fun resolveStoreFile(raw: String?): File? {
    if (raw.isNullOrBlank()) return null
    val asFile = File(raw)
    if (asFile.isAbsolute) return asFile.takeIf { it.exists() }
    rootProject.file(raw).takeIf { it.exists() }?.let { return it }
    return file(raw).takeIf { it.exists() }
}

android {
    namespace = "cn.debubu.signalinsight"
    compileSdk {
        // API 37.2（Android 17）—— 当前最新稳定版
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "cn.debubu.signalinsight"
        minSdk = 31
        targetSdk = 37
        // v1.1.0：签名密钥变更（原密钥丢失，换用新密钥库）。
        // 因签名不匹配，已安装 v1.0.x 的用户需先卸载再安装本版本，
        // 故按次版本号递增（1.0.7 → 1.1.0）以明确区分。
        versionCode = 8
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // 使用与 release 相同的签名，确保 test APK 兼容已安装的 release 版
            resolveStoreFile(keystoreProperties["storeFile"]?.toString())?.let { ks ->
                signingConfig = signingConfigs.create("debugCustom") {
                    keyAlias = keystoreProperties["keyAlias"].toString()
                    keyPassword = keystoreProperties["keyPassword"].toString()
                    storeFile = ks
                    storePassword = keystoreProperties["storePassword"].toString()
                }
            }
        }
        release {
            val ks = resolveStoreFile(keystoreProperties["storeFile"]?.toString())
            signingConfig = if (ks != null) {
                signingConfigs.create("release") {
                    keyAlias = keystoreProperties["keyAlias"].toString()
                    keyPassword = keystoreProperties["keyPassword"].toString()
                    storeFile = ks
                    storePassword = keystoreProperties["storePassword"].toString()
                }
            } else {
                // 密钥库缺失时回退到 debug 签名（仅便于本地出包验证，正式发版必须提供密钥库）
                logger.warn("[SignalInsight] 未找到 release 密钥库，已回退到 debug 签名")
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

    // APK 输出命名：SignalInsight-v<版本号>-release.apk（例：SignalInsight-v1.1.0-release.apk）
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                output.outputFileName.set(
                    "SignalInsight-v${defaultConfig.versionName}-${variant.buildType.orEmpty()}.apk"
                )
            }
        }
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