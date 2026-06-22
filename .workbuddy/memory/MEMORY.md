# Signal Insight 项目记忆

## 项目概述
- **项目名称**：Signal Insight
- **包名**：cn.debubu.signalinsight
- **应用 ID**：cn.debubu.signalinsight
- **项目路径**：C:\Users\hpmdr\Documents\code\SingnalInsight

## 技术栈
- **语言**：Kotlin
- **架构模式**：MVVM（ViewModel 使用 StateFlow，UI 用 collectAsState 收集）
- **构建系统**：Gradle 9.5.1
- **AGP**：9.2.0
- **Kotlin**：2.4.0
- **Compose BOM**：2026.05.00
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

## 测试
- **测试框架**：Compose UI Test (ui-test-junit4)
- **测试文件**：`app/src/androidTest/.../PermissionFlowTest.kt`
- **测试覆盖**：权限流程 UI 测试（初始渲染、点击、拒绝、授权跳转）
- **运行命令**：`./gradlew connectedDebugAndroidTest`（需手机连接 ADB）

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
  └── explainerRoute != null → AnimatedContent 转场动画
      └── when(route.key): BandExplainer / RSRPExplainer / ...（8 个科普组件）
```

### 数据流架构（完整链路）

```
┌─────────────┐     ┌──────────────────┐     ┌──────────────────┐     ┌───────────┐
│ Telephony   │──→  │ CellularRepository│──→  │ CellularViewModel │──→  │ Compose   │
│ Manager API │     │ (callbackFlow)   │     │ (StateFlow)      │     │ UI        │
└─────────────┘     └──────────────────┘     └──────────────────┘     └───────────┘
```

#### 第一步：系统接口监听（CellularRepository.kt）

使用 `TelephonyManager` + `TelephonyCallback.CellInfoListener`：

```kotlin
// 为每个 SIM 卡槽创建独立的 callbackFlow（slotId=0 为 SIM1, slotId=1 为 SIM2）
fun getCellularDataFlow(slotId: Int): Flow<CellularData> = callbackFlow {
    // 1. 通过 SubscriptionManager 获取 subscriptionId
    // 2. 创建 TelephonyManager.createForSubscriptionId(subscriptionId)
    // 3. 注册 telephonyManager.registerTelephonyCallback(cellInfoListener)
    // 4. Listener.onCellInfoChanged() → extractCellularData() → trySend()
    // 5. 返回初始 CellInfo: telephonyManager.allCellInfo
    // 6. awaitClose → 注销监听器
}

// 双卡合并
fun getDualSimCellularDataFlow(): Flow<Pair<CellularData, CellularData>> {
    return getCellularDataFlow(0).combine(getCellularDataFlow(1)) { sim1, sim2 -> sim1 to sim2 }
}
```

#### 第二步：数据提取（extractCellularData → CellularSignalInfo.fromCellInfo）

```
CellInfo（系统 API） → CellInfoLte / CellInfoNr / CellInfoWcdma / CellInfoGsm
                         ↓
                    CellularSignalInfo（统一数据类）
```

各网络类型参数提取规则（基于 Android 官方 API 参考文档）：

```
                    RSRP         RSRQ         SINR          RSSI        PCI          EARFCN       BAND         TAC
5G NR (SS-)        ssRsrp→      ssRsrq→      ssSinr→      ❌ 无 API  pci          nrarfcn      bands[0]     tac
                   csiRsrp↑     csiRsrq↑     csiSinr↑      ⚠️ NR 协议层有 RSSI                        or nxxx
                                                             (RSRQ 的分母)，但因 TDD 波束
                                                             使总功率波动剧烈，Google 未开放接口

4G LTE             rsrp         rsrq         rssnr        ✅ 可用    pci          earfcn       bands[0]     tac
                                                             (返回 [-113,-51])       or Bxx

3G WCDMA           ❌            ❌            ❌           ❌@hide    psc          uarfcn       ❌           lac
                                                             (系统API，第三方不可用)

2G GSM             ❌            ❌            ❌           ✅ 可用    cid          ❌           ❌           lac
```

关键处理：
- **5G NR**: `Int.MAX_VALUE` 检测 → 三级降级（SS → CSI → MAX_VALUE）。RSSI 在 NR 协议层存在（作为 RSRQ 的分母：RSRQ = N×RSRP/RSSI），但因 TDD 动态波束导致总功率波动剧烈、参考价值低，Google 未对应用层开放接口。App 显示 N/A，详情页在兼容性说明中解释了原因。
- **4G LTE**: `rssnr` 作为 SINR 等效值；RSSI 可用（getRssi() 返回 [-113,-51] dBm 或 UNAVAILABLE）。
- **3G WCDMA**: 仅 `dbm`/`psc`/`uarfcn`/`lac` 可用。RSRP/RSRQ/SINR/RSSI 均不适用（getRssi() 是 @hide 系统 API）。
- **2G GSM**: 仅 `dbm`/`rssi`(真实值)/`cid`/`lac` 可用。

#### 第三步：ViewModel 数据处理（CellularViewModel.kt）

```kotlin
// 原始数据（来自 Repository）
private val _sim1Data = MutableStateFlow<CellularData?>(null)
private val _sim2Data = MutableStateFlow<CellularData?>(null)
private val _activeSim = MutableStateFlow(1)

// 收集数据
init {
    viewModelScope.launch(Dispatchers.IO) {
        repository.getDualSimCellularDataFlow().collect { (sim1, sim2) ->
            _sim1Data.value = sim1
            _sim2Data.value = sim2
        }
    }
}

// 转换为 UI 数据模型
val sim1SignalData: StateFlow<SignalData> = _sim1Data
    .map { it?.servingCell.toSignalData() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SignalData())

// toSignalData() 使用默认参数，Int.MAX_VALUE 表示不可用
private fun CellularSignalInfo?.toSignalData(): SignalData = this?.let { cell ->
    SignalData(
        dbm = cell.dbm,
        progress = if (cell.dbm != Int.MAX_VALUE) ((cell.dbm + 120) / 60f).coerceIn(0f, 1f) else 0f,
        operatorName = cell.operatorName,
        networkType = cell.networkType,
        rsrp = cell.rsrp, rsrq = cell.rsrq, sinr = cell.sinr,
        rssi = cell.rssi, pci = cell.pci, earfcn = cell.earfcn,
        band = cell.band, tac = cell.tac,
    )
} ?: SignalData()
```

#### 第四步：UI 渲染（SimContentPage.kt）

```kotlin
val sim1Data by viewModel.sim1SignalData.collectAsState()

// 信号环：显示 dbm 值
// 5G → "SS-RSRP: -98" | 4G → "RSRP: -93" | 无信号 → "N/A"

// 指标网格：8 个 Metric 卡片
val is5gNet = signalData.networkType.contains("5G")
fun displayValue(value: Int): String = if (value != Int.MAX_VALUE) value.toString() else "N/A"

Metric(MetricKey.RSRP, if (is5gNet) "SS-RSRP" else "RSRP", displayValue(rsrp), "dBm")
Metric(MetricKey.RSRQ, if (is5gNet) "SS-RSRQ" else "RSRQ", displayValue(rsrq), "dB")
Metric(MetricKey.SINR, if (is5gNet) "SS-SINR" else "SINR", displayValue(sinr), "dB")
```

#### 生命周期适配

```kotlin
// MainScreen.kt 中的 LifecycleEventObserver
ON_RESUME → permissionViewModel.checkAllPermissions() + cellularViewModel.resumeDataCollection()
ON_PAUSE  → cellularViewModel.pauseDataCollection()
```

### MIUI 兼容：双监听器模式（重要架构决策）

**问题**：MIUI/Xiaomi 设备上 `TelephonyCallback.CellInfoListener` 回调的 `CellSignalStrengthLte.rssnr` 返回 `Int.MAX_VALUE`，无法读取 SINR。

**方案**：CellInfo + PhoneStateListener 双路监听

```kotlin
// 1. CellInfo 监听器（主数据源）
val callback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
    override fun onCellInfoChanged(list: List<CellInfo>) {
        extractCellularData(list, slotId, operatorName, _lteSinrFallback)
    }
}

// 2. PhoneStateListener（SINR 备用，MIUI 兼容）
@Suppress("DEPRECATION")
val ssListener = object : PhoneStateListener(context.mainExecutor) {
    // API 31+ 移除了 Looper 构造函数，必须用 Executor
    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
        for (css in signalStrength?.cellSignalStrengths ?: emptyList()) {
            if (css is CellSignalStrengthLte && css.rssnr != Int.MAX_VALUE) {
                _lteSinrFallback = css.rssnr
            }
        }
    }
}
tm.listen(ssListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
```

**关键约束**：
- `PhoneStateListener(context.mainExecutor)` — API 31+ 必需
- `_lteSinrFallback` 为类级 `var`，可被后续 CellInfo 回调读取
- **备用值仅用于服务小区**（`CellularSignalInfo.fromCellInfo()`）。邻小区不使用备用值，因为 SignalStrength 回调返回的是设备级别的 LTE RSSNR 聚合值，无法区分各邻小区，直接应用会在 UI 上显示出所有邻小区 SINR 相同的不良体验
- `fromCellInfo()` 三级降级：`rssnr → lteRssnrFallback → Int.MAX_VALUE`
- 已验证：MIUI 上服务小区 RSSNR = 21 dB 正确回退

## 导航架构（Navigation Compose）
- **方案**：Jetpack Navigation Compose（NavHost + NavController），依赖 `2.9.8`
- **路由定义**：在 `NavRoutes` object 中统一管理
  - `cellular` → 信号监测页
  - `settings` → 设置页
  - `about` → 关于页
  - `explainer/{metricKey}` → 详解页（含进入/退出/弹出动画）
- **动画**：NavHost 内置 `enterTransition/exitTransition/popEnterTransition/popExitTransition`
  - 标签页间：`fadeIn(250) + fadeOut(150)`
  - 进入详解：`slideInHorizontally(tween(300)) + fadeIn(250)`
  - 返回主页：对称滑出
- **新增页面**：在 NavHost 中加一行 `composable("路由") { Page() }`，无需手写动画

## 近期 UI 更新（2026-06-15 远程提交）
- 透明 AppBar / NavigationBar（边到边效果）
- SIM 卡图标（`ic_sim_1.xml` / `ic_sim_2.xml`）
- 全页面动态 padding 适配

#### 关键数据模型

```kotlin
// 原始数据模型（Repository 层）
data class CellularSignalInfo(
    val operatorName: String, val networkType: String,
    val dbm: Int, val rsrp: Int, val rsrq: Int, val sinr: Int,
    val rssi: Int, val pci: Int, val earfcn: Int, val band: String, val tac: Int,
    // 不可用字段为 Int.MAX_VALUE
)

// UI 数据模型（ViewModel → UI 层）
data class SignalData(
    val dbm: Int, val progress: Float, val operatorName: String, val networkType: String,
    val rsrp: Int, val rsrq: Int, val sinr: Int, val rssi: Int,
    val pci: Int, val earfcn: Int, val band: String, val tac: Int,
    // 不可用字段为 Int.MAX_VALUE，UI 显示 "N/A"
)
```

#### ⚠️ StateFlow → Compose 常见陷阱

1. **`derivedStateOf` 不能冷读 `StateFlow.value`** — 必须通过 `collectAsState()` 转换
2. **`SnapshotStateList` 元素修改** — 必须用 `list[index] = item.copy(...)` 替换整个元素
3. **`WhileSubscribed(5000)`** — 最后一个订阅者消失后 5 秒内仍保持上游活跃，适合快速切后台/前台
4. **双卡数据独立** — `sim1SignalData` / `sim2SignalData` 各自通过 `.map {}.stateIn()` 派生

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

## CellInfo 刷新机制（2026-06-22 解决）

### 问题
`CellInfoListener.onCellInfoChanged()` 是被动的——Modem 只在信号发生"有意义变化"时才回调。
设备静止时可能数秒甚至十几秒无更新，UI 显示卡顿。打开 Cellular-Z 后能跟着秒刷新。

### 根因
`CellInfoListener` 依赖 Modem 自行判断何时上报。`requestCellInfoUpdate()` 主动通过 RIL 层告诉 Modem 刷新。

### 解决方案
- `CellularRepository.requestCellInfoUpdate(slotId)`: fire-and-forget 调用 `tm.requestCellInfoUpdate()`，
  不处理回调数据（数据通过已有的 CellInfoListener 自动抵达 callbackFlow）
- `CellularViewModel`: 新增 `refreshJob`，前台每 5s 调用一次两个卡槽的主动刷新
- 生命周期：ON_RESUME 启动 refreshJob，ON_PAUSE 取消
- 间隔 5s 原因：AOSP `ServiceStateTracker` 节流为亮屏+充电 2s、其他 10s；
  实测小米设备有效阈值在 2s~5s 之间

### 修改文件
- `CellularRepository.kt`: +requestCellInfoUpdate() 方法
- `CellularViewModel.kt`: +refreshJob, startPeriodicRefresh(), 更新 pauseDataCollection/restartDataCollection

## 邻小区频段错误修复（2026-06-22）

### 问题
所有邻小区频段显示为"1"，但实际设备连接 n78。

### 根因
小米设备 `CellIdentity.bands` 对邻小区返回占位值 `[1]`，代码无条件信任了 `bands.firstOrNull()`。
旧 fallback `earfcn / 1000` 精度不足（EARFCN 1500 = Band 3，但 1500/1000=1）。

### 解决方案
- 新增 `earfcnToBand()` / `nrarfcnToBand()` 函数，覆盖国内主流 LTE/NR 频段
- 服务小区：bands 优先 → 查表兜底
- 邻小区：查表优先 → bands 兜底
- 所有频段统一加前缀（B/n）：n78, B3
