package cn.debubu.signalinsight.data.cellular

import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CellInfo 解析的安全兜底测试 —— 回归 [PR #2](https://github.com/hpmdr/Signal-Insight/pull/2)
 * 「修复闪退问题」所指的强制转型问题。
 *
 * 背景：fromCellInfo 原先对 cellSignalStrength / cellIdentity 直接使用强制转型或直接解引用。
 * 这些取值运行在 CellularRepository 的专用线程（CellularRepo-SlotN）上，一旦抛异常，
 * 线程会被终止、监听器静默失效，进程随之被杀。AOSP 虽把 getter 标注为 @NonNull，
 * 但 CellIdentity 字段非 final、CellInfoNr 存在不做 null 校验的 @hide setter，
 * 厂商 ROM 下发空对象时会真实触发。
 *
 * 这些用例依赖 [CellInfo] 各子类的 public 无参构造（经 build.gradle.kts 的
 * testOptions.unitTests.isReturnDefaultValues 放开桩方法），其 getter 一律返回 null，
 * 正好模拟「厂商返回了空对象」这一场景。
 *
 * 未覆盖：解析成功路径。桩对象无法注入真实测量值，happy path 需真机 / 仪器化测试覆盖。
 */
class CellInfoParsingTest {

    /**
     * 构造一个 CellInfo 桩对象。
     *
     * 各CellInfo 子类的 public 无参构造在 SDK android.jar 里是 package-private（@hide），
     * 因此只能反射调用。配合 build.gradle.kts 的 testOptions.unitTests.isReturnDefaultValues，
     * 这样得到的对象其 cellSignalStrength / cellIdentity getter 一律返回 null，
     * 正好模拟「厂商下发了空对象」这一场景。
     */
    private fun <T> stubCellInfo(cls: Class<T>): T =
        cls.getDeclaredConstructor().apply { isAccessible = true }.newInstance()

    private val lte: CellInfoLte = stubCellInfo(CellInfoLte::class.java)
    private val nr: CellInfoNr = stubCellInfo(CellInfoNr::class.java)
    private val wcdma: CellInfoWcdma = stubCellInfo(CellInfoWcdma::class.java)
    private val gsm: CellInfoGsm = stubCellInfo(CellInfoGsm::class.java)
    // CellInfoCdma 已被平台标记 deprecated，但它正是「未支持的网络制式」的代表，
    // 用它验证 else -> null 分支，不抑制会在编译期产生弃用告警。
    @Suppress("DEPRECATION")
    private val cdma: CellInfoCdma = stubCellInfo(CellInfoCdma::class.java)

    // ── 服务小区：解析失败必须返回 null，而不是抛异常或造占位数据 ──

    @Test
    fun `服务小区在缺少信号强度或小区标识时返回 null`() {
        assertNull("CellInfoLte", CellularSignalInfo.fromCellInfo(lte, 0, true))
        assertNull("CellInfoNr", CellularSignalInfo.fromCellInfo(nr, 0, true))
        assertNull("CellInfoWcdma", CellularSignalInfo.fromCellInfo(wcdma, 0, true))
        assertNull("CellInfoGsm", CellularSignalInfo.fromCellInfo(gsm, 0, true))
    }

    @Test
    fun `服务小区解析失败时不返回带运营商名的占位壳子`() {
        // 回归点：旧实现回落到 CellularSignalInfo(...) 默认实例，那会带着 operatorName
        // 和 networkType="Unknown" 推给 UI，看起来像「有数据但全无指标」。
        val result = CellularSignalInfo.fromCellInfo(nr, 1, true, operatorName = "中国联通")
        assertNull(result)
    }

    @Test
    fun `未覆盖的网络制式返回 null 由调用方兜底`() {
        assertNull("CellInfoCdma 不在支持列表内", CellularSignalInfo.fromCellInfo(cdma, 0, true))
    }

    // ── 邻小区：解析失败必须返回 null，否则会往表格里塞进一行虚构数据 ──

    @Test
    fun `邻小区在缺少信号强度或小区标识时返回 null`() {
        assertNull("CellInfoLte", NeighborCellInfo.fromCellInfo(lte))
        assertNull("CellInfoNr", NeighborCellInfo.fromCellInfo(nr))
    }

    @Test
    fun `未覆盖的网络制式的邻小区返回 null 而不是默认值`() {
        assertNull(NeighborCellInfo.fromCellInfo(cdma))
    }

    @Test
    fun `邻小区默认值是假数据而非无数据哨兵因此不可用作兜底`() {
        // 这正是不能用默认值兜底的原因：-120/-20 看起来像真实测量值，
        // 会被 toNeighborModels() 当作正常数据排序并渲染成一行。
        val fallback = NeighborCellInfo()
        assertEquals(-120, fallback.rsrp)
        assertEquals(-20, fallback.rsrq)
        assertEquals(-20, fallback.sinr)
        assertFalse(
            "默认值不应被误认为「无数据」",
            isUnavailable(fallback.rsrp)
        )
        // 对比：服务小区模型用的是真正的哨兵值
        assertTrue(isUnavailable(CellularSignalInfo().rsrp))
    }

    // ── 订阅 ID 有效性（供 getTelephonyManagerForSlot 短路使用） ──

    @Test
    fun `空卡槽的无效订阅 ID 不被判为可用`() {
        assertFalse("INVALID_SUBSCRIPTION_ID(-1)", isUsableSubscriptionId(-1))
        assertFalse("旧分支使用的 Int.MAX_VALUE 标记", isUsableSubscriptionId(Int.MAX_VALUE))
    }

    @Test
    fun `真实订阅 ID 被判为可用`() {
        assertTrue(isUsableSubscriptionId(1))
        assertTrue(isUsableSubscriptionId(3))
        assertTrue(isUsableSubscriptionId(255))
    }

    // ── NR 指标解析：SS 优先、CSI 兜底、都没有则无数据 ──

    @Test
    fun `NR 指标优先取 SS 值`() {
        assertEquals(-98, resolveNrMetric(ssValue = -98, csiValue = -105))
        assertEquals(-12, resolveNrMetric(ssValue = -12, csiValue = -18))
        assertEquals(9, resolveNrMetric(ssValue = 9, csiValue = 3))
    }

    @Test
    fun `NR 指标在 SS 不可用时回退到 CSI 值`() {
        assertEquals(-105, resolveNrMetric(ssValue = Int.MAX_VALUE, csiValue = -105))
        assertEquals(3, resolveNrMetric(ssValue = Int.MAX_VALUE, csiValue = 3))
    }

    @Test
    fun `NR 指标在 SS 与 CSI 都不可用时返回无数据哨兵`() {
        assertEquals(Int.MAX_VALUE, resolveNrMetric(Int.MAX_VALUE, Int.MAX_VALUE))
        assertTrue(isUnavailable(resolveNrMetric(Int.MAX_VALUE, Int.MAX_VALUE)))
    }

    @Test
    fun `NR 指标不会把 SS 的哨兵值当测量值返回`() {
        // 回归点：哨兵值 Int.MAX_VALUE 若被原样透传，UI 会显示 2147483647
        val v = resolveNrMetric(ssValue = Int.MAX_VALUE, csiValue = Int.MAX_VALUE)
        assertTrue("哨兵值必须被拦截为无数据", isUnavailable(v))
    }
}
