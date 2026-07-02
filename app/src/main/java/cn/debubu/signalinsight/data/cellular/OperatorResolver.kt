package cn.debubu.signalinsight.data.cellular

/**
 * 根据 MCC + MNC 解析运营商中文名称。
 *
 * 用途：TelephonyManager 在某些机型上返回的 operatorAlphaLong 可能为 null
 * 或为空字符串，此时通过 PLMN 编码回退到硬编码映射表。
 *
 * @param mcc          移动国家代码（例 "460"）
 * @param mnc          移动网络代码（例 "00"）
 * @param fallbackName 系统提供的保底名称，查表失败时返回
 * @return 运营商名称，如 "China Mobile" / "China Telecom"
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
