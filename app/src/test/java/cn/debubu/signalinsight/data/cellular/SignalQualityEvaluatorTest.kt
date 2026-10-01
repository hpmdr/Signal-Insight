package cn.debubu.signalinsight.data.cellular

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SignalQualityEvaluator] 单元测试 —— 纯逻辑，不依赖 Android 框架。
 *
 * 重点覆盖批次 A2 的核心不变量：
 * **权重分母只统计「实际可用」的参数**，参数不可用(Int.MAX_VALUE)不得被当作 0 分扣分。
 */
class SignalQualityEvaluatorTest {

    private val unavailable = Int.MAX_VALUE

    /** 构造 5G 数据：三个参数可分别置为不可用 */
    private fun nr(
        rsrp: Int = unavailable,
        rsrq: Int = unavailable,
        sinr: Int = unavailable
    ) = SignalData(networkType = "5G NR", rsrp = rsrp, rsrq = rsrq, sinr = sinr)

    private fun lte(
        rsrp: Int = unavailable,
        rsrq: Int = unavailable,
        sinr: Int = unavailable,
        rssi: Int = unavailable
    ) = SignalData(networkType = "4G LTE", rsrp = rsrp, rsrq = rsrq, sinr = sinr, rssi = rssi)

    // ── 基线：全参数可用时，结果必须与加权公式一致（防止归一化改动引入回归） ──

    @Test
    fun `5G 全参数可用时按 40-25-35 加权`() {
        // scoreRsrp(-91)=80, scoreRsrq(-10)=50, scoreSinr(17)=75
        // (80*40 + 50*25 + 75*35) / 100 = 70
        val r = SignalQualityEvaluator.evaluate(nr(rsrp = -91, rsrq = -10, sinr = 17))
        assertEquals(70, r.totalScore)
        assertEquals(SignalQualityEvaluator.Rating.GOOD, r.rating)
    }

    @Test
    fun `4G 全参数可用时按 35-20-30-15 加权`() {
        // scoreRsrp(-91)=80, scoreRsrq(-10)=50, scoreSinr(17)=75, scoreRssi(-80)=60
        // (80*35 + 50*20 + 75*30 + 60*15) / 100 = 6950/100 = 69
        val r = SignalQualityEvaluator.evaluate(
            lte(rsrp = -91, rsrq = -10, sinr = 17, rssi = -80)
        )
        assertEquals(69, r.totalScore)
    }

    // ── A2 核心：缺参数必须重新归一化，不得记 0 分 ──

    @Test
    fun `5G 缺 SINR 时权重重新归一化而不是按 0 分扣分`() {
        // 可用部分：(80*40 + 50*25) / (40+25) = 68
        // 旧算法(错误)：(80*40 + 50*25 + 0*35) / 100 = 44
        val r = SignalQualityEvaluator.evaluate(nr(rsrp = -91, rsrq = -10, sinr = unavailable))
        assertEquals(68, r.totalScore)
        assertTrue("缺参数不应把评级压到「一般」以下", r.totalScore >= 60)
        assertEquals(SignalQualityEvaluator.Rating.GOOD, r.rating)
        assertNotEquals("必须与旧算法的 44 分不同", 44, r.totalScore)
    }

    @Test
    fun `2G 仅有 dbm 时该参数独占 100% 权重`() {
        // scoreDbm(-75) = 90；旧算法：(90*40 + 0*60)/100 = 36
        val r = SignalQualityEvaluator.evaluate(
            SignalData(networkType = "2G GSM", dbm = -75, rssi = unavailable)
        )
        assertEquals(90, r.totalScore)
        assertEquals("2G 只有 dbm 一个可用参数参与评分", 1, r.paramScores.count { it.value != unavailable })
    }

    @Test
    fun `3G 仅有 dbm 时得分为该参数自身评分`() {
        val r = SignalQualityEvaluator.evaluate(
            SignalData(networkType = "3G WCDMA", dbm = -75)
        )
        assertEquals(90, r.totalScore)
    }

    @Test
    fun `4G 同时缺 SINR 与 RSSI 时按剩余权重归一化`() {
        // (80*35 + 50*20) / (35+20) = 3800/55 = 69
        val r = SignalQualityEvaluator.evaluate(
            lte(rsrp = -91, rsrq = -10, sinr = unavailable, rssi = unavailable)
        )
        assertEquals(69, r.totalScore)
    }

    // ── 边界：全不可用 / 极端值 ──

    @Test
    fun `全部参数不可用时为 0 分且无除零异常`() {
        val r = SignalQualityEvaluator.evaluate(nr())
        assertEquals(0, r.totalScore)
        assertEquals(SignalQualityEvaluator.Rating.WEAK, r.rating)
    }

    @Test
    fun `不可用参数仍进入 paramScores 供 UI 展示 N A`() {
        val r = SignalQualityEvaluator.evaluate(nr(rsrp = -91, rsrq = unavailable, sinr = unavailable))
        val sinr = r.paramScores.first { it.key == MetricKey.SINR }
        assertEquals(unavailable, sinr.value)
        assertEquals("N/A", sinr.valueStr)
    }

    @Test
    fun `短板识别不会选中不可用参数`() {
        // RSRP 80 分、RSRQ 50 分；SINR 不可用 → 短板必须是 RSRQ（5G 下标签为 SS-RSRQ）
        val r = SignalQualityEvaluator.evaluate(nr(rsrp = -91, rsrq = -10, sinr = unavailable))
        assertEquals("SS-RSRQ", r.weaknessParam)
    }

    @Test
    fun `评级边界正确`() {
        // 极好：RSRP -70(95) RSRQ -5(85) SINR 25(95) → (3800+2125+3325)/100 = 92
        assertEquals(
            SignalQualityEvaluator.Rating.EXCELLENT,
            SignalQualityEvaluator.evaluate(nr(rsrp = -70, rsrq = -5, sinr = 25)).rating
        )
        // 极弱：RSRP -120(10) RSRQ -20(20) SINR -5(10) → (400+500+350)/100 = 12
        assertEquals(
            SignalQualityEvaluator.Rating.WEAK,
            SignalQualityEvaluator.evaluate(nr(rsrp = -120, rsrq = -20, sinr = -5)).rating
        )
    }

    // ── 公开评分分发（供 UI 动态显色复用）──

    @Test
    fun `score 分发对不可用值返回 0`() {
        assertEquals(0, SignalQualityEvaluator.score(MetricKey.RSRP, unavailable))
        assertEquals(0, SignalQualityEvaluator.score(MetricKey.SINR, unavailable))
        assertEquals(80, SignalQualityEvaluator.score(MetricKey.RSRP, -91))
    }
}
