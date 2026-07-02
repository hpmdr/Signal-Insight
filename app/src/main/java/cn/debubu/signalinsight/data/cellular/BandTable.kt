package cn.debubu.signalinsight.data.cellular

/**
 * LTE / NR 频点号 → 频段名称映射表。
 *
 * 覆盖国内运营商主流频段。不在表中的频点由 [CellularSignalInfo.fromCellInfo]
 * 回退到 [android.telephony.CellIdentity.bands] 系统 API。
 *
 * 数据来源：3GPP TS 36.101 / TS 38.101 定义的 NR-ARFCN 计算公式。
 */

/** LTE EARFCN → Band（国内主流 LTE FDD/TDD 频段） */
internal val LTE_EARFCN_TO_BAND = listOf(
    0 to 599 to "B1",        // 2100 MHz  FDD
    1200 to 1949 to "B3",    // 1800 MHz  FDD
    2400 to 2649 to "B5",    // 850 MHz   FDD
    3450 to 3799 to "B8",    // 900 MHz   FDD
    36200 to 36349 to "B34", // 2000 MHz  TDD
    37750 to 38249 to "B38", // 2600 MHz  TDD
    38250 to 38649 to "B39", // 1900 MHz  TDD
    38650 to 39649 to "B40", // 2300 MHz  TDD
    39650 to 41589 to "B41", // 2500 MHz  TDD
)

/** NR NR-ARFCN → Band（国内主流 5G NR 频段） */
internal val NR_ARFCN_TO_BAND = listOf(
    151600 to 160600 to "n28", // 700 MHz
    173400 to 178800 to "n5",  // 850 MHz
    185000 to 192000 to "n8",  // 900 MHz
    361000 to 376000 to "n3",  // 1800 MHz
    422000 to 434000 to "n1",  // 2100 MHz
    499200 to 537999 to "n41", // 2500 MHz
    620000 to 653333 to "n78", // 3500 MHz
    693334 to 733333 to "n79", // 4700 MHz
)

/**
 * 查找 LTE EARFCN 对应的频段名称。
 *
 * @param earfcn LTE 信道号，≤0 直接返回 null
 * @return 频段名称（如 "B3"），不在表中返回 null
 */
internal fun earfcnToBand(earfcn: Int): String? {
    if (earfcn <= 0) return null
    for ((range, band) in LTE_EARFCN_TO_BAND) {
        if (earfcn in range.first..range.second) return band
    }
    return null
}

/**
 * 查找 NR NR-ARFCN 对应的频段名称。
 *
 * @param nrarfcn 5G NR 信道号，≤0 直接返回 null
 * @return 频段名称（如 "n78"），不在表中返回 null
 */
internal fun nrarfcnToBand(nrarfcn: Int): String? {
    if (nrarfcn <= 0) return null
    for ((range, band) in NR_ARFCN_TO_BAND) {
        if (nrarfcn in range.first..range.second) return band
    }
    return null
}
