package cn.debubu.signalinsight

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.debubu.signalinsight.data.cellular.NeighborCellTableModel
import cn.debubu.signalinsight.ui.cellular.NeighborCellsCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 邻小区卡片的确定性 UI 测试（批次 A1 的验收手段）。
 *
 * 背景：`CellInfo.UNAVAILABLE` 的常量值就是 `Int.MAX_VALUE`(2147483647)，
 * 而本项目也用 `Int.MAX_VALUE` 表示「无数据」。因此渲染层必须显式判空，
 * 否则：
 *   - 屏幕直接显示 `2147483647`；
 *   - `rsrpColor(Int.MAX_VALUE)` 命中 `> -85` → 被染成「优秀」绿色；
 *   - 列表按 RSRP 降序 → 未上报的邻区被置顶。
 *
 * 该场景依赖 Modem 是否上报邻区，靠真机难以稳定复现，故用合成数据固化为自动化断言。
 */
@RunWith(AndroidJUnit4::class)
class NeighborCellsCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val noData: String
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(R.string.metric_no_data)

    private fun render(cells: List<NeighborCellTableModel>, is5g: Boolean = true) {
        composeRule.setContent {
            MaterialTheme {
                NeighborCellsCard(neighborCells = cells, is5gNetwork = is5g)
            }
        }
    }

    @Test
    fun 全部字段不可用时显示占位文案且绝不出现哨兵数值() {
        render(
            listOf(
                NeighborCellTableModel(
                    pci = Int.MAX_VALUE,
                    earfcn = Int.MAX_VALUE,
                    band = "",
                    rsrp = Int.MAX_VALUE,
                    rsrq = Int.MAX_VALUE,
                    sinr = Int.MAX_VALUE,
                )
            )
        )

        composeRule.onNodeWithText(Int.MAX_VALUE.toString()).assertDoesNotExist()

        // pci / earfcn / rsrp / rsrq / sinr 五列应全部回退为占位文案
        assertEquals(
            5,
            composeRule.onAllNodesWithText(noData).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun 混入不可用邻区时其他邻区数值仍正常显示() {
        render(
            listOf(
                NeighborCellTableModel(
                    pci = Int.MAX_VALUE,
                    earfcn = Int.MAX_VALUE,
                    band = "",
                    rsrp = Int.MAX_VALUE,
                    rsrq = Int.MAX_VALUE,
                    sinr = Int.MAX_VALUE,
                ),
                NeighborCellTableModel(
                    pci = 733,
                    earfcn = 504990,
                    band = "n41",
                    rsrp = -91,
                    rsrq = -10,
                    sinr = 17,
                ),
            )
        )

        composeRule.onNodeWithText(Int.MAX_VALUE.toString()).assertDoesNotExist()
        composeRule.onNodeWithText("-91").assertIsDisplayed()
        composeRule.onNodeWithText("733").assertIsDisplayed()
        composeRule.onNodeWithText("n41").assertIsDisplayed()
        // 仅第一行产生 5 个占位
        assertEquals(
            5,
            composeRule.onAllNodesWithText(noData).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun 可用邻区不产生任何占位文案() {
        render(
            listOf(
                NeighborCellTableModel(
                    pci = 733,
                    earfcn = 504990,
                    band = "n41",
                    rsrp = -91,
                    rsrq = -10,
                    sinr = 17,
                )
            )
        )

        assertEquals(
            0,
            composeRule.onAllNodesWithText(noData).fetchSemanticsNodes().size,
        )
    }

    /**
     * 回归防线：厂商对**邻区**普遍不上报 rssnr（真机实测为 CellInfo.UNAVAILABLE），
     * 此时 SINR 列必须显示「无数据」——这里用合成数据把该分支锁死，
     * 避免以后有人「顺手」把它改成显示数字或 0。
     */
    @Test
    fun 邻区SINR不可用时显示无数据而其余列正常() {
        render(
            listOf(
                NeighborCellTableModel(
                    pci = 400,
                    earfcn = 3590,
                    band = "B8",
                    rsrp = -82,
                    rsrq = -9,
                    sinr = Int.MAX_VALUE,
                )
            )
        )

        // 只有 SINR 一列回退为占位，其余五列正常显示
        assertEquals(
            1,
            composeRule.onAllNodesWithText(noData).fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithText("-82").assertIsDisplayed()
        composeRule.onNodeWithText("-9").assertIsDisplayed()
        composeRule.onNodeWithText("400").assertIsDisplayed()
        composeRule.onNodeWithText("3590").assertIsDisplayed()
        composeRule.onNodeWithText("B8").assertIsDisplayed()
    }

    /** 邻区 SINR 有真实值（含 0 这种「有效但极差」的值）时必须照常显示数字 */
    @Test
    fun 邻区SINR有值时显示数值() {
        render(
            listOf(
                NeighborCellTableModel(
                    pci = 380,
                    earfcn = 1650,
                    band = "B3",
                    rsrp = -104,
                    rsrq = -5,
                    sinr = 0,
                ),
                NeighborCellTableModel(
                    pci = 282,
                    earfcn = 2452,
                    band = "B5",
                    rsrp = -102,
                    rsrq = -9,
                    sinr = 12,
                ),
            )
        )

        assertEquals(
            0,
            composeRule.onAllNodesWithText(noData).fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithText("12").assertIsDisplayed()
    }
}
