package cn.debubu.signalinsight.data.cellular

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「无数据」展示规则的单元测试。
 *
 * 背景：Android 用 `CellInfo.UNAVAILABLE`(常量值 `Int.MAX_VALUE` = 2147483647)表示
 * 「系统未上报」，而本项目也用 `Int.MAX_VALUE` 表示「无数据」。两者同值，因此渲染层
 * 必须显式判等，否则会把哨兵值当成真实测量值显示出来（历史上邻小区 RSRP/RSRQ 就踩过这个坑，
 * 且因 `Int.MAX_VALUE > -85` 为真而被染成「优秀」绿色）。
 *
 * 这些规则原先散落在 Compose 渲染代码里，无法在无设备环境下测试；
 * 现下沉到 [isUnavailable] / [displayMetric]，使核心判定可被纯逻辑单测覆盖。
 */
class MetricDisplayTest {

    // ─── isUnavailable ────────────────────────────────────────

    @Test
    fun `Int_MAX_VALUE 视为不可用`() {
        assertTrue(isUnavailable(Int.MAX_VALUE))
    }

    @Test
    fun `真实测量值不视为不可用`() {
        assertFalse(isUnavailable(-140))
        assertFalse(isUnavailable(-85))
        assertFalse(isUnavailable(0))
        assertFalse(isUnavailable(17))
        assertFalse(isUnavailable(Int.MIN_VALUE))
        assertFalse(isUnavailable(Int.MAX_VALUE - 1))
    }

    // ─── displayMetric ────────────────────────────────────────

    @Test
    fun `不可用值显示为占位文案而不是哨兵数字`() {
        assertEquals("N/A", displayMetric(Int.MAX_VALUE, "N/A"))
        assertEquals("无数据", displayMetric(Int.MAX_VALUE, "无数据"))
    }

    @Test
    fun `可用值原样显示`() {
        assertEquals("-82", displayMetric(-82, "无数据"))
        assertEquals("-9", displayMetric(-9, "无数据"))
        assertEquals("0", displayMetric(0, "无数据"))
        assertEquals("400", displayMetric(400, "无数据"))
    }

    /** 关键回归：哨兵值绝不能被格式化成 "2147483647" */
    @Test
    fun `哨兵值永远不会被格式化成2147483647`() {
        val shown = displayMetric(Int.MAX_VALUE, "无数据")
        assertFalse(shown.contains("2147483647"))
        assertEquals("无数据", shown)
    }

    // ─── 评分侧的同源约定 ──────────────────────────────────────

    @Test
    fun `评分函数对不可用值返回0分且不会误判为好`() {
        listOf(MetricKey.RSRP, MetricKey.RSRQ, MetricKey.SINR, MetricKey.RSSI).forEach { key ->
            assertEquals("$key 不可用应为 0 分", 0, SignalQualityEvaluator.score(key, Int.MAX_VALUE))
        }
    }
}
