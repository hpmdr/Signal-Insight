# Signal Insight - 信号监测仪

一款专业的 Android **蜂窝网络信号监测**工具，基于 Jetpack Compose + Material 3 开发。
实时展示 2G/3G/4G/5G 信号的各项核心指标，区分各网络类型参数差异，并提供详细的科普解读。

---

## 信号监听原理

```mermaid
flowchart TB
    subgraph Android["Android 系统层"]
        TM[TelephonyManager]
        SC[SubscriptionManager<br/>SIM卡订阅管理]
        TC[TelephonyCallback<br/>CellInfoListener]
    end

    subgraph Data["数据层 · CellularRepository.kt"]
        CF[callbackFlow<br/>协程回调流]
        EC[extractCellularData<br/>解析CellInfo]
        FI[fromCellInfo<br/>按网络类型分流]
    end

    subgraph Model["数据模型 · CellularSignalModel.kt"]
        L4G[CellInfoLte<br/>rsrp/rsrq/rssnr/rssi/pci/earfcn/band/tac]
        NR[CellInfoNr<br/>ssRsrp/ssRsrq/ssSinr/pci/nrarfcn/band/tac]
        W3G[CellInfoWcdma<br/>dbm/psc/uarfcn/lac]
        G2G[CellInfoGsm<br/>dbm/rssi/cid/lac]
    end

    subgraph ViewModel["ViewModel · CellularViewModel.kt"]
        COLL[collect { sim1, sim2 -> }]
        SF[.map{}.stateIn()<br/>StateFlow&lt;SignalData&gt;]
        LIFE[生命周期管理<br/>pause/resumeDataCollection]
    end

    subgraph UI["UI 层 · Compose"]
        CS[collectAsState<br/>Compose响应式状态]
        CP[CellularPage<br/>双卡HorizontalPager]
        SG[SimContentPage<br/>信号环 + 指标网格]
        NT[5G → SS-RSRP/SS-RSRQ/SS-SINR<br/>4G → RSRP/RSRQ/SINR<br/>不可用 → N/A]
    end

    TM --> SC
    SC -->|getSubscriptionIdForSlot| TC
    TC -->|onCellInfoChanged| CF
    CF --> EC
    EC --> FI

    FI -->|is CellInfoLte| L4G
    FI -->|is CellInfoNr| NR
    FI -->|is CellInfoWcdma| W3G
    FI -->|is CellInfoGsm| G2G

    L4G & NR & W3G & G2G -->|Flow 发射| COLL
    COLL -->|MutableStateFlow| SF
    LIFE -->|ON_PAUSE → 暂停<br/>ON_RESUME → 恢复| COLL

    SF -->|WhileSubscribed| CS
    CS --> CP
    CP --> SG
    SG --> NT
```

### 核心流程说明

1. **系统监听**：`TelephonyManager` + `TelephonyCallback.CellInfoListener` 注册回调，监听基站信号变化
2. **数据提取**：`callbackFlow` 将回调转为协程 Flow，`extractCellularData()` 解析 `CellInfo` 列表
3. **类型分流**：`fromCellInfo()` 根据 `CellInfo` 实际类型（LTE/NR/WCDMA/GSM）分别提取可用参数
4. **ViewModel 处理**：通过 `MutableStateFlow` → `.map{}.stateIn()` 转为不可变 UI 状态
5. **Compose 渲染**：`collectAsState()` 收集 StateFlow，驱动 UI 响应式更新

### 各网络类型参数差异

| 参数 | 5G NR | 4G LTE | 3G WCDMA | 2G GSM |
|------|-------|--------|---------|--------|
| **RSRP** | **SS-RSRP** (ssRsrp→csiRsrp) | RSRP (rsrp) | N/A | N/A |
| **RSRQ** | **SS-RSRQ** (ssRsrq→csiRsrq) | RSRQ (rsrq) | N/A | N/A |
| **SINR** | **SS-SINR** (ssSinr→csiSinr) | rssnr 作为 SINR 等效值 | N/A | N/A |
| **RSSI** | N/A（NR 无此概念） | rssi | N/A | rssi |
| **PCI** | pci | pci | psc（主扰码） | cid（小区ID） |
| **频率** | nrarfcn | earfcn | uarfcn | N/A |
| **频段** | bands API | bands API | N/A | N/A |
| **TAC** | tac | tac | lac | lac |

---

## 功能特性

### 📊 实时信号监测
- **双卡同时监测**：HorizontalPager 左右滑动切换 SIM 卡
- **信号环形进度条**：绿/黄/红三色直观指示信号强度
- **8 大核心指标网格**：按网络类型自适应标签（5G 显示 SS-RSRP，4G 显示 RSRP）
- **不可用指标自动隐藏**：3G/2G 不支持的参数显示 N/A

### 📖 参数科普解读
点击每个指标格块进入全屏详情页（带 AnimatedContent 转场动画）：

| 指标 | 科普内容 |
|------|---------|
| **RSRP / SS-RSRP** | 信号强度范围 + 影响因素 + 当前值个性化评估 |
| **RSRQ / SS-RSRQ** | 信号质量分级 + RSRP/RSRQ 联合判断 |
| **SINR / SS-SINR** | 信噪比 → 下载速度映射表 + 评估 |
| **RSSI** | RSSI vs RSRP 对比 + 什么时候参考 RSSI |
| **Band** | 运营商频段分布表（移动/联通/电信/广电） |
| **PCI** | 基站切换原理 + PCI Mod 3 干扰 |
| **EARFCN** | 频率换算公式 + 运营商频点范围 |
| **TAC** | TAC 更新场景 + 作用说明 |

### 🌐 国际化
- **中文 / 英文自动切换**（跟随系统语言）
- 所有 UI 文本、科普内容均双语

### 🎨 主题
- Material Design 3（Material You）
- 支持浅色/深色模式

---

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.4.0 |
| UI 框架 | Jetpack Compose + Material 3 | BOM 2026.05.00 |
| 架构 | MVVM (Repository + ViewModel + StateFlow) | - |
| 异步 | Kotlin Coroutines + Flow + StateFlow | - |
| 构建系统 | Gradle | 9.5.1 |
| AGP | Android Gradle Plugin | 9.2.0 |
| 动画 | AnimatedContent (spring+tween+SizeTransform) | - |
| 测试 | Compose UI Test (ui-test-junit4) | - |
| 编译 SDK | Android 16 (API 36) | - |
| 最低支持 | Android 12 (API 31) | - |

---

## 项目结构

```
app/src/main/java/cn/debubu/signalinsight/
├── data/
│   ├── cellular/
│   │   ├── CellularRepository.kt        # TelephonyManager + callbackFlow
│   │   └── CellularSignalModel.kt       # 数据模型 + 各网络类型参数解析
│   └── permission/
│       └── PermissionManager.kt         # 权限管理（含永久拒绝检测）
├── ui/
│   ├── cellular/
│   │   ├── CellularPage.kt              # 双卡切换主页面（HorizontalPager）
│   │   ├── CellularViewModel.kt         # 视图模型（StateFlow 驱动）
│   │   ├── SimContentPage.kt            # 单卡信号内容（信号环+指标网格+邻小区）
│   │   ├── BandExplainer.kt ~ TACExplainer.kt  # 8 个科普组件
│   │   └── ExplainerUtils.kt            # 共享组件（SectionCard / MetricExplainerShell）
│   ├── main/
│   │   └── MainScreen.kt                # 主界面（Scaffold + AnimatedContent + 生命周期）
│   ├── permission/
│   │   ├── PermissionScreen.kt          # 权限申请页面
│   │   └── PermissionViewModel.kt       # 权限视图模型
│   └── theme/
│       ├── Color.kt / Theme.kt / Type.kt  # Material 3 主题
├── MainActivity.kt                      # 应用入口
└── SignalInsightApplication.kt          # Application 类（单例仓库）
```

---

## 构建指南

### 环境要求
- **JDK** 17+（推荐 Microsoft JDK 21）
- **Android SDK** API 31 + API 36
- **Gradle** 9.5.1（gradlew 自动下载）

### 快速开始

```bash
# 克隆
git clone https://gitee.com/debumao/SingnalInsight.git
cd SingnalInsight

# 构建 Debug
./gradlew assembleDebug

# 运行测试（需要连接手机/模拟器）
./gradlew connectedDebugAndroidTest

# 构建 Release（需要 keystore.properties 签名配置）
./gradlew assembleRelease
```

### 签名配置

创建 `keystore.properties`：
```properties
storePassword=你的密码
keyPassword=你的密码
keyAlias=你的别名
storeFile=你的密钥库文件路径
```

已内置开发测试密钥 `app/signal_insight.jks`（密码 `Android123`），发布前请替换。

---

## 权限说明

| 权限 | 用途 | API 级别 |
|------|------|---------|
| `ACCESS_FINE_LOCATION` | 访问 CellInfo（系统要求位置权限才能获取基站信息） | 必须 |
| `READ_PHONE_STATE` | 读取运营商名称、网络类型 | Android 12 及以下 |
| `READ_BASIC_PHONE_STATE` | 读取基本电话状态 | Android 13+ 替代 |

> 应用**不会**收集或上传任何位置信息或个人数据，所有数据仅在本地设备上处理。

---

## 国际化

- **中文**：`res/values/strings.xml`
- **英文**：`res/values-en/strings.xml`
- 使用 `stringResource(R.string.xxx)` 标准 Compose API

---

## 许可证

```
版权所有 © 2026 debumao
本软件仅供学习和研究使用。未经许可，不得用于商业目的。
```
