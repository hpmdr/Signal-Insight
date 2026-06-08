package cn.debubu.signalinsight.data.cellular

import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager

/**
 * 根据 MCC 和 MNC 获取运营商中文名称
 *
 * @param mcc 移动国家代码
 * @param mnc 移动网络代码
 * @param fallbackName 保底运营商名称（系统提供的 operatorAlphaLong）
 * @return 运营商中文名称
 */
fun getOperatorNameByMccMnc(mcc: String?, mnc: String?, fallbackName: String? = null): String {
    if (mcc == null || mnc == null) return fallbackName ?: "Unknown"

    return when ("$mcc$mnc") {
        "46000", "46002", "46004", "46007" -> "China Mobile"
        "46001", "46006", "46009" -> "China Unicom"
        "46003", "46005", "46011" -> "China Telecom"
        "46015" -> "China Broadcasting"
        else -> fallbackName ?: "Unknown"
    }
}

/**
 * 蜂窝信号信息数据类
 * 包含从 Android TelephonyManager API 获取的原始信号数据
 *
 * @param simSlotId SIM 卡槽 ID (0 或 1)
 * @param operatorName 运营商名称
 * @param networkType 网络类型 (如 "5G NR", "4G LTE", "3G" 等)
 * @param dbm 信号强度 (dBm)
 * @param rsrp 参考信号接收功率 (dBm)
 * @param rsrq 参考信号接收质量 (dB)
 * @param sinr 信号与干扰加噪声比 (dB)
 * @param rssi 接收信号强度指示 (dBm)
 * @param pci 物理小区标识
 * @param earfcn E-UTRA 绝对无线频率信道号
 * @param band 频段 (如 "n78", "B3" 等)
 * @param tac 跟踪区域码
 * @param isPrimary 是否为主卡
 */
data class CellularSignalInfo(
    val simSlotId: Int = 0,
    val operatorName: String = "Unknown",
    val networkType: String = "Unknown",
    val dbm: Int = -120,
    val rsrp: Int = -120,
    val rsrq: Int = -20,
    val sinr: Int = -20,
    val rssi: Int = -120,
    val pci: Int = 0,
    val earfcn: Int = 0,
    val band: String = "",
    val tac: Int = 0,
    val isPrimary: Boolean = false
) {
    companion object {
        /**
         * 从 CellInfo 创建 CellularSignalInfo
         * const cellInfoNr = {
         *   // --- 基础状态与标识 (Basic Info) ---
         *   mRegistered: "YES",          // 是否已注册：YES 表示手机当前正连接并使用该小区上网
         *   mTimeStamp: 65267123439086,  // 系统时间戳 (ns)：表示该信号信息被获取时的系统运行时间
         *   mCellConnectionStatus: 0,    // 连接状态：0 表示主小区 (Primary)，负责核心数据传输
         *
         *   // --- 身份识别模块 (CellIdentityNr) ---
         *   cellIdentity: {
         *     mPci: 565,                 // 物理小区标识 (Physical Cell ID)：区分附近基站的临时编号
         *     mTac: 7678474,             // 跟踪区域代码 (Tracking Area Code)：类似区域邮编，用于移动追踪
         *     mNrArfcn: 627264,          // 5G 频道号：对应频率约 3400MHz 左右 (n78 频段)
         *     mBands: [78],              // 频段：n78。中国联通/电信 5G 主力频段，高带宽、高网速
         *     mMcc: "460",               // 移动国家代码：460 代表中国
         *     mMnc: "01",                // 移动网络代码：01 代表中国联通 (00 移动, 03/05/11 电信)
         *     mNci: 31409225733,         // 5G 小区唯一 ID：全球唯一的基站编号，用于精确位置识别
         *     mAlphaLong: "中国联通",      // 运营商全称
         *     mAlphaShort: "中国联通",     // 运营商简称
         *   },
         *
         *   // --- 信号强度模块 (CellSignalStrengthNr) ---
         *   cellSignalStrength: {
         *     // 【无效项警告】：2147483647 是 Java Int 最大值，代表该项数据系统暂未获取或暂不支持
         *     csiRsrp: 2147483647,       // CSI-RSRP (信道状态参考信号接收功率)：无效值
         *     csiRsrq: 2147483647,       // CSI-RSRQ (信道状态参考信号接收质量)：无效值
         *     csiSinr: 2147483647,       // CSI-SINR (信道状态信噪比)：无效值
         *
         *     // 【核心有效指标】：基于同步信号 (SS) 的测量值
         *     ssRsrp: -98,               // 信号接收功率 (dBm)：主参考值。-90~-100 属中等偏好，信号尚可
         *     ssRsrq: -12,               // 信号接收质量 (dB)：通常在 -3 到 -20 之间，-12 表现良好
         *     ssSinr: 9,                 // 信号信噪比 (dB)：数值越大下载越稳。9 表示连接稳定但非极速
         *
         *     level: 3,                  // 信号等级 (0-4/5)：系统综合评定。3 级代表信号“良好”
         *     parametersUseForLevel: 0,  // 计算等级所用的参考值类型：0 通常指基于 RSRP 计算
         *   }
         * };
         */
        fun fromCellInfo(cellInfo: CellInfo, simSlotId: Int, isPrimary: Boolean, operatorName: String = "Unknown"): CellularSignalInfo {
            val signalStrength = when (cellInfo) {
                is CellInfoLte -> {
                    val signalStrengthLte = cellInfo.cellSignalStrength
                    val cellIdentityLte = cellInfo.cellIdentity
                    val cellBand = cellIdentityLte.bands.firstOrNull()?.toString()
                        ?: "B${cellIdentityLte.earfcn / 1000}"
                    val lteSinr = signalStrengthLte.rssnr.let {
                        if (it != Int.MAX_VALUE) it else -20
                    }
                    val resolvedOperator = operatorName.takeUnless { it == "Unknown" }
                        ?: cellIdentityLte.operatorAlphaLong?.toString()
                        ?: "Unknown"
                    CellularSignalInfo(
                        simSlotId = simSlotId,
                        operatorName = resolvedOperator,
                        networkType = "4G LTE",
                        dbm = signalStrengthLte.dbm,
                        rsrp = signalStrengthLte.rsrp,
                        rsrq = signalStrengthLte.rsrq,
                        sinr = lteSinr,
                        rssi = signalStrengthLte.rssi,
                        pci = cellIdentityLte.pci,
                        earfcn = cellIdentityLte.earfcn,
                        band = cellBand,
                        tac = cellIdentityLte.tac,
                        isPrimary = isPrimary
                    )
                }
                /*
                *
                * */
                is CellInfoNr -> {
                    val signalStrengthNr = cellInfo.cellSignalStrength as CellSignalStrengthNr
                    val cellIdentityNr = cellInfo.cellIdentity as CellIdentityNr
                    val cellBand = cellIdentityNr.bands.firstOrNull()?.toString()
                        ?: "n${cellIdentityNr.nrarfcn / 1000}"

                    val invalidValue = Int.MAX_VALUE
                    val rsrp = if (signalStrengthNr.ssRsrp != invalidValue) {
                        signalStrengthNr.ssRsrp
                    } else if (signalStrengthNr.csiRsrp != invalidValue) {
                        signalStrengthNr.csiRsrp
                    } else {
                        -120
                    }

                    val rsrq = if (signalStrengthNr.ssRsrq != invalidValue) {
                        signalStrengthNr.ssRsrq
                    } else if (signalStrengthNr.csiRsrq != invalidValue) {
                        signalStrengthNr.csiRsrq
                    } else {
                        -20
                    }

                    val sinr = if (signalStrengthNr.ssSinr != invalidValue) {
                        signalStrengthNr.ssSinr
                    } else if (signalStrengthNr.csiSinr != invalidValue) {
                        signalStrengthNr.csiSinr
                    } else {
                        -20
                    }

                    val resolvedOperator = operatorName.takeUnless { it == "Unknown" }
                        ?: cellIdentityNr.operatorAlphaLong?.toString()
                        ?: "Unknown"

                    CellularSignalInfo(
                        simSlotId = simSlotId,
                        operatorName = resolvedOperator,
                        networkType = "5G NR",
                        dbm = signalStrengthNr.dbm,
                        rsrp = rsrp,
                        rsrq = rsrq,
                        sinr = sinr,
                        pci = cellIdentityNr.pci,
                        earfcn = cellIdentityNr.nrarfcn,
                        band = cellBand,
                        tac = cellIdentityNr.tac,
                        isPrimary = isPrimary
                    )
                }
                is CellInfoWcdma -> {
                    val signalStrengthWcdma = cellInfo.cellSignalStrength
                    val cellIdentityWcdma = cellInfo.cellIdentity
                    val resolvedOperator = operatorName.takeUnless { it == "Unknown" }
                        ?: cellIdentityWcdma.operatorAlphaLong?.toString()
                        ?: "Unknown"
                    CellularSignalInfo(
                        simSlotId = simSlotId,
                        operatorName = resolvedOperator,
                        networkType = "3G WCDMA",
                        dbm = signalStrengthWcdma.dbm,
                        pci = cellIdentityWcdma.psc,
                        earfcn = cellIdentityWcdma.uarfcn,
                        tac = cellIdentityWcdma.lac,
                        isPrimary = isPrimary
                    )
                }
                is CellInfoGsm -> {
                    val signalStrengthGsm = cellInfo.cellSignalStrength
                    val cellIdentityGsm = cellInfo.cellIdentity
                    val resolvedOperator = operatorName.takeUnless { it == "Unknown" }
                        ?: cellIdentityGsm.operatorAlphaLong?.toString()
                        ?: "Unknown"
                    CellularSignalInfo(
                        simSlotId = simSlotId,
                        operatorName = resolvedOperator,
                        networkType = "2G GSM",
                        dbm = signalStrengthGsm.dbm,
                        rssi = signalStrengthGsm.rssi,
                        pci = cellIdentityGsm.cid,
                        tac = cellIdentityGsm.lac,
                        isPrimary = isPrimary
                    )
                }
                else -> CellularSignalInfo(
                    simSlotId = simSlotId, isPrimary = isPrimary,
                    operatorName = operatorName.takeUnless { it == "Unknown" } ?: "Unknown"
                )
            }
            return signalStrength
        }

        /**
         * 获取网络类型字符串
         */
        fun getNetworkTypeName(networkType: Int): String {
            return when (networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G WCDMA"
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G WCDMA"
                TelephonyManager.NETWORK_TYPE_GSM,
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_EDGE -> "2G GSM"
                else -> "Unknown"
            }
        }
    }
}

/**
 * 邻小区信息数据类
 * 包含邻小区的信号和网络信息
 *
 * @param pci 物理小区标识
 * @param rsrp 参考信号接收功率 (dBm)
 * @param rsrq 参考信号接收质量 (dB)
 * @param sinr 信号与干扰加噪声比 (dB)
 * @param rssi 接收信号强度指示 (dBm)
 * @param earfcn E-UTRA 绝对无线频率信道号
 * @param band 频段
 * @param isServing 是否为服务小区
 */
data class NeighborCellInfo(
    val pci: Int = 0,
    val rsrp: Int = -120,
    val rsrq: Int = -20,
    val sinr: Int = -20,
    val rssi: Int = -120,
    val earfcn: Int = 0,
    val band: String = "",
    val isServing: Boolean = false
) {
    companion object {
        /**
         * 从 CellInfo 创建 NeighborCellInfo
         */
        fun fromCellInfo(cellInfo: CellInfo, isServing: Boolean = false): NeighborCellInfo {
            return when (cellInfo) {
                is CellInfoLte -> {
                    val signalStrengthLte = cellInfo.cellSignalStrength
                    val cellIdentityLte = cellInfo.cellIdentity
                    val cellBand = cellIdentityLte.bands.firstOrNull()?.toString()
                        ?: "B${cellIdentityLte.earfcn / 1000}"
                    val lteSinr = signalStrengthLte.rssnr.let {
                        if (it != Int.MAX_VALUE) it else -20
                    }
                    NeighborCellInfo(
                        pci = cellIdentityLte.pci,
                        rsrp = signalStrengthLte.rsrp,
                        rsrq = signalStrengthLte.rsrq,
                        sinr = lteSinr,
                        rssi = signalStrengthLte.rssi,
                        earfcn = cellIdentityLte.earfcn,
                        band = cellBand,
                        isServing = isServing
                    )
                }
                is CellInfoNr -> {
                    val signalStrengthNr = cellInfo.cellSignalStrength as CellSignalStrengthNr
                    val cellIdentityNr = cellInfo.cellIdentity as CellIdentityNr
                    val cellBand = cellIdentityNr.bands.firstOrNull()?.toString()
                        ?: "n${cellIdentityNr.nrarfcn / 1000}"

                    val invalidValue = Int.MAX_VALUE
                    val rsrp = if (signalStrengthNr.ssRsrp != invalidValue) {
                        signalStrengthNr.ssRsrp
                    } else if (signalStrengthNr.csiRsrp != invalidValue) {
                        signalStrengthNr.csiRsrp
                    } else {
                        -120
                    }

                    val rsrq = if (signalStrengthNr.ssRsrq != invalidValue) {
                        signalStrengthNr.ssRsrq
                    } else if (signalStrengthNr.csiRsrq != invalidValue) {
                        signalStrengthNr.csiRsrq
                    } else {
                        -20
                    }

                    val sinr = if (signalStrengthNr.ssSinr != invalidValue) {
                        signalStrengthNr.ssSinr
                    } else if (signalStrengthNr.csiSinr != invalidValue) {
                        signalStrengthNr.csiSinr
                    } else {
                        -20
                    }

                    NeighborCellInfo(
                        pci = cellIdentityNr.pci,
                        rsrp = rsrp,
                        rsrq = rsrq,
                        sinr = sinr,
                        earfcn = cellIdentityNr.nrarfcn,
                        band = cellBand,
                        isServing = isServing
                    )
                }
                else -> NeighborCellInfo(isServing = isServing)
            }
        }
    }
}

/**
 * 完整的蜂窝数据信息
 * 包含主服务小区和所有邻小区信息
 *
 * @param servingCell 服务小区信息
 * @param neighborCells 邻小区列表
 * @param timestamp 数据时间戳
 */
data class CellularData(
    val servingCell: CellularSignalInfo? = null,
    val neighborCells: List<NeighborCellInfo> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * GSM 信号信息数据类
 * 包含 GSM 网络的详细信号参数
 *
 * @param signalStrength GSM 信号强度 (0-31)
 * @param bitErrorRate GSM 误码率 (0-7)
 * @param level GSM 信号等级
 * @param band GSM 频段
 * @param arfcn GSM 绝对无线频道号
 * @param frequency GSM 频率 (MHz)
 */
data class GsmSignalModel(
    val signalStrength: Int = 0,
    val bitErrorRate: Int = 0,
    val level: Int = 0,
    val band: String = "",
    val arfcn: Int = 0,
    val frequency: Double = 0.0
)

/**
 * LTE 信号信息数据类
 * 包含 LTE 网络的详细信号参数
 *
 * @param rsrp 参考信号接收功率 (dBm)
 * @param rsrq 参考信号接收质量 (dB)
 * @param sinr 信号与干扰加噪声比 (dB)
 * @param cqi 信道质量指示
 * @param level LTE 信号等级
 * @param band LTE 频段
 * @param earfcn LTE 绝对无线频道号
 * @param frequency LTE 频率 (MHz)
 * @param pci LTE 物理小区标识
 */
data class LteSignalModel(
    val rsrp: Int = -120,
    val rsrq: Int = -20,
    val sinr: Int = -20,
    val cqi: Int = 0,
    val level: Int = 0,
    val band: String = "",
    val earfcn: Int = 0,
    val frequency: Double = 0.0,
    val pci: Int = 0
)

/**
 * 5G NR 信号信息数据类
 * 包含 5G NR 网络的详细信号参数
 *
 * @param ssRsrp SS 参考信号接收功率 (dBm)
 * @param ssRsrq SS 参考信号接收质量 (dB)
 * @param ssSinr SS 信号与干扰加噪声比 (dB)
 * @param level 5G NR 信号等级
 * @param band 5G NR 频段
 * @param nrarfcn 5G NR 绝对无线频道号
 * @param frequency 5G NR 频率 (MHz)
 * @param pci 5G NR 物理小区标识
 */
data class NrSignalModel(
    val ssRsrp: Int = -120,
    val ssRsrq: Int = -20,
    val ssSinr: Int = -20,
    val level: Int = 0,
    val band: String = "",
    val nrarfcn: Int = 0,
    val frequency: Double = 0.0,
    val pci: Int = 0
)