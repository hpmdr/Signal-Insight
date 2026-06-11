package cn.debubu.signalinsight.ui.cellular

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.data.cellular.MetricKey
import cn.debubu.signalinsight.data.cellular.NeighborCellTableModel
import cn.debubu.signalinsight.data.cellular.SignalData
import kotlinx.coroutines.launch

// =====================================================================
// 数据模型（供 CellularPage、SimContentPage 共用）
// =====================================================================

data class MetricInfo(
    val labelResId: Int,
    val fullResId: Int,
    val descResId: Int,
    val impactResId: Int,
    val tipResId: Int? = null,
    val range: List<RangeStep>? = null
)

data class RangeStep(
    val labelResId: Int,
    val text: String,
    val color: Color
)

data class Metric(
    val key: MetricKey,
    val label: String,
    val value: String,
    val unit: String
)

data class SimStatus(
    val id: Int,
    val name: String,
    val isReady: Boolean = true
)

// =====================================================================
// 主页面：HorizontalPager + NavigationBar 双卡切换
// =====================================================================

@Composable
fun CellularPage(
    modifier: Modifier = Modifier,
    viewModel: CellularViewModel,
    onOpenExplainer: (key: MetricKey, signalData: SignalData) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val activeSim by viewModel.activeSim.collectAsState()

    // ── 收集 StateFlow 为 Compose State（必须用 collectAsState，.value 是冷读不会触发重组） ──
    val sim1Data by viewModel.sim1SignalData.collectAsState()
    val sim1Neighbors by viewModel.sim1NeighborCells.collectAsState()
    val sim2Data by viewModel.sim2SignalData.collectAsState()
    val sim2Neighbors by viewModel.sim2NeighborCells.collectAsState()

    // ---- SIM 选项（从已收集的 state 派生，响应式更新） ----
    val noSimText = remember { context.getString(R.string.operator_no_sim) }
    val simOptions by remember {
        derivedStateOf {
            listOf(
                SimStatus(
                    1,
                    if (sim1Data.operatorName != "Unknown") sim1Data.operatorName else noSimText,
                    sim1Data.operatorName != "Unknown"
                ),
                SimStatus(
                    2,
                    if (sim2Data.operatorName != "Unknown") sim2Data.operatorName else noSimText,
                    sim2Data.operatorName != "Unknown"
                )
            )
        }
    }

    // ---- 指标元数据（静态数据，仅创建一次） ----
    val metricsDatabase = remember {
        mapOf(
            MetricKey.RSRP to MetricInfo(
                R.string.metric_rsrp_label, R.string.metric_rsrp_full,
                R.string.metric_rsrp_desc, R.string.metric_rsrp_impact,
                range = listOf(
                    RangeStep(R.string.range_rsrp_weak, "<-110", Color(0xFFBA1A1A)),
                    RangeStep(R.string.range_rsrp_fair, "-100~-80", Color(0xFF6C5D00)),
                    RangeStep(R.string.range_rsrp_excellent, ">-80dBm", Color(0xFF386B28))
                )
            ),
            MetricKey.RSRQ to MetricInfo(
                R.string.metric_rsrq_label, R.string.metric_rsrq_full,
                R.string.metric_rsrq_desc, R.string.metric_rsrq_impact,
                range = listOf(
                    RangeStep(R.string.range_rsrq_poor, "<-20dB", Color(0xFFBA1A1A)),
                    RangeStep(R.string.range_rsrq_medium, "-15~-10", Color(0xFF6C5D00)),
                    RangeStep(R.string.range_rsrq_good, ">-10dB", Color(0xFF386B28))
                )
            ),
            MetricKey.SINR to MetricInfo(
                R.string.metric_sinr_label, R.string.metric_sinr_full,
                R.string.metric_sinr_desc, R.string.metric_sinr_impact,
                range = listOf(
                    RangeStep(R.string.range_sinr_poor, "<0dB", Color(0xFFBA1A1A)),
                    RangeStep(R.string.range_sinr_fair, "0~20dB", Color(0xFF6C5D00)),
                    RangeStep(R.string.range_sinr_good, ">20dB", Color(0xFF386B28))
                )
            ),
            MetricKey.RSSI to MetricInfo(
                R.string.metric_rssi_label, R.string.metric_rssi_full,
                R.string.metric_rssi_desc, R.string.metric_rssi_impact
            ),
            MetricKey.Band to MetricInfo(
                R.string.metric_band_label, R.string.metric_band_full,
                R.string.metric_band_desc, R.string.metric_band_impact,
                tipResId = R.string.metric_band_tip
            ),
            MetricKey.PCI to MetricInfo(
                R.string.metric_pci_label, R.string.metric_pci_full,
                R.string.metric_pci_desc, R.string.metric_pci_impact,
                tipResId = R.string.metric_pci_tip
            ),
            MetricKey.EARFCN to MetricInfo(
                R.string.metric_earfcn_label, R.string.metric_earfcn_full,
                R.string.metric_earfcn_desc, R.string.metric_earfcn_impact
            ),
            MetricKey.TAC to MetricInfo(
                R.string.metric_tac_label, R.string.metric_tac_full,
                R.string.metric_tac_desc, R.string.metric_tac_impact
            )
        )
    }

    // ---- Pager 状态（当前页面索引 = activeSim - 1） ----
    val pagerState = rememberPagerState(
        initialPage = activeSim - 1,
        pageCount = { 2 }
    )

    // 滑动同步：用户滑页 → 通知 ViewModel 切换 SIM
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState.currentPage) {
        val simId = pagerState.currentPage + 1
        if (simId != activeSim) {
            viewModel.switchSim(simId)
        }
    }

    // 外部同步：ViewModel activeSim 变化（如代码调用 switchSim）→ 滑动 Pager
    LaunchedEffect(activeSim) {
        val targetPage = activeSim - 1
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // ---- 布局 ----
    Column(modifier = modifier.fillMaxSize()) {

        // 内容区域 — HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val simId = page + 1
            when (simId) {
                1 -> SimContentPage(
                    signalData = sim1Data,
                    neighborCells = sim1Neighbors,
                    metricsDatabase = metricsDatabase,
                    onMetricClick = { key -> onOpenExplainer(key, sim1Data) }
                )
                2 -> SimContentPage(
                    signalData = sim2Data,
                    neighborCells = sim2Neighbors,
                    metricsDatabase = metricsDatabase,
                    onMetricClick = { key -> onOpenExplainer(key, sim2Data) }
                )
            }
        }

        // 底部导航栏 — 文字 + 底部滑动指示条（方案 D）
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            var containerWidth by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            val tabCount = simOptions.size.coerceAtLeast(1)

            // 指示条：宽度为屏幕宽度的 1/4，居中于每个 tab
            val tabWidth = if (containerWidth > 0.dp) containerWidth / tabCount else 0.dp
            val barWidth = if (containerWidth > 0.dp) containerWidth / 4 else 0.dp
            val targetOffset = tabWidth * pagerState.currentPage + (tabWidth - barWidth) / 2
            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "IndicatorSlide"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .onGloballyPositioned { coords ->
                        containerWidth = with(density) { coords.size.width.toDp() }
                    }
            ) {
                // 文字按钮
                Row(modifier = Modifier.fillMaxSize()) {
                    simOptions.forEachIndexed { index, sim ->
                        val isSelected = pagerState.currentPage == index
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "TabTextColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sim.name,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }

                // 底部短横线指示条（居中于每个 tab，弹簧动画滑动）
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset, y = (-8).dp)
                        .width(barWidth)
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                        )
                )
            }
        }
    }

}

