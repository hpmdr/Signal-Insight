# Signal Insight - 信号监测仪

一款专业的 Android **蜂窝网络信号监测**工具，实时展示 4G/5G 信号的各项核心指标，并提供详细的科普解读。

---

## 功能特性

### 📊 实时信号监测
- **双卡同时监测**：支持双 SIM 卡切换查看，显示每张卡的信号数据
- **信号环形进度条**：直观展示信号强度等级（绿/黄/红三色指示）
- **8 大核心指标网格**：在同一页面集中展示所有重要参数

### 📖 参数科普解读
点击每个指标格块，弹出详细科普弹窗：

| 指标 | 弹窗内容 |
|------|---------|
| **RSRP** | 信号强度 5 级范围 + 影响因素 + 根据当前值个性化评估 |
| **RSRQ** | 信号质量 3 级 + RSRP/RSRQ 联合判断表 + 评估 |
| **SINR** | 信噪比 → 下载速度映射表（SINR 值对应 Mbps）+ 评估 |
| **RSSI** | RSSI 与 RSRP 对比说明 + 什么时候参考 RSSI + 评估 |
| **Band** | 频段概念 + **运营商频段分布表**（移动/联通/电信/广电）+ 频段特性对比 |
| **PCI** | 基站切换原理 + PCI Mod 3 干扰说明 + 评估 |
| **EARFCN** | 频率换算公式 + 运营商频点范围 + 评估 |
| **TAC** | TAC 更新场景 + 作用说明 + 评估 |

### 🌐 国际化
- **中文 / 英文自动切换**：跟随系统语言设置
- 所有 UI 文本、科普内容均中英文双语

### 🎨 主题适配
- 支持浅色/深色模式
- 使用 Material Design 3（Material You）自动适配主题色

---

## 技术架构

### 数据流

```
┌─────────────────────────────────────────────────────────────────┐
│ Layer 1: Android System API                                     │
│                                                                 │
│  TelephonyManager.allCellInfo                                    │
│  TelephonyCallback.CellInfoListener.onCellInfoChanged()          │
│  SubscriptionManager.OnSubscriptionsChangedListener (SIM热插拔) │
└──────────────────────────┬──────────────────────────────────────┘
                           │ callbackFlow
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ Layer 2: Data Layer (CellularRepository.kt)                     │
│                                                                 │
│  getCellularDataFlow(slotId) → Flow<CellularData>               │
│  getDualSimCellularDataFlow() → Flow<Pair<CellularData, Data>>  │
│  extractCellularData() → 解析 CellInfo 为结构化数据             │
│  CellularSignalInfo.fromCellInfo() → 按网络类型提取参数          │
│    ├── CellInfoLte  → rsrp, rsrq, rssnr, rssi, band, pci, tac  │
│    ├── CellInfoNr   → ssRsrp, ssRsrq, ssSinr, band, pci, tac   │
│    ├── CellInfoWcdma → dbm, pci, earfcn                         │
│    └── CellInfoGsm  → dbm, rssi, pci                            │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Flow (Kotlin Coroutine)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ Layer 3: ViewModel (CellularViewModel.kt)                       │
│                                                                 │
│  collect { } → mutableStateOf → State<SignalData>               │
│  信号解析在 Dispatchers.IO 线程执行，不阻塞主线程                │
└──────────────────────────┬──────────────────────────────────────┘
                           │ State Hoisting
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ Layer 4: Compose UI (Jetpack Compose + Material3)               │
│                                                                 │
│  CellularPage (主页面)                                           │
│    ├── CompactSimSwitcher (SIM切换器)                            │
│    ├── SignalCircle (信号环 + RSRP + 质量标签)                   │
│    ├── ServingCellGrid (8 个指标格块)                            │
│    │   └── click → selectedMetricKey                             │
│    ├── NeighborCellList (邻小区列表)                             │
│    └── ModalBottomSheet + when(key):                             │
│         ├── BandExplainer / RSRPExplainer / ... × 8              │
└─────────────────────────────────────────────────────────────────┘
```

### 项目结构

```
app/src/main/java/cn/debubu/signalinsight/
├── data/
│   ├── cellular/
│   │   ├── CellularRepository.kt        # 数据仓库 (TelephonyManager + Flow)
│   │   └── CellularSignalModel.kt       # 数据模型 + CellInfo 解析
│   ├── permission/
│   │   └── PermissionManager.kt         # 权限管理
│   └── di/
│       └── AppModule.kt                 # 依赖注入（预留）
├── ui/
│   ├── cellular/
│   │   ├── CellularPage.kt              # 主页面 Composable
│   │   ├── CellularViewModel.kt         # 视图模型
│   │   ├── BandExplainer.kt             # 频段科普弹窗
│   │   ├── RSRPExplainer.kt             # RSRP 科普弹窗
│   │   ├── RSRQExplainer.kt             # RSRQ 科普弹窗
│   │   ├── SINRExplainer.kt             # SINR 科普弹窗
│   │   ├── RSSIExplainer.kt             # RSSI 科普弹窗
│   │   ├── PCIExplainer.kt              # PCI 科普弹窗
│   │   ├── EARFCNExplainer.kt           # EARFCN 科普弹窗
│   │   ├── TACExplainer.kt              # TAC 科普弹窗
│   │   └── ExplainerUtils.kt            # 共享 UI 组件
│   ├── main/
│   │   └── MainScreen.kt                # 主界面 (导航抽屉 + 权限入口)
│   ├── permission/
│   │   ├── PermissionScreen.kt          # 权限申请页面
│   │   └── PermissionViewModel.kt       # 权限视图模型
│   └── theme/
│       ├── Color.kt                     # 主题色定义
│       ├── Theme.kt                     # Material3 主题
│       └── Type.kt                      # 字体样式
├── MainActivity.kt                      # 应用入口 Activity
└── SignalInsightApplication.kt          # Application 类
```

### 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.3.0 |
| UI 框架 | Jetpack Compose + Material3 | BOM 最新版 |
| 架构模式 | MVVM (Repository + ViewModel) | - |
| 异步 | Kotlin Coroutines + Flow | - |
| 构建系统 | Gradle | 8.13 |
| 编译 SDK | Android 16 (API 36) | - |
| 最低支持 | Android 12 (API 31) | - |

---

## 信号参数说明

### 4G LTE 参数

| 参数 | API 源 | 说明 | 单位 |
|------|--------|------|------|
| RSRP | `CellSignalStrengthLte.rsrp` | 参考信号接收功率，信号强度的核心指标 | dBm |
| RSRQ | `CellSignalStrengthLte.rsrq` | 参考信号接收质量，反映信号纯净度 | dB |
| **RSSNR** | `CellSignalStrengthLte.rssnr` | 信噪比等效值，作为 SINR 显示 | dB |
| RSSI | `CellSignalStrengthLte.rssi` | 接收信号强度指示（含噪声） | dBm |
| Band | `CellIdentityLte.bands` | 实际频段号（从系统 API 直接读取） | - |
| PCI | `CellIdentityLte.pci` | 物理小区标识，范围 0~503 | - |
| EARFCN | `CellIdentityLte.earfcn` | E-UTRA 绝对无线频率信道号 | - |
| TAC | `CellIdentityLte.tac` | 跟踪区域代码 | - |

### 5G NR 参数

| 参数 | API 源 | 说明 | 单位 |
|------|--------|------|------|
| **SS-RSRP** | `CellSignalStrengthNr.ssRsrp` | 同步信号参考接收功率，5G 核心指标 | dBm |
| **SS-RSRQ** | `CellSignalStrengthNr.ssRsrq` | 同步信号参考接收质量 | dB |
| **SS-SINR** | `CellSignalStrengthNr.ssSinr` | 同步信号信噪比 | dB |
| Band | `CellIdentityNr.bands` | 实际 NR 频段号 | - |
| PCI | `CellIdentityNr.pci` | 物理小区标识，范围 0~1007 | - |
| NR-ARFCN | `CellIdentityNr.nrarfcn` | NR 绝对无线频率信道号 | - |
| TAC | `CellIdentityNr.tac` | 跟踪区域代码 | - |

---

## 构建指南

### 环境要求

- **JDK** 17+（推荐 Microsoft JDK 21）
- **Android SDK** API 31 + API 36
- **Gradle** 8.13（gradlew 自动下载）

### 快速开始

```bash
# 1. 克隆项目
git clone https://gitee.com/debumao/SingnalInsight.git

# 2. 配置 SDK 路径（或设置 ANDROID_HOME 环境变量）
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# 3. 构建 Debug 版本
./gradlew assembleDebug

# 4. 构建 Release 版本（需要签名密钥）
./gradlew assembleRelease

# 5. 安装到连接的设备
./gradlew installDebug
```

### 签名配置

项目包含一个**开发测试密钥**（`app/signal_insight.jks`），密码为 `Android123`，仅用于开发测试阶段。

**正式发布前**请生成专属密钥：
```bash
keytool -genkey -v -keystore app/release.jks \
  -alias release_key -keyalg RSA -keysize 2048 -validity 10000
```

然后创建 `keystore.properties`：
```properties
storePassword=你的密码
keyPassword=你的密码
keyAlias=release_key
storeFile=release.jks
```

---

## 权限说明

应用需要以下权限以获取蜂窝网络信号数据：

| 权限 | 用途 | 所需 API 级别 |
|------|------|--------------|
| `ACCESS_FINE_LOCATION` | 访问 CellInfo（系统要求位置权限才能获取基站信息） | 必须 |
| `READ_PHONE_STATE` | 读取运营商名称、网络类型等电话状态 | Android 12 及以下 |
| `READ_BASIC_PHONE_STATE` | 读取基本电话状态 | Android 13+ 替代方案 |

应用不会收集或上传任何位置信息或个人数据，所有数据仅在本地设备上处理。

---

## 国际化

- **默认语言**：中文（`res/values/strings.xml`）
- **英语支持**：`res/values-en/strings.xml`
- 所有 UI 文本使用 `stringResource(R.string.xxx)` 标准 Compose API
- 科普内容完整双语翻译

---

## 许可证

```
版权所有 © 2026 debumao

本软件仅供学习和研究使用。未经许可，不得用于商业目的。
```
