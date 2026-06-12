package cn.debubu.signalinsight.data.cellular

/**
 * 信号综合质量评估器
 *
 * 负责计算各网络制式的综合评分、评级及生成自然语言诊断文本。
 * 评分阈值与各 Explainer 详情页的评估段保持一致。
 */
object SignalQualityEvaluator {

    /** 综合评分结果 */
    data class Evaluation(
        val totalScore: Int,           // 综合评分 0-100
        val rating: Rating,            // 评级
        val diagnosis: String,         // 自然语言诊断
        val weaknessParam: String?,    // 短板参数名称（最拖后腿的参数）
        val weaknessHint: String,      // 针对短板参数的建议
        val paramScores: List<ParamScore>  // 各参数独立评分
    )

    /** 评级 */
    enum class Rating(val label: String, val emoji: String, val minScore: Int) {
        EXCELLENT("优秀", "🟢", 80),
        GOOD("良好", "🟡", 60),
        FAIR("一般", "🟠", 40),
        POOR("较差", "🔴", 20),
        WEAK("极弱", "⚫", 0);
    }

    /** 参数评分 */
    data class ParamScore(
        val key: MetricKey,
        val label: String,        // SS-RSRP / RSRP / RSSI 等
        val value: Int,           // 原始值（Int.MAX_VALUE 表示不可用）
        val valueStr: String,     // 显示的字符串（"-98" 或 "N/A"）
        val score: Int,           // 0-100
        val unit: String,         // dBm / dB / ""
        val briefExplanation: String  // 一句话说明
    )

    // ─── 各参数评分 ─────────────────────────────────────────

    /** SS-RSRP / RSRP 评分（与详情页评估段一致） */
    private fun scoreRsrp(v: Int) = when {
        v == Int.MAX_VALUE -> 0
        v > -85 -> 95
        v > -95 -> 80
        v > -105 -> 55
        v > -115 -> 30
        else -> 10
    }

    /** SS-RSRQ / RSRQ 评分（与详情页一致） */
    private fun scoreRsrq(v: Int) = when {
        v == Int.MAX_VALUE -> 0
        v > -10 -> 85
        v > -15 -> 50
        else -> 20
    }

    /** SS-SINR / RSSNR 评分（与详情页一致） */
    private fun scoreSinr(v: Int) = when {
        v == Int.MAX_VALUE -> 0
        v > 20 -> 95
        v > 10 -> 75
        v > 5 -> 50
        v > 0 -> 30
        else -> 10
    }

    /** RSSI 评分 */
    private fun scoreRssi(v: Int) = when {
        v == Int.MAX_VALUE -> 0
        v > -70 -> 90
        v > -90 -> 60
        else -> 25
    }

    /** 通用 dbm 评分（WCDMA/GSM 用） */
    private fun scoreDbm(v: Int) = when {
        v == Int.MAX_VALUE -> 0
        v > -85 -> 90
        v > -95 -> 70
        v > -105 -> 45
        else -> 20
    }

    // ─── 一句话说明 ────────────────────────────────────────

    private fun briefExplanation(key: MetricKey, is5g: Boolean): String = when (key) {
        MetricKey.RSRP -> if (is5g) "5G 同步信号参考功率，衡量信号强度"
                          else "LTE 参考信号接收功率，衡量信号强度"
        MetricKey.RSRQ -> if (is5g) "5G 同步信号接收质量，反映干扰程度"
                          else "LTE 参考信号接收质量，反映干扰程度"
        MetricKey.SINR -> if (is5g) "5G 信号与干扰加噪声比，影响网速"
                          else "LTE 参考信号信噪比，影响网速"
        MetricKey.RSSI -> "接收信号强度指示，综合的总功率测量"
        else -> ""
    }

    private fun paramLabel(key: MetricKey, is5g: Boolean, isLte: Boolean): String = when (key) {
        MetricKey.RSRP -> if (is5g) "SS-RSRP" else "RSRP"
        MetricKey.RSRQ -> if (is5g) "SS-RSRQ" else "RSRQ"
        MetricKey.SINR -> if (is5g) "SS-SINR" else if (isLte) "RSSNR" else "SINR"
        MetricKey.RSSI -> "RSSI"
        else -> ""
    }

    private fun paramUnit(key: MetricKey): String = when (key) {
        MetricKey.RSRP, MetricKey.RSSI -> "dBm"
        MetricKey.RSRQ, MetricKey.SINR -> "dB"
        else -> ""
    }

    // ─── 短板诊断 ──────────────────────────────────────────

    private fun weaknessHint(key: MetricKey): String = when (key) {
        MetricKey.RSRP -> "信号强度偏弱，建议靠近窗户或基站方向移动"
        MetricKey.RSRQ -> "可能存在同频干扰，信号接收质量不佳"
        MetricKey.SINR -> "信噪比较低，周围干扰源较多，影响数据传输速率"
        MetricKey.RSSI -> "总接收功率不足，可能处于信号覆盖边缘"
        else -> ""
    }

    private fun overallDiagnosis(
        totalScore: Int, rating: Rating,
        networkType: String,
        band: String,
        weakness: String?
    ): String = buildString {
        when (rating) {
            Rating.EXCELLENT -> append("你的 $networkType 信号非常好！")
            Rating.GOOD -> append("你的 $networkType 信号处于良好水平，日常使用流畅。")
            Rating.FAIR -> append("你的 $networkType 信号处于中等水平，基本可用。")
            Rating.POOR -> append("你的 $networkType 信号较弱，可能需要改善位置。")
            Rating.WEAK -> append("你的 $networkType 信号极弱，可能无法正常连接。")
        }
        if (band.isNotEmpty()) {
            append("当前频段 $band 覆盖表现良好。")
        }
        if (weakness != null) {
            append("主要短板在于 $weakness。")
        }
    }

    // ─── 主入口 ────────────────────────────────────────────

    /**
     * 对当前信号数据进行综合评估。
     * 根据网络类型自动选择可用参数和权重。
     */
    fun evaluate(signalData: SignalData): Evaluation {
        val is5g = signalData.networkType.contains("5G")
        val isLte = signalData.networkType.contains("4G") || signalData.networkType.contains("LTE")
        val isWcdma = signalData.networkType.contains("3G") || signalData.networkType.contains("WCDMA")
        val isGsm = signalData.networkType.contains("2G") || signalData.networkType.contains("GSM")

        val paramScores = mutableListOf<ParamScore>()

        if (is5g || isLte) {
            val rsrpScore = scoreRsrp(signalData.rsrp)
            paramScores.add(ParamScore(
                key = MetricKey.RSRP,
                label = paramLabel(MetricKey.RSRP, is5g, isLte),
                value = signalData.rsrp,
                valueStr = if (signalData.rsrp != Int.MAX_VALUE) signalData.rsrp.toString() else "N/A",
                score = rsrpScore,
                unit = paramUnit(MetricKey.RSRP),
                briefExplanation = briefExplanation(MetricKey.RSRP, is5g)
            ))

            val rsrqScore = scoreRsrq(signalData.rsrq)
            paramScores.add(ParamScore(
                key = MetricKey.RSRQ,
                label = paramLabel(MetricKey.RSRQ, is5g, isLte),
                value = signalData.rsrq,
                valueStr = if (signalData.rsrq != Int.MAX_VALUE) signalData.rsrq.toString() else "N/A",
                score = rsrqScore,
                unit = paramUnit(MetricKey.RSRQ),
                briefExplanation = briefExplanation(MetricKey.RSRQ, is5g)
            ))

            val sinrScore = scoreSinr(signalData.sinr)
            paramScores.add(ParamScore(
                key = MetricKey.SINR,
                label = paramLabel(MetricKey.SINR, is5g, isLte),
                value = signalData.sinr,
                valueStr = if (signalData.sinr != Int.MAX_VALUE) signalData.sinr.toString() else "N/A",
                score = sinrScore,
                unit = paramUnit(MetricKey.SINR),
                briefExplanation = briefExplanation(MetricKey.SINR, is5g)
            ))
        }

        if (isLte) {
            val rssiScore = scoreRssi(signalData.rssi)
            paramScores.add(ParamScore(
                key = MetricKey.RSSI,
                label = "RSSI",
                value = signalData.rssi,
                valueStr = if (signalData.rssi != Int.MAX_VALUE) signalData.rssi.toString() else "N/A",
                score = rssiScore,
                unit = "dBm",
                briefExplanation = briefExplanation(MetricKey.RSSI, false)
            ))
        }

        if (isWcdma || isGsm) {
            val dbmScore = scoreDbm(signalData.dbm)
            paramScores.add(ParamScore(
                key = MetricKey.RSRP,
                label = "dBm",
                value = signalData.dbm,
                valueStr = if (signalData.dbm != Int.MAX_VALUE) signalData.dbm.toString() else "N/A",
                score = dbmScore,
                unit = "dBm",
                briefExplanation = "总接收信号强度"
            ))
        }

        if (isGsm && signalData.rssi != Int.MAX_VALUE) {
            val rssiScore = scoreRssi(signalData.rssi)
            paramScores.add(ParamScore(
                key = MetricKey.RSSI,
                label = "RSSI",
                value = signalData.rssi,
                valueStr = signalData.rssi.toString(),
                score = rssiScore,
                unit = "dBm",
                briefExplanation = "GSM 接收信号强度"
            ))
        }

        // 计算加权总分
        val totalScore = when {
            is5g -> {
                val rsrp = paramScores.find { it.key == MetricKey.RSRP }?.score ?: 0
                val rsrq = paramScores.find { it.key == MetricKey.RSRQ }?.score ?: 0
                val sinr = paramScores.find { it.key == MetricKey.SINR }?.score ?: 0
                (rsrp * 40 + rsrq * 25 + sinr * 35) / 100
            }
            isLte -> {
                val rsrp = paramScores.find { it.key == MetricKey.RSRP }?.score ?: 0
                val rsrq = paramScores.find { it.key == MetricKey.RSRQ }?.score ?: 0
                val sinr = paramScores.find { it.key == MetricKey.SINR }?.score ?: 0
                val rssi = paramScores.find { it.key == MetricKey.RSSI }?.score ?: 0
                (rsrp * 35 + rsrq * 20 + sinr * 30 + rssi * 15) / 100
            }
            isWcdma -> paramScores.firstOrNull()?.score ?: 0
            isGsm -> {
                val dbm = scoreDbm(signalData.dbm)
                val rssi = if (signalData.rssi != Int.MAX_VALUE) scoreRssi(signalData.rssi) else 0
                (dbm * 40 + rssi * 60) / 100
            }
            else -> 0
        }

        val rating = Rating.entries.firstOrNull { totalScore >= it.minScore } ?: Rating.WEAK

        // 找出短板（分数最低的可用参数）
        val availableParams = paramScores.filter { it.value != Int.MAX_VALUE }
        val weakness = availableParams.minByOrNull { it.score }
        val weaknessName = weakness?.label
        val weaknessHintText = if (weakness != null) weaknessHint(weakness.key) else "所有指标表现良好，无需担心。"

        val diagnosis = overallDiagnosis(totalScore, rating, signalData.networkType, signalData.band, weaknessName)

        return Evaluation(
            totalScore = totalScore,
            rating = rating,
            diagnosis = diagnosis,
            weaknessParam = weaknessName,
            weaknessHint = weaknessHintText,
            paramScores = paramScores
        )
    }
}
