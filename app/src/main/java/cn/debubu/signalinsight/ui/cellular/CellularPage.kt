package cn.debubu.signalinsight.ui.cellular

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import cn.debubu.signalinsight.R
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellularPage(
    modifier: Modifier = Modifier,
    viewModel: CellularViewModel
) {
    val context = LocalContext.current
    val signalData by viewModel.currentSignalData
    val neighborCells by viewModel.neighborCells

    var selectedMetricKey by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val simOptions = remember {
        val sim1Name = viewModel.getSimOperatorName(1)
        val sim2Name = viewModel.getSimOperatorName(2)
        listOf(
            SimStatus(1, sim1Name, viewModel.isSimInserted(1)),
            SimStatus(2, sim2Name, viewModel.isSimInserted(2))
        )
    }

    val metricsDatabase = remember {
        mapOf(
            "RSRP" to MetricInfo(
                R.string.metric_rsrp_label,
                R.string.metric_rsrp_full,
                R.string.metric_rsrp_desc,
                R.string.metric_rsrp_impact,
                range = listOf(
                    RangeStep(R.string.range_rsrp_weak, "<-110", Color(0xFFBA1A1A)),
                    RangeStep(R.string.range_rsrp_fair, "-100~-80", Color(0xFF6C5D00)),
                    RangeStep(R.string.range_rsrp_excellent, ">-80dBm", Color(0xFF386B28))
                )
            ),
            "RSRQ" to MetricInfo(
                R.string.metric_rsrq_label,
                R.string.metric_rsrq_full,
                R.string.metric_rsrq_desc,
                R.string.metric_rsrq_impact,
                range = listOf(
                    RangeStep(R.string.range_rsrq_poor, "<-20dB", Color(0xFFBA1A1A)),
                    RangeStep(R.string.range_rsrq_medium, "-15~-10", Color(0xFF6C5D00)),
                    RangeStep(R.string.range_rsrq_good, ">-10dB", Color(0xFF386B28))
                )
            ),
            "SINR" to MetricInfo(
                R.string.metric_sinr_label,
                R.string.metric_sinr_full,
                R.string.metric_sinr_desc,
                R.string.metric_sinr_impact,
                range = listOf(
                    RangeStep(R.string.range_sinr_poor, "<0dB", Color(0xFFBA1A1A)),
                    RangeStep(R.string.range_sinr_fair, "0~20dB", Color(0xFF6C5D00)),
                    RangeStep(R.string.range_sinr_good, ">20dB", Color(0xFF386B28))
                )
            ),
            "RSSI" to MetricInfo(
                R.string.metric_rssi_label,
                R.string.metric_rssi_full,
                R.string.metric_rssi_desc,
                R.string.metric_rssi_impact
            ),
            "Band" to MetricInfo(
                R.string.metric_band_label,
                R.string.metric_band_full,
                R.string.metric_band_desc,
                R.string.metric_band_impact,
                tipResId = R.string.metric_band_tip
            ),
            "PCI" to MetricInfo(
                R.string.metric_pci_label,
                R.string.metric_pci_full,
                R.string.metric_pci_desc,
                R.string.metric_pci_impact,
                tipResId = R.string.metric_pci_tip
            ),
            "EARFCN" to MetricInfo(
                R.string.metric_earfcn_label,
                R.string.metric_earfcn_full,
                R.string.metric_earfcn_desc,
                R.string.metric_earfcn_impact
            ),
            "TAC" to MetricInfo(
                R.string.metric_tac_label,
                R.string.metric_tac_full,
                R.string.metric_tac_desc,
                R.string.metric_tac_impact
            )
        )
    }

    val statusColor = when {
        signalData.dbm > -85 -> Color(0xFF386B28)
        signalData.dbm > -105 -> Color(0xFF6C5D00)
        else -> Color(0xFFBA1A1A)
    }
    val statusLabel = when {
        signalData.dbm > -85 -> context.getString(R.string.signal_excellent)
        signalData.dbm > -105 -> context.getString(R.string.signal_fair)
        else -> context.getString(R.string.signal_poor)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CompactSimSwitcher(
            activeSimId = viewModel.activeSim,
            simOptions = simOptions,
            onSimSelected = { sim ->
                if (sim.isReady) {
                    viewModel.switchSim(sim.id)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.cellular_no_sim_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        Surface(
            onClick = { selectedMetricKey = "RSRP"; showSheet = true },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp), contentAlignment = Alignment.Center
            ) {
                val animatedRsrp by animateIntAsState(
                    targetValue = signalData.dbm,
                    animationSpec = tween(1000)
                )
                val progress by animateFloatAsState(
                    targetValue = signalData.progress,
                    animationSpec = tween(1500)
                )

                Canvas(modifier = Modifier.size(180.dp)) {
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        style = Stroke(width = 45f)
                    )
                    drawArc(
                        color = statusColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 45f, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$animatedRsrp",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            letterSpacing = (-2).sp
                        )
                        Text(
                            "dBm",
                            Modifier.padding(bottom = 10.dp, start = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
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

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CellTower,
                        null,
                        Modifier.size(14.dp),
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

                val row1 = listOf(
                    Metric("RSRP", signalData.rsrp.toString(), "dBm"),
                    Metric("RSRQ", signalData.rsrq.toString(), "dB"),
                    Metric("SINR", signalData.sinr.toString(), "dB"),
                    Metric("RSSI", signalData.rssi.toString(), "dBm")
                )
                val row2 = listOf(
                    Metric("Band", signalData.band, ""),
                    Metric("PCI", signalData.pci.toString(), ""),
                    Metric("EARFCN", signalData.earfcn.toString(), ""),
                    Metric("TAC", signalData.tac.toString(), "")
                )

                val grid = listOf(row1, row2)

                grid.forEachIndexed { rowIndex, rowItems ->
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        rowItems.forEachIndexed { colIndex, item ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedMetricKey = item.label; showSheet = true
                                    }
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

                    if (rowIndex == 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }

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
                            null,
                            Modifier.size(10.dp),
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

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Radar,
                        null,
                        Modifier.size(14.dp),
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(vertical = 8.dp)
                ) {
                    listOf(stringResource(R.string.column_pci), stringResource(R.string.column_frequency), stringResource(R.string.column_band), "RSRP", "RSRQ", "SINR").forEach {
                        Text(
                            it,
                            Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
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
                            Text(
                                cell.pci.toString(),
                                Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                cell.earfcn.toString(),
                                Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                cell.band,
                                Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                cell.rsrp.toString(),
                                Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Black,
                                color = if (cell.rsrp > -85) Color(0xFF386B28) else if (cell.rsrp > -105) Color(
                                    0xFF6C5D00
                                ) else Color(0xFFBA1A1A)
                            )
                            Text(
                                cell.rsrq.toString(),
                                Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                cell.sinr.toString(),
                                Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (index < neighborCells.size - 1) HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }

    if (showSheet && selectedMetricKey != null) {
        val key = selectedMetricKey!!
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

@Composable
fun MetricExplainerContent(info: MetricInfo, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    null,
                    Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    stringResource(info.labelResId),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(info.fullResId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.explainer_what_is),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    stringResource(info.descResId),
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Star,
                null,
                Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.explainer_impact),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Text(
            stringResource(info.impactResId),
            Modifier.padding(top = 6.dp, start = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        info.range?.let { ranges ->
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.explainer_standard),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.outline
            )
            Surface(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        ranges.forEach { range ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    stringResource(range.labelResId),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = range.color
                                )
                                Text(
                                    range.text,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    ) {
                        ranges.forEachIndexed { i, r ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                color = r.color.copy(alpha = 0.3f),
                                shape = when (i) {
                                    0 -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                                    ranges.size - 1 -> RoundedCornerShape(
                                        topEnd = 4.dp,
                                        bottomEnd = 4.dp
                                    )

                                    else -> RoundedCornerShape(0.dp)
                                }
                            ) {}
                            if (i < ranges.size - 1) Spacer(Modifier.width(2.dp))
                        }
                    }
                }
            }
        }

        info.tipResId?.let { tipResId ->
            Spacer(Modifier.height(20.dp))
            Surface(
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, null, Modifier.size(14.dp), tint = Color(0xFF1976D2))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(tipResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(stringResource(R.string.explainer_understand), fontWeight = FontWeight.Bold)
        }
    }
}

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

    val shakeOffset = remember { Animatable(0f) }

    val activeIndex = simOptions.indexOfFirst { it.id == activeSimId }.coerceAtLeast(0)
    val targetOffset = if (containerWidth > 0.dp) {
        (containerWidth / simOptions.size) * activeIndex
    } else 0.dp

    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
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
