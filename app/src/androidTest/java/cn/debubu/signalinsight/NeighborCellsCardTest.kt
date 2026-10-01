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
}
