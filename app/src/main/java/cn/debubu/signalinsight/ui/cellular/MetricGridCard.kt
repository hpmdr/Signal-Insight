package cn.debubu.signalinsight.ui.cellular

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.data.cellular.MetricKey
import cn.debubu.signalinsight.data.cellular.SignalData

/**
 * 单个指标格子的数据载体。
 */
internal data class Metric(
    val key: MetricKey,
    val label: String,
    val value: String,
    val unit: String,
)

/**
 * 指标网格卡片 — 2×4 排列 8 个信号参数（RSRP/RSRQ/SINR/RSSI/Band/PCI/EARFCN/TAC）。
 *
 * 每个格子通过 [SharedTransitionScope.sharedBounds] 绑定到对应的科普详情页，
 * 点击触发容器缩放动画展开。
 *
 * @param signalData            当前 SIM 的信号数据
 * @param modifier              Compose Modifier（遵循 API 规范）
 * @param sharedTransitionScope SharedTransition 作用域
 * @param animatedVisibilityScope 动画可见性作用域
 * @param sharedContentStates   每个 MetricKey → SharedContentState 的映射
 * @param onMetricClick         指标点击回调
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MetricGridCard(
    signalData: SignalData,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedContentStates: Map<MetricKey, SharedTransitionScope.SharedContentState>,
    onMetricClick: (MetricKey) -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            // 标题行
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.CellTower,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.cellular_serving_cell_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            val is5gNet = signalData.networkType.contains("5G")
            val na = stringResource(R.string.metric_no_data)

            fun displayValue(value: Int): String =
                if (value != Int.MAX_VALUE) value.toString() else na

            val rows = listOf(
                listOf(
                    Metric(MetricKey.RSRP, if (is5gNet) "SS-RSRP" else "RSRP", displayValue(signalData.rsrp), "dBm"),
                    Metric(MetricKey.RSRQ, if (is5gNet) "SS-RSRQ" else "RSRQ", displayValue(signalData.rsrq), "dB"),
                    Metric(MetricKey.SINR, if (is5gNet) "SS-SINR" else "SINR", displayValue(signalData.sinr), "dB"),
                    Metric(MetricKey.RSSI, "RSSI", displayValue(signalData.rssi), "dBm"),
                ),
                listOf(
                    Metric(MetricKey.Band, "Band", signalData.band.ifEmpty { na }, ""),
                    Metric(MetricKey.PCI, "PCI", displayValue(signalData.pci), ""),
                    Metric(MetricKey.EARFCN, "EARFCN", displayValue(signalData.earfcn), ""),
                    Metric(MetricKey.TAC, "TAC", displayValue(signalData.tac), ""),
                ),
            )

            rows.forEachIndexed { rowIndex, rowItems ->
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    rowItems.forEachIndexed { colIndex, item ->
                        val sharedState = sharedContentStates[item.key]!!
                        Box(
                            modifier = with(sharedTransitionScope) {
                                Modifier
                                    .weight(1f)
                                    .sharedBounds(
                                        sharedContentState = sharedState,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                        boundsTransform = { _, _ ->
                                            tween(420, easing = CubicBezierEasing(0.1f, 0.8f, 0.1f, 1.0f))
                                        },
                                    )
                                    .clickable { onMetricClick(item.key) }
                                    .padding(vertical = 10.dp)
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    ),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = item.value,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (item.unit.isNotEmpty()) {
                                    Text(
                                        text = item.unit,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        letterSpacing = 0.5.sp,
                                        style = TextStyle(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        ),
                                    )
                                } else {
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                        if (colIndex < 3) {
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
                if (rowIndex == 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }

            // 底部提示
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.cellular_tap_hint),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
