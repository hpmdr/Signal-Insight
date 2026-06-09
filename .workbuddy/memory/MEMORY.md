# Signal Insight 项目记忆

## 项目概述
- **项目名称**：Signal Insight
- **包名**：cn.debubu.signalinsight
- **应用 ID**：cn.debubu.signalinsight
- **项目路径**：C:\Users\hpmdr\Documents\code\SingnalInsight

## 技术栈
- **语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **构建系统**：Gradle 8.13
- **编译 SDK**：36
- **最小 SDK**：31
- **目标 SDK**：36

## 开发环境配置

### JDK 配置
- **版本**：Microsoft JDK 21.0.11
- **路径**：C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot
- **JAVA_HOME**：C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot

### Android SDK 配置
- **SDK 路径**：C:\Users\hpmdr\app\Android
- **ANDROID_HOME**：C:\Users\hpmdr\app\Android
- **已安装组件**：
  - platform-tools (最新版)
  - build-tools;36.0.0
  - platforms;android-36
  - platforms;android-31
  - cmdline-tools (latest)

### 环境变量
- **ANDROID_HOME** = C:\Users\hpmdr\app\Android
- **PATH** 包含：
  - C:\Users\hpmdr\app\Android\cmdline-tools\latest\bin
  - C:\Users\hpmdr\app\Android\platform-tools

### 项目配置文件
- **local.properties**：sdk.dir=C:/Users/hpmdr/app/Android

## 构建命令

### 清理构建
```bash
./gradlew clean
```

### 构建 Debug 版本
```bash
./gradlew assembleDebug
```

### 完整构建
```bash
./gradlew build
```

### 停止 Gradle 守护进程
```bash
./gradlew --stop
```

## 已知问题（待修复）

### 测试任务失败
- **原因**：文件被其他进程锁定
- **解决方案**：
  ```bash
  ./gradlew --stop
  ./gradlew clean
  ./gradlew build
  ```

## 已修复的问题

### 弃用 API 修复
1. **CellularRepository.kt:114** - `networkType` → `dataNetworkType`（API 30+）
2. **CellularPage.kt:555** - `Divider()` → `HorizontalDivider()`

### 数据源修复
- **LTE RSSNR**: `CellSignalStrengthLte.rssnr` 作为 LTE 的 SINR 等效值
- **Band 频段**: 使用 `bands.firstOrNull()` 从 CellIdentity API 获取真实值

## 架构说明

### UI 层架构（Composable + MVVM）
```
MainScreen.kt（主容器）
  ├── 权限未授权 → PermissionScreen
  ├── 权限已授权
  │   └── selectedDestination:
  │       ├── Cellular → CellularPage
  │       │   ├── HorizontalPager（左右滑动）
  │       │   │   ├── Page 0 → SimContentPage(sim1SignalData, sim1NeighborCells)
  │       │   │   └── Page 1 → SimContentPage(sim2SignalData, sim2NeighborCells)
  │       │   └── NavigationBar（底部，文字+滑动指示条）
  │       ├── About → AboutScreen
  │       └── Settings → SettingsScreen
  └── selectedExplainerKey != null → ExplainerDetailScreen（全屏详情页）
      └── when(key):
          ├── BandExplainer.kt
          ├── RSRPExplainer.kt
          ├── RSRQExplainer.kt
          ├── SINRExplainer.kt
          ├── RSSIExplainer.kt
          ├── PCIExplainer.kt
          ├── EARFCNExplainer.kt
          └── TACExplainer.kt
```

### 数据流
```
TelephonyManager → TelephonyCallback → callbackFlow
  → Flow<CellularData> → combine(SIM1, SIM2)
  → ViewModel.collect → mutableStateOf
  → Compose 渲染 → Explainer 读取当前值
```

## 国际化
- 默认语言：中文（values/strings.xml）
- 英文支持：values-en/strings.xml
- 所有 UI 文本使用 `stringResource(R.string.xxx)`
- 创建了 BandExplainer 等 8 个组件的完整中英文科普内容

## 项目结构
```
SingnalInsight/
├── app/
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── java/cn/debubu/signalinsight/
│   │   │   ├── data/cellular/
│   │   │   │   ├── CellularRepository.kt    # 数据仓库
│   │   │   │   └── CellularSignalModel.kt   # 数据模型
│   │   │   ├── data/permission/
│   │   │   ├── ui/cellular/
│   │   │   │   ├── CellularPlugin.kt        # 主页面
│   │   │   │   ├── CellularViewModel.kt     # 视图模型
│   │   │   │   ├── BandExplainer.kt         # 8 个科普弹窗
│   │   │   │   ├── RSRPExplainer.kt
│   │   │   │   ├── RSRQExplainer.kt
│   │   │   │   ├── SINRExplainer.kt
│   │   │   │   ├── RSSIExplainer.kt
│   │   │   │   ├── PCIExplainer.kt
│   │   │   │   ├── EARFCNExplainer.kt
│   │   │   │   ├── TACExplainer.kt
│   │   │   │   ├── ExplainerDetailScreen.kt # 全屏科普详情页
│   │   │   │   ├── ExplainerUtils.kt        # 共享组件
│   │   │   ├── ui/main/
│   │   │   ├── ui/permission/
│   │   │   ├── ui/theme/
│   │   │   └── MainActivity.kt
│   │   └── res/
│   │       ├── values/strings.xml           # 中文
│   │       ├── values-en/strings.xml        # 英文
│   │       └── ...
│   └── build/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── local.properties
```
