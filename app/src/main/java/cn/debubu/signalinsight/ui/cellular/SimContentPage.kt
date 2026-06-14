package cn.debubu.signalinsight.ui.cellular

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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

/**
 * 单张 SIM 卡的内容页面 — 包含信号环、指标网格、邻小区列表。
 *
 * 此组件无状态，数据由父组件传入，满足 2 张卡复用同一个页面模板的需求。
 *
 * @param signalData     当前 SIM 的信号摘要数据
 * @param neighborCells  当前 SIM 的邻小区列表
 * @param onMetricClick  用户点击某个指标方格时的回调
 */
@Composable
fun SimContentPage(
    signalData: SignalData,
    neighborCells: List<NeighborCellTableModel>,
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

    // rememberSaveable 确保从详情页返回后恢复滚动位置
    val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    // 动态获取状态栏 + 系统导航栏高度
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp + 20.dp
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp + 20.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = topPadding, bottom = bottomPadding, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ===== 信号环 =====
        SignalRingCard(
            signalData = signalData,
            statusColor = statusColor,
            statusLabel = statusLabel,
            onClick = { onMetricClick(MetricKey.OVERVIEW) }
        )

        // ===== 指标网格 =====
        MetricGridCard(
            signalData = signalData,
            onMetricClick = onMetricClick
        )

        // ===== 邻小区列表 =====
        NeighborCellsCard(neighborCells = neighborCells, is5gNetwork = signalData.networkType.contains("5G"))
    }
}

// =====================================================================
// 内部组件
// =====================================================================

/**
 * 信号环卡片 — 显示环形进度 + dBm 数值 + 运营商/网络类型 + 质量标签
 */
@Composable
private fun SignalRingCard(
    signalData: SignalData,
    statusColor: Color,
    statusLabel: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            val animatedRsrp by animateIntAsState(
                targetValue = if (signalData.dbm != Int.MAX_VALUE) signalData.dbm else -120,
                animationSpec = tween(1000)
            )
            val progress by animateFloatAsState(
                targetValue = signalData.progress,
                animationSpec = tween(1500)
            )

            // 环形图
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 45f
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = statusColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 中央文字
            val isDbmValid = signalData.dbm != Int.MAX_VALUE
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (isDbmValid) "$animatedRsrp" else "N/A",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        letterSpacing = (-2).sp
                    )
                    if (isDbmValid) {
                        Text(
                            "dBm",
                            Modifier.padding(bottom = 10.dp, start = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    text = "${signalData.operatorName} ${signalData.networkType}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.alpha(0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        stringResource(R.string.cellular_signal_quality, statusLabel),
                        Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

/**
 * 指标网格卡片 — 8 个信号参数（RSRP/RSRQ/SINR/RSSI/Band/PCI/EARFCN/TAC），点击弹出科普
 */
@Composable
private fun MetricGridCard(
    signalData: SignalData,
    onMetricClick: (MetricKey) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            // 标题行
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CellTower,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.cellular_serving_cell_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // 两行指标 — 根据网络类型调整标签和可用性
            val is5gNet = signalData.networkType.contains("5G")
            val na = "N/A"

            fun displayValue(value: Int): String = if (value != Int.MAX_VALUE) value.toString() else na

            val rows = listOf(
                listOf(
                    Metric(MetricKey.RSRP, if (is5gNet) "SS-RSRP" else "RSRP", displayValue(signalData.rsrp), "dBm"),
                    Metric(MetricKey.RSRQ, if (is5gNet) "SS-RSRQ" else "RSRQ", displayValue(signalData.rsrq), "dB"),
                    Metric(MetricKey.SINR, if (is5gNet) "SS-SINR" else "SINR", displayValue(signalData.sinr), "dB"),
                    Metric(MetricKey.RSSI, "RSSI", displayValue(signalData.rssi), "dBm")
                ),
                listOf(
                    Metric(MetricKey.Band, "Band", signalData.band.ifEmpty { na }, ""),
                    Metric(MetricKey.PCI, "PCI", displayValue(signalData.pci), ""),
                    Metric(MetricKey.EARFCN, "EARFCN", displayValue(signalData.earfcn), ""),
                    Metric(MetricKey.TAC, "TAC", displayValue(signalData.tac), "")
                )
            )

            rows.forEachIndexed { rowIndex, rowItems ->
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    rowItems.forEachIndexed { colIndex, item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onMetricClick(item.key) }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.value,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (item.unit.isNotEmpty()) {
                                Text(
                                    text = item.unit,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    letterSpacing = 0.5.sp,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            } else {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        // 列分隔线
                        if (colIndex < 3) {
                            VerticalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                // 行分隔线
                if (rowIndex == 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // 底部提示
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.cellular_tap_hint),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * 邻小区列表卡片 — 表头 + 邻区行
 */
@Composable
private fun NeighborCellsCard(neighborCells: List<NeighborCellTableModel>, is5gNetwork: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            // 标题行
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Radar,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.cellular_neighbor_title),
                    Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // 表头 — 5G 时 RSRP/RSRQ/SINR 加 SS- 前缀
            val headers = listOf(
                stringResource(R.string.column_pci),
                stringResource(R.string.column_frequency),
                stringResource(R.string.column_band),
                if (is5gNetwork) "SS-RSRP" else "RSRP",
                if (is5gNetwork) "SS-RSRQ" else "RSRQ",
                if (is5gNetwork) "SS-SINR" else "SINR"
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(vertical = 8.dp)
            ) {
                headers.forEach { header ->
                    Text(
                        header,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // 列表行
            if (neighborCells.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.cellular_neighbor_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                neighborCells.forEachIndexed { index, cell ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cell.pci.toString(), Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(cell.earfcn.toString(), Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(cell.band, Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline)
                        Text(cell.rsrp.toString(), Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black,
                            color = when {
                                cell.rsrp > -85 -> Color(0xFF386B28)
                                cell.rsrp > -105 -> Color(0xFF6C5D00)
                                else -> Color(0xFFBA1A1A)
                            })
                        Text(cell.rsrq.toString(), Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall)
                        Text(cell.sinr.let { if (it != Int.MAX_VALUE) it.toString() else "N/A" }, Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (index < neighborCells.size - 1) {
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}
