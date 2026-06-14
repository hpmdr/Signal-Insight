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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.res.painterResource
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
    onOpenExplainer: (key: MetricKey) -> Unit = { }
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

    // ---- 布局 —— 只渲染内容，NavigationBar 已移至 Scaffold.bottomBar ----
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        val simId = page + 1
        when (simId) {
            1 -> SimContentPage(
                signalData = sim1Data,
                neighborCells = sim1Neighbors,
                onMetricClick = { key -> onOpenExplainer(key) }
            )
            2 -> SimContentPage(
                signalData = sim2Data,
                neighborCells = sim2Neighbors,
                onMetricClick = { key -> onOpenExplainer(key) }
            )
        }
    }

}

