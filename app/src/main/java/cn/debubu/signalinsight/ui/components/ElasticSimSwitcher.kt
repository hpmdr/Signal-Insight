package cn.debubu.signalinsight.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R

/**
 * 弹性水滴双卡槽切换器 (v3.2)
 *
 * 基于非对称双边界弹簧算法实现"水滴拉伸、流动并吸附"的果冻动效。
 * Left/Right 两个边界独立绑定 spring，切换时领先沿先走、落后沿滞后，
 * 宽度 animatedRight - animatedLeft 动态拉伸，模拟液滴惯性形变。
 *
 * @param selectedSim 当前选中卡槽 (1 或 2)
 * @param sim1Name 卡槽 1 显示名称
 * @param sim2Name 卡槽 2 显示名称
 * @param onSimSelected 卡槽切换回调
 * @param modifier 布局修饰符
 * @param activeColor 水滴滑块填充色
 * @param containerColor 切换器底盘背景色
 */
@Composable
fun ElasticSimSwitcher(
    selectedSim: Int,
    sim1Name: String,
    sim2Name: String,
    onSimSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
) {
    val containerHeight = 48.dp
    val outerPadding = 4.dp
    val innerCornerRadius = 20.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .padding(outerPadding)
    ) {
        val containerWidth = maxWidth
        val itemWidth = containerWidth / 2

        // 双边界目标值
        val targetLeft = if (selectedSim == 1) 0.dp else itemWidth
        val targetRight = if (selectedSim == 1) itemWidth else (containerWidth - (outerPadding * 2))

        // 弹性物理引擎：中度回弹 + 低刚度 → 液滴拉伸效果
        val springSpec = spring<Dp>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )

        // 左右边界独立绑定弹簧
        val animatedLeft by animateDpAsState(
            targetValue = targetLeft,
            animationSpec = springSpec,
            label = "LiquidLeftBound"
        )
        val animatedRight by animateDpAsState(
            targetValue = targetRight,
            animationSpec = springSpec,
            label = "LiquidRightBound"
        )

        // 当前帧水滴宽度（形变发生器）
        val currentPillWidth = animatedRight - animatedLeft

        Box(Modifier.fillMaxSize()) {
            // 底层：物理弹簧水滴滑块
            Box(
                modifier = Modifier
                    .offset(x = animatedLeft)
                    .width(currentPillWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(innerCornerRadius))
                    .background(activeColor)
            )

            // 上层：交互区域
            Row(modifier = Modifier.fillMaxSize()) {

                // ── 卡槽 1 ──
                val isSim1Active = selectedSim == 1
                val sim1Color = if (isSim1Active) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSimSelected(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_sim_1),
                            contentDescription = "SIM 1",
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(sim1Color)
                        )
                        Text(
                            text = sim1Name,
                            fontSize = 14.sp,
                            fontWeight = if (isSim1Active) FontWeight.Bold else FontWeight.Normal,
                            color = sim1Color,
                            maxLines = 1
                        )
                    }
                }

                // ── 卡槽 2 ──
                val isSim2Active = selectedSim == 2
                val sim2Color = if (isSim2Active) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSimSelected(2) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_sim_2),
                            contentDescription = "SIM 2",
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(sim2Color)
                        )
                        Text(
                            text = sim2Name,
                            fontSize = 14.sp,
                            fontWeight = if (isSim2Active) FontWeight.Bold else FontWeight.Normal,
                            color = sim2Color,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
