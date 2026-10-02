import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 读取签名配置：私有密钥优先，回退到公开测试密钥
//
// ⚠️⚠️ 重要：关于「正式发布」的签名密钥 ⚠️⚠️
//
// 线上 v1.0.0 ~ v1.1.0 全部由 **GitHub Actions 用 Secrets 中的密钥**签名，
// 该密钥的证书指纹为：
//     6C:1D:3F:27:19:AD:78:8C:80:C3:63:E3:E3:AD:49:C3:11:5A:A5:F4:0A:18:27:7A:7A:24:3D:36:80:6B:27:E2
// 一把只在 GitHub Secrets 里，**本地没有它的文件备份**（Secrets 只写不读，无法导出）。
//
// 本地的 private-release.jks 是**另一把**密钥，指纹：
//     B9:8A:E3:A9:F7:76:40:08:33:E6:5E:6B:03:9C:E3:5C:39:44:23:2D:89:DB:5E:35:E1:C6:24:9F:DA:B9:D4:4F
// 用它签出的包**与线上包签名不匹配**，用户无法覆盖安装！
//
// ✅ 因此：**分发一律走 GitHub Release（CI 构建）**，不要用本地 assembleRelease 的产物分发。
//    本地 release 构建仅用于验证能否编译通过 / 观察混淆后行为。
//
// 密钥库路径解析规则（兼容两种放置方式）：
//   1) storeFile 是绝对路径            → 直接使用
//   2) storeFile 相对「项目根目录」存在 → 使用根目录下的该文件
//   3) 否则                            → 按 Gradle 默认语义相对「app 模块目录」解析
val privatePropsFile = rootProject.file("private-keystore.properties")
val publicPropsFile = rootProject.file("keystore.properties")
val keystorePropertiesFile = if (privatePropsFile.exists()) privatePropsFile else publicPropsFile
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

/** 线上发布密钥的证书指纹（仅 CI 的 Secrets 中持有；用于本地构建时给出警示） */
val PUBLISHED_SIGNING_CERT_SHA256 =
    "6C:1D:3F:27:19:AD:78:8C:80:C3:63:E3:E3:AD:49:C3:11:5A:A5:F4:0A:18:27:7A:7A:24:3D:36:80:6B:27:E2"

/** 本地 private-release.jks 的证书指纹（非线上密钥，仅供本地验证） */
val LOCAL_ONLY_SIGNING_CERT_SHA256 =
    "B9:8A:E3:A9:F7:76:40:08:33:E6:5E:6B:03:9C:E3:5C:39:44:23:2D:89:DB:5E:35:E1:C6:24:9F:DA:B9:D4:4F"

/**
 * 将 storeFile 解析为实际存在的密钥库文件；都不存在时返回 null（由调用方决定回退策略）
 */
fun resolveStoreFile(raw: String?): File? {
    if (raw.isNullOrBlank()) return null
    val asFile = File(raw)
    if (asFile.isAbsolute) return asFile.takeIf { it.exists() }
    rootProject.file(raw).takeIf { it.exists() }?.let { return it }
    return file(raw).takeIf { it.exists() }
}

/**
 * 本地签名配置是否为「非线上发布密钥」。
 * 通过项的存在性判断：只要用的是 private-keystore.properties（本地密钥 B）就成立。
 * 不读取密钥库内容（避免构建期额外 I/O），仅依据使用的是哪份配置来判断。
 */
val usingLocalOnlyKeystore: Boolean =
    keystorePropertiesFile == privatePropsFile && privatePropsFile.exists()

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
        // 版本历史：
        //   1.0.7  → 1.1.0：曾按「签名密钥变更」发布，但该判断后被证伪——
        //                   线上密钥（GitHub Secrets 中）从未变更，v1.0.0~v1.1.0
        //                   实测证书指纹完全一致，用户可正常覆盖升级。
        //   1.1.0  → 1.1.1：参数详解页按 3GPP 规范逐条修正（报告范围、TAC 位宽等）
        //                   及频率单位统一，属修正类改动，故升补丁号。
        versionCode = 9
        versionName = "1.1.1"

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
                // 本地构建时给出明确警示：不要用本地产物分发
                if (usingLocalOnlyKeystore) {
                    val bar = "=".repeat(72)
                    logger.warn(bar)
                    logger.warn("警告: 正在使用「本地密钥」,签出的 APK 无法覆盖线上版本!")
                    logger.warn("  本次签名证书指纹: $LOCAL_ONLY_SIGNING_CERT_SHA256")
                    logger.warn("  线上发布的指纹  : $PUBLISHED_SIGNING_CERT_SHA256")
                    logger.warn("  两者不同 -> 该 APK 与 GitHub Release 上的包签名不匹配,")
                    logger.warn("  用户安装时会报 INSTALL_FAILED_UPDATE_INCOMPATIBLE。")
                    logger.warn("  正式分发请一律走 GitHub Release（由 CI 用 Secrets 中的密钥签名）。")
                    logger.warn("  本地 assembleRelease 仅用于验证编译 / 混淆结果。")
                    logger.warn(bar)
                }
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