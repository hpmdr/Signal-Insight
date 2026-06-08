package cn.debubu.signalinsight.ui.cellular

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch

// =====================================================================
// 数据模型（供 CellularPage、SimContentPage、CompactSimSwitcher 共用）
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellularPage(
    modifier: Modifier = Modifier,
    viewModel: CellularViewModel
) {
    val context = LocalContext.current

    // ---- 弹窗状态 ----
    var selectedMetricKey by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ---- SIM 选项（响应式派生，数据到达后自动更新） ----
    val simOptions by remember {
        derivedStateOf {
            listOf(
                SimStatus(1, viewModel.getSimOperatorName(1), viewModel.isSimInserted(1)),
                SimStatus(2, viewModel.getSimOperatorName(2), viewModel.isSimInserted(2))
            )
        }
    }

    // ---- 指标元数据（静态数据，仅创建一次） ----
    val metricsDatabase = remember {
        mapOf(
            "RSRP" to MetricInfo(
                R.string.metric_rsrp_label, R.string.metric_rsrp_full,
                R.string.metric_rsrp_desc, R.string.metric_rsrp_impact,
                range = listOf(
                    RangeStep(R.string.range_rsrp_weak, "<-110", Color(0xFFBA1A1A)),
                    RangeStep(R.string.range_rsrp_fair, "-100~-80", Color(0xFF6C5D00)),
                    RangeStep(R.string.range_rsrp_excellent, ">-80dBm", Color(0xFF386B28))
                )
            ),
            "RSRQ" to MetricInfo(
                R.string.metric_rsrq_label, R.string.metric_rsrq_full,
                R.string.metric_rsrq_desc, R.string.metric_rsrq_impact,
                range = listOf(
                    RangeStep(R.string.range_rsrq_poor, "<-20dB", Color(0xFFBA1A1A)),
                    RangeStep(R.string.range_rsrq_medium, "-15~-10", Color(0xFF6C5D00)),
                    RangeStep(R.string.range_rsrq_good, ">-10dB", Color(0xFF386B28))
                )
            ),
            "SINR" to MetricInfo(
                R.string.metric_sinr_label, R.string.metric_sinr_full,
                R.string.metric_sinr_desc, R.string.metric_sinr_impact,
                range = listOf(
                    RangeStep(R.string.range_sinr_poor, "<0dB", Color(0xFFBA1A1A)),
                    RangeStep(R.string.range_sinr_fair, "0~20dB", Color(0xFF6C5D00)),
                    RangeStep(R.string.range_sinr_good, ">20dB", Color(0xFF386B28))
                )
            ),
            "RSSI" to MetricInfo(
                R.string.metric_rssi_label, R.string.metric_rssi_full,
                R.string.metric_rssi_desc, R.string.metric_rssi_impact
            ),
            "Band" to MetricInfo(
                R.string.metric_band_label, R.string.metric_band_full,
                R.string.metric_band_desc, R.string.metric_band_impact,
                tipResId = R.string.metric_band_tip
            ),
            "PCI" to MetricInfo(
                R.string.metric_pci_label, R.string.metric_pci_full,
                R.string.metric_pci_desc, R.string.metric_pci_impact,
                tipResId = R.string.metric_pci_tip
            ),
            "EARFCN" to MetricInfo(
                R.string.metric_earfcn_label, R.string.metric_earfcn_full,
                R.string.metric_earfcn_desc, R.string.metric_earfcn_impact
            ),
            "TAC" to MetricInfo(
                R.string.metric_tac_label, R.string.metric_tac_full,
                R.string.metric_tac_desc, R.string.metric_tac_impact
            )
        )
    }

    // ---- Pager 状态（当前页面索引 = activeSim - 1） ----
    val pagerState = rememberPagerState(
        initialPage = viewModel.activeSim - 1,
        pageCount = { 2 }
    )

    // 滑动同步：用户滑页 → 通知 ViewModel 切换 SIM
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState.currentPage) {
        val simId = pagerState.currentPage + 1
        if (simId != viewModel.activeSim) {
            viewModel.switchSim(simId)
        }
    }

    // 外部同步：ViewModel activeSim 变化（如代码调用 switchSim）→ 滑动 Pager
    LaunchedEffect(viewModel.activeSim) {
        val targetPage = viewModel.activeSim - 1
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
                    signalData = viewModel.sim1SignalData.value,
                    neighborCells = viewModel.sim1NeighborCells.value,
                    metricsDatabase = metricsDatabase,
                    onMetricClick = { key -> selectedMetricKey = key; showSheet = true }
                )
                2 -> SimContentPage(
                    signalData = viewModel.sim2SignalData.value,
                    neighborCells = viewModel.sim2NeighborCells.value,
                    metricsDatabase = metricsDatabase,
                    onMetricClick = { key -> selectedMetricKey = key; showSheet = true }
                )
            }
        }

        // 底部导航栏 — 药丸式分段按钮（方案 C）
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            // 药丸容器
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp)
                    .height(34.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    simOptions.forEachIndexed { index, sim ->
                        val isSelected = pagerState.currentPage == index

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
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
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- 底部科普弹窗（所有页面共用） ----
    if (showSheet && selectedMetricKey != null) {
        val key = selectedMetricKey!!
        val signalData = if (viewModel.activeSim == 1) viewModel.sim1SignalData.value
                         else viewModel.sim2SignalData.value

        ModalBottomSheet(
            onDismissRequest = { showSheet = false; selectedMetricKey = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            when (key) {
                "Band" -> BandExplainer(
                    currentBand = signalData.band,
                    onClose = { showSheet = false; selectedMetricKey = null }
                )
                "RSRP" -> RsrpExplainer(
                    currentRsrp = signalData.dbm,
                    onClose = { showSheet = false; selectedMetricKey = null }
                )
                "RSRQ" -> RsrqExplainer(
                    currentRsrq = signalData.rsrq,
                    onClose = { showSheet = false; selectedMetricKey = null }
                )
                "SINR" -> SinrExplainer(
                    currentSinr = signalData.sinr,
                    onClose = { showSheet = false; selectedMetricKey = null }
                )
                "RSSI" -> RssiExplainer(
                    currentRssi = signalData.rssi,
                    onClose = { showSheet = false; selectedMetricKey = null }
                )
                "PCI" -> PciExplainer(
                    currentPci = signalData.pci,
                    onClose = { showSheet = false; selectedMetricKey = null }
                )
                "EARFCN" -> EarfcnExplainer(
                    currentEarfcn = signalData.earfcn,
                    onClose = { showSheet = false; selectedMetricKey = null }
                )
                "TAC" -> TacExplainer(
                    currentTac = signalData.tac,
                    onClose = { showSheet = false; selectedMetricKey = null }
                )
            }
        }
    }
}

// =====================================================================
// 保留 CompactSimSwitcher（旧版顶部切换器，当前未使用，但兼容保留）
// =====================================================================

@Composable
fun CompactSimSwitcher(
    activeSimId: Int,
    simOptions: List<SimStatus> = listOf(
        SimStatus(1, "中国移动"),
        SimStatus(2, "未插卡", isReady = false)
    ),
    onSimSelected: (SimStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val shakeOffset = remember { androidx.compose.animation.core.Animatable(0f) }

    val activeIndex = simOptions.indexOfFirst { it.id == activeSimId }.coerceAtLeast(0)
    val targetOffset = if (containerWidth > 0.dp) {
        (containerWidth / simOptions.size) * activeIndex
    } else 0.dp

    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "SliderOffset"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onGloballyPositioned { containerWidth = with(density) { it.size.width.toDp() } },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.padding(4.dp)) {
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .fillMaxHeight()
                    .fillMaxWidth(1f / simOptions.size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                simOptions.forEach { sim ->
                    val isSelected = activeSimId == sim.id
                    val contentAlpha = if (sim.isReady) 1f else 0.4f
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "ContentColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .offset(x = if (!sim.isReady && shakeOffset.value != 0f) shakeOffset.value.dp else 0.dp)
                            .clip(CircleShape)
                            .alpha(contentAlpha)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (sim.isReady) {
                                    onSimSelected(sim)
                                } else {
                                    scope.launch {
                                        repeat(2) {
                                            shakeOffset.animateTo(4f, animationSpec = tween(50))
                                            shakeOffset.animateTo(-4f, animationSpec = tween(50))
                                        }
                                        shakeOffset.animateTo(0f, animationSpec = tween(50))
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = sim.name,
                                color = contentColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected && sim.isReady) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
