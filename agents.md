# agents.md — SignalInsight 项目上下文

> 本文档供编程智能体（AI Agent）快速理解项目，请在每次开始工作前阅读本文档。

---

## 项目概述

SignalInsight 是一款 Android 蜂窝网络信号智能监测仪，基于 Jetpack Compose + Material 3 构建。
通过 Android TelephonyManager API 获取蜂窝网络信号数据，实时展示 RSRP/RSRQ/SINR 等信号指标，
支持双卡独立监测（HorizontalPager 滑动切换），并提供综合信号质量评估（评分+诊断+短板建议）和信号指标科普教育体系。

- **包名**: `cn.debubu.signalinsight`
- **应用 ID**: `cn.debubu.signalinsight`
- **minSdk**: 31 (Android 12)
- **targetSdk / compileSdk**: 36 (Android 16)
- **架构**: 单 Activity + MVVM + StateFlow + Navigation Compose（NavHost + 抽屉导航）

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.4.0 | 主要开发语言 |
| Android Gradle Plugin | 9.2.0 | 构建系统 |
| Gradle | 9.5.1 | 构建工具（腾讯镜像加速）|
| Jetpack Compose BOM | 2026.05.00 | UI 框架物料清单 |
| Material 3 | 随 BOM | 设计系统 |
| Navigation Compose | 2.9.8 | 页面路由导航（支持参数化路由 + 转场动画）|
| Activity Compose | 1.12.2 | Activity 集成 |
| Lifecycle Runtime KTX | 2.10.0 | 生命周期感知 |
| Core KTX | 1.17.0 | AndroidX 核心扩展 |

版本集中管理于 `gradle/libs.versions.toml`，依赖声明使用 `libs.` 别名。
Gradle 发行版使用腾讯镜像加速：`https://mirrors.cloud.tencent.com/gradle/gradle-9.5.1-bin.zip`

---

## 项目结构

```
SignalInsight/
├── build.gradle.kts                    # 根构建脚本
├── settings.gradle.kts                 # 项目设置
├── gradle.properties                   # Gradle 属性
├── gradle/
│   ├── libs.versions.toml              # ★ 版本目录（所有依赖版本集中管理）
│   └── wrapper/gradle-wrapper.properties  # Gradle 9.5.1（腾讯镜像）
├── local.properties                    # sdk.dir 指向本地 SDK
├── keystore.properties                 # 签名密钥配置（版本管理）
├── README.md                           # 项目文档（数据流图 + 指标说明）
├── app/
│   ├── build.gradle.kts                # App 模块构建脚本（含 debug/release 签名）
│   ├── signal_insight.jks              # 签名密钥库（版本管理）
│   ├── proguard-rules.pro              # ProGuard 混淆规则
│   └── src/
│       ├── androidTest/                    # 集成测试
│       │   └── java/cn/debubu/signalinsight/PermissionFlowTest.kt
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/cn/debubu/signalinsight/
│       │   │   ├── MainActivity.kt              # 入口 Activity
│       │   │   ├── SignalInsightApplication.kt  # Application 类
│       │   │   ├── data/
│       │   │   │   ├── cellular/
│       │   │   │   │   ├── CellularRepository.kt         # 信号数据仓库（callbackFlow）
│       │   │   │   │   ├── CellularSignalModel.kt        # 信号数据模型（三层架构）
│       │   │   │   │   └── SignalQualityEvaluator.kt     # ★ 信号质量评估器（纯逻辑）
│       │   │   │   ├── permission/
│       │   │   │   │   └── PermissionManager.kt          # 权限管理
│       │   │   │   └── theme/
│       │   │   │       └── ThemeManager.kt               # 主题管理（DataStore）
│       │   │   └── ui/
│       │   │       ├── cellular/
│       │   │       │   ├── CellularPage.kt              # 信号监测主容器（双卡 Pager）
│       │   │       │   ├── SimContentPage.kt            # ★ 单卡内容模板（无状态）
│       │   │       │   ├── SignalOverviewScreen.kt      # ★ 信号综合概览页（评分环）
│       │   │       │   ├── CellularViewModel.kt         # 信号数据 ViewModel
│       │   │       │   ├── BandExplainer.kt             # 频段科普页
│       │   │       │   ├── RSRPExplainer.kt             # RSRP 科普页
│       │   │       │   ├── RSRQExplainer.kt             # RSRQ 科普页
│       │   │       │   ├── SINRExplainer.kt             # SINR 科普页
│       │   │       │   ├── RSSIExplainer.kt             # RSSI 科普页
│       │   │       │   ├── PCIExplainer.kt              # PCI 科普页
│       │   │       │   ├── EARFCNExplainer.kt           # EARFCN 科普页
│       │   │       │   ├── TACExplainer.kt              # TAC 科普页
│       │   │       │   └── ExplainerUtils.kt            # 科普页共享组件
│       │   │       ├── about/
│       │   │       │   └── AboutScreen.kt               # 关于页（动态版本号 + 隐私说明）
│       │   │       ├── main/
│       │   │       │   └── MainScreen.kt                # 主导航（NavHost + 抽屉）
│       │   │       ├── permission/
│       │   │       │   ├── PermissionScreen.kt          # 权限申请页面
│       │   │       │   └── PermissionViewModel.kt       # 权限管理 ViewModel
│       │   │       ├── settings/
│       │   │       │   ├── SettingsScreen.kt            # ★ 设置页面（主题管理）
│       │   │       │   └── ThemeViewModel.kt            # ★ 主题 ViewModel
│       │   │       └── theme/
│       │   │           ├── Color.kt                     # 配色定义
│       │   │           ├── ColorSchemePresets.kt        # ★ 主题预设色方案
│       │   │           ├── Theme.kt                     # 主题（edgeToEdge）
│       │   │           └── Type.kt                      # 排版样式
│       │   └── res/
│       │       ├── drawable/        # 图标资源
│       │       ├── mipmap-*/        # 启动图标
│       │       ├── values/          # strings.xml（中文，含所有指标科普文本）
│       │       ├── values-en/       # strings.xml（英文，含完整科普内容）
│       │       └── values-night/    # 暗色主题 themes.xml
│       └── test/                    # 单元测试骨架
└── .gitignore
```

---

## 页面架构（Navigation Compose）

```
MainActivity
  └── setContent { SignalInsightTheme { Surface { MainScreen(...) } } }
        └── MainScreen (ModalNavigationDrawer + NavHost)
              ├── TopAppBar: 动态标题 + 菜单/返回按钮
              ├── ModalNavigationDrawer
              │     ├── 信号监测 → NavRoutes.CELLULAR
              │     ├── 设置     → NavRoutes.SETTINGS
              │     └── 关于     → NavRoutes.ABOUT
              └── NavHost(startDestination = "cellular")
                    ├── composable("cellular")
                    │     └── CellularPage(cellularViewModel)
                    │           ├── HorizontalPager
                    │           │   ├── Page 0 → SimContentPage(sim1SignalData)
                    │           │   └── Page 1 → SimContentPage(sim2SignalData)
                    │           └── NavigationBar（底部 SIM 切换 + 滑动指示条）
                    ├── composable("explainer/{metricKey}")
                    │     └── AnimatedContent(slideIn + fadeIn)
                    │           └── when(metricKey):
                    │                 BAND/EARFCN/PCI/RSRP/RSRQ/RSSI/SINR/TAC/OVERVIEW
                    ├── composable("settings")
                    │     └── SettingsScreen(themeViewModel)
                    └── composable("about")
                          └── AboutScreen()
```

---

## 核心数据模型（三层架构）

### 第一层：原始数据（CellularSignalInfo）

```kotlin
data class CellularSignalInfo(
    val simSlotId: Int, val operatorName: String, val networkType: String,
    val dbm: Int,     // 信号强度 dBm（通用指标）
    val rsrp: Int,    // RSRP / SS-RSRP（dBm）
    val rsrq: Int,    // RSRQ / SS-RSRQ（dB）
    val sinr: Int,    // SINR / SS-SINR 或 LTE rssnr（dB）
    val rssi: Int,    // RSSI（dBm，仅 LTE/GSM 可用，NR/UMTS 不可用）
    val pci: Int,     // 物理小区标识
    val earfcn: Int,  // 中心频点
    val band: String, // 频段（如 "n78", "B3"）
    val tac: Int,     // 跟踪区域码
)
// 不可用字段使用 Int.MAX_VALUE 标记
```

### 第二层：传输数据（CellularData）

```kotlin
data class CellularData(
    val servingCell: CellularSignalInfo?,
    val neighborCells: List<NeighborCellInfo>,
    val timestamp: Long
)
```

### 第三层：UI 数据（SignalData）

```kotlin
data class SignalData(
    val dbm: Int, val progress: Float, val operatorName: String,
    val networkType: String, val rsrp: Int, val rsrq: Int, val sinr: Int,
    val rssi: Int, val pci: Int, val earfcn: Int, val band: String, val tac: Int,
)
```

### NeighborCellInfo（邻小区）

```kotlin
data class NeighborCellInfo(
    val pci: Int, val rsrp: Int, val rsrq: Int, val sinr: Int,
    val rssi: Int, val earfcn: Int, val band: String
)
```

---

## 数据流架构（完整链路）

```
┌──────────────────┐     ┌────────────────────┐     ┌──────────────────┐     ┌───────────┐
│ Telephony        │──→  │ CellularRepository  │──→  │ CellularViewModel │──→  │ Compose   │
│ Manager API      │     │ (callbackFlow)      │     │ (StateFlow)      │     │ UI        │
│                  │     │                     │     │                   │     │           │
│ ┌──────────────┐ │     │ ┌─────────────────┐ │     │ ┌───────────────┐ │     │           │
│ │ CellInfo     │ │     │ │ 被动监听        │ │     │ │ resumeData    │ │     │           │
│ │ Listener     │─┤──→  │ │ CellInfoListener│─┤──→  │ │ Collection()  │─┤──→  │           │
│ │ (被动)       │ │     │ │                 │ │     │ │               │ │     │           │
│ ├──────────────┤ │     │ ├─────────────────┤ │     │ ├───────────────┤ │     │           │
│ │ requestCell  │ │     │ │ 主动轮询        │ │     │ │ refreshJob    │ │     │           │
│ │ InfoUpdate() │ │ ←── │ │ 每5s调用 TM.    │ │     │ │ 每5s触发      │ │     │           │
│ │ (主动)       │ │     │ │ requestCell     │ │     │ │               │ │     │           │
│ │              │ │     │ │ InfoUpdate()    │ │     │ │               │ │     │           │
│ └──────────────┘ │     │ └─────────────────┘ │     │ └───────────────┘ │     │           │
└──────────────────┘     └────────────────────┘     └──────────────────┘     └───────────┘
```

### 主动 + 被动混合刷新机制

**问题背景**：`CellInfoListener` 是被动的——Modem 只在信号发生"有意义变化"时才回调。
设备静止时可能数秒甚至十几秒无更新，导致 UI 卡顿。

**解决方案**：在 `CellularViewModel` 中新增 `refreshJob` 协程，前台运行时每 5 秒调用
`repository.requestCellInfoUpdate(slotId)`。该方法调用 `TelephonyManager.requestCellInfoUpdate()`
主动请求 Modem 刷新 CellInfo，结果通过已注册的 `CellInfoListener` 自动抵达 `callbackFlow`。

**生命周期**：
- `ON_RESUME` → `startDataCollection()` → 启动 `dataCollectionJob` + `refreshJob`
- `ON_PAUSE` → `pauseDataCollection()` → 取消两个 Job，停止监听和轮询
- 间隔选 5s：AOSP 源码 `ServiceStateTracker` 中节流为亮屏+充电 2s、其他 10s；
  实测小米设备有效阈值在 2s~5s 之间，5s 为跨设备安全值。

### 频段反算

**问题**：`CellIdentity.bands` API 对邻小区不可靠，小米设备返回占位值 `[1]`。
**解决**：EARFCN/NR-ARFCN → 频段查表优先于 `bands` API。覆盖国内主流 LTE（B1/3/5/8/34/38/39/40/41）
和 NR（n1/3/5/8/28/41/78/79）频段。
- **3G WCDMA**: 仅 dbm/psc/uarfcn/lac 可用（RSSI 为 @hide 系统 API）
- **2G GSM**: 仅 dbm/rssi/cid/lac 可用

### 第三步：ViewModel 处理（CellularViewModel.kt）

```kotlin
// 双卡独立存储
private val _sim1Data = MutableStateFlow<CellularData?>(null)
private val _sim2Data = MutableStateFlow<CellularData?>(null)

// 派生 UI 数据
val sim1SignalData: StateFlow<SignalData> = _sim1Data.map { ... }.stateIn(...)
val sim2SignalData: StateFlow<SignalData> = _sim2Data.map { ... }.stateIn(...)
```

### 第四步：UI 渲染（SimContentPage.kt）

```kotlin
val sim1Data by viewModel.sim1SignalData.collectAsState()
// 5G → "SS-RSRP: -98" | 4G → "RSRP: -93" | 无信号 → "N/A"
// Int.MAX_VALUE → 显示 "N/A"
```

---

## MIUI 兼容：双监听器模式（重要架构决策）

**问题**：MIUI/Xiaomi 设备上 `TelephonyCallback.CellInfoListener` 不返回 LTE RSSNR（SINR）。

**方案**：CellInfo + PhoneStateListener 双路监听

```kotlin
// 1. CellInfo 监听器（主数据源）
val callback = TelephonyCallback.CellInfoListener { list -> 
    extractCellularData(list, slotId, operatorName, _lteSinrFallback)
}
// 2. PhoneStateListener（SINR 备用，MIUI 设备）
val ssListener = PhoneStateListener { signalStrength ->
    signalStrength?.cellSignalStrengths?.forEach { css ->
        if (css is CellSignalStrengthLte && css.rssnr != Int.MAX_VALUE) {
            _lteSinrFallback = css.rssnr
        }
    }
}
tm.listen(ssListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
```

**关键约束**：
- `PhoneStateListener(context.mainExecutor)` — API 31+ 必需 Executor 参数
- 备用值仅用于服务小区（邻小区不使用，避免全部显示相同 SINR）
- `fromCellInfo()` 三级降级：`rssnr → lteRssnrFallback → Int.MAX_VALUE`

---

## 主题管理

- **ThemeManager**：基于 DataStore 持久化用户主题偏好
- **ThemeViewModel**：管理主题状态（动态取色开关 + 预设主题索引）
- **ColorSchemePresets**：多种预设配色方案
- **Material You 动态取色**：Android 12+ 支持（与预设主题互斥）
- 设置页提供：动态取色开关 + 主题色圆点选择器

**预设色方案**：
| 名称 | 色值 | 说明 |
|------|------|------|
| 绿色 | #4CAF50 | 默认（信号主题）|
| 蓝色 | #2196F3 | Material 经典蓝 |
| 紫色 | #9C27B0 | 深紫 |
| 红色 | #F44336 | 红 |
| 橙色 | #FF9800 | 橙 |

---

## 信号质量评估器（SignalQualityEvaluator）

纯逻辑单例，不依赖 Android 框架，可单元测试。

- **综合评分**：0-100 分
- **评级**：EXCELLENT / GOOD / FAIR / POOR / WEAK（含表情符号）
- **自然语言诊断**：根据各参数自动生成描述
- **短板识别**：找出最低分参数并生成改善建议
- **按网络类型差异化权重**：
  - 5G: RSRP 40% + RSRQ 25% + SINR 35%
  - 4G: RSRP 35% + RSRQ 20% + SINR 30% + RSSI 15%
  - 3G/2G: 简化的 dbm + RSSI 加权

---

## 国际化

- 默认语言：中文（values/strings.xml）
- 英文支持：values-en/strings.xml（完整科普内容翻译）
- 所有 UI 文本使用 `stringResource(R.string.xxx)`

---

## 编码规范

### 依赖管理
- **必须使用** `libs.versions.toml` 集中管理版本，不得硬编码版本号。
- 在 `build.gradle.kts` 中使用 `alias(libs.plugins.xxx)` 和 `libs.xxx` 引用。

### 包名规范
- 根包名: `cn.debubu.signalinsight`
- UI 层: `cn.debubu.signalinsight.ui.xxx`
- 数据层: `cn.debubu.signalinsight.data.xxx`

### Compose 规范
- 页面容器以 `Page` 结尾（如 `CellularPage`），全屏页面以 `Screen` 结尾（如 `SettingsScreen`、`SignalOverviewScreen`）
- 状态管理使用 `ViewModel + StateFlow`，UI 层只负责展示（无状态）
- 详情/科普页面以 `Explainer` 结尾（如 `RSRPExplainer.kt`）

### 权限
- `AndroidManifest.xml` 已声明 `READ_PHONE_STATE`、`ACCESS_FINE_LOCATION` 等权限
- 运行时权限通过 `PermissionManager` + `PermissionViewModel` 管理
- 权限页面支持中英文

---

## 当前开发状态

### ✅ 已完成
- [x] MVVM + StateFlow 数据架构（三层模型 + callbackFlow + StateFlow）
- [x] Navigation Compose 导航（ModalNavigationDrawer + NavHost）
- [x] 双卡信号监测（HorizontalPager + 底部 SIM 滑动条）
- [x] 信号概览页（综合评分环 + 诊断 + 短板建议）
- [x] 8 个指标全屏科普页（含中英文内容，转场动画）
- [x] 信号质量评估器（SignalQualityEvaluator，纯逻辑可测试）
- [x] Material You 动态取色 + 预设主题（ThemeManager + ColorSchemePresets）
- [x] 设置页（主题管理）
- [x] 权限管理（运行时申请 + 引导 + 中英文）
- [x] 生命周期管理（ON_RESUME 恢复 / ON_PAUSE 暂停数据收集）
- [x] MIUI 兼容（双监听器 SINR 回退）
- [x] HOT 插拔监听（SubscriptionManager.OnSubscriptionsChangedListener）
- [x] 初始化防抖保护（hasValidEver 防止网络切换时空数据闪烁）
- [x] 签名配置（debug + release 共用的 keystore）
- [x] 国际化（中文 + 英文）
- [x] 关于页
- [x] README.md 项目文档（含架构图和指标说明）
- [x] Compose UI 测试（权限流程）

### ⬜ 待实现
- [ ] **图表展示** — 信号变化趋势图（Compose Charts）
- [ ] **后台记录** — Foreground Service 持续监测 + Room 数据库持久化
- [ ] **ProGuard 规则** — 补充 release 构建的混淆规则（目前为空）
- [ ] **更多集成测试** — ViewModel、数据解析逻辑的测试

---

## 构建与运行

```bash
# 调试构建
./gradlew assembleDebug

# 安装到设备（自动签名）
./gradlew installDebug

# 运行集成测试（需连接设备）
./gradlew connectedDebugAndroidTest

# 清除构建
./gradlew clean

# 停止 Gradle 守护进程
./gradlew --stop

# 查看依赖树
./gradlew app:dependencies
```

---

## Git 约定

- 提交信息格式: `<type>: <description>`（feat/fix/refactor/docs/test/chore）
- 远程仓库: `git@gitee.com:debumao/SingnalInsight.git`

---

## 常见问题

**Q: 为什么 minSdk 是 31？**
A: 使用 `TelephonyManager.requestCellInfoUpdate()` 和 `TelephonyCallback.CellInfoListener`，需要 Android 12+。

**Q: 为什么没有 Retrofit/OkHttp？**
A: 信号监测工具，不发起网络请求，读取系统 TelephonyManager 的本地数据。

**Q: 双卡如何工作？**
A: 两张 SIM 卡独立创建 callbackFlow，通过 `combine()` 合并；UI 层通过 HorizontalPager 左右滑动切换显示。

**Q: MIUI 设备 SINR 显示异常？**
A: 已通过 PhoneStateListener 双监听器模式修复。如仍有问题请反馈。

---

*最后更新: 2026-06-13*
