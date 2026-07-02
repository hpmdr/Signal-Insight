package cn.debubu.signalinsight.ui.cellular

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.data.cellular.MetricKey
import cn.debubu.signalinsight.data.cellular.NeighborCellTableModel
import cn.debubu.signalinsight.data.cellular.SignalData

/**
 * 单张 SIM 卡的内容页面 — 包含信号环、指标网格、邻小区列表。
 *
 * 此组件无状态，数据由父组件传入，满足 2 张卡复用同一个页面模板的需求。
 *
 * @param signalData     当前 SIM 的信号摘要数据
 * @param neighborCells  当前 SIM 的邻小区列表
 * @param onMetricClick  用户点击某个指标方格时的回调
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SimContentPage(
    signalData: SignalData,
    neighborCells: List<NeighborCellTableModel>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedContentStates: Map<MetricKey, SharedTransitionScope.SharedContentState>,
    onMetricClick: (MetricKey) -> Unit,
) {
    val context = LocalContext.current

    // 信号强度状态色 & 文字标签
    val statusColor = when {
        signalData.dbm == Int.MAX_VALUE -> MaterialTheme.colorScheme.outline
        signalData.dbm > -85 -> Color(0xFF386B28)
        signalData.dbm > -105 -> Color(0xFF6C5D00)
        else -> Color(0xFFBA1A1A)
    }
    val statusLabel = when {
        signalData.dbm == Int.MAX_VALUE -> ""
        signalData.dbm > -85 -> context.getString(R.string.signal_excellent)
        signalData.dbm > -105 -> context.getString(R.string.signal_fair)
        else -> context.getString(R.string.signal_poor)
    }

    val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp + 20.dp
    val bottomPadding = 20.dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 宽度 > 600dp 视为横屏/平板宽屏模式
        val isLandscape = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = topPadding, bottom = bottomPadding, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp)
        ) {
            if (isLandscape) {
                // ── 横屏：信号环 + 指标网格 左右双列 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(0.48f)) {
                        SignalRingCard(
                            signalData = signalData,
                            statusColor = statusColor,
                            statusLabel = statusLabel,
                            onClick = { onMetricClick(MetricKey.OVERVIEW) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            sharedContentStates = sharedContentStates,
                            ringSize = 110.dp,
                        )
                    }
                    Column(Modifier.weight(0.52f)) {
                        MetricGridCard(
                            signalData = signalData,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            sharedContentStates = sharedContentStates,
                            onMetricClick = onMetricClick
                        )
                    }
                }
            } else {
                // ── 竖屏：原有纵向布局 ──
                SignalRingCard(
                    signalData = signalData,
                    statusColor = statusColor,
                    statusLabel = statusLabel,
                    onClick = { onMetricClick(MetricKey.OVERVIEW) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedContentStates = sharedContentStates,
                )

                MetricGridCard(
                    signalData = signalData,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedContentStates = sharedContentStates,
                    onMetricClick = onMetricClick
                )
            }

            // ── 邻小区列表（横竖屏共用） ──
            NeighborCellsCard(neighborCells = neighborCells, is5gNetwork = signalData.networkType.contains("5G"))
        }
    }
}
