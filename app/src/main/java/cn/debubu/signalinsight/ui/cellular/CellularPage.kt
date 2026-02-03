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
import androidx.compose.material3.Divider
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
import kotlinx.coroutines.launch

data class MetricInfo(
    val label: String,
    val full: String,
    val desc: String,
    val impact: String,
    val tip: String? = null,
    val range: List<RangeStep>? = null
)

data class RangeStep(
    val label: String,
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

    var selectedMetric by remember { mutableStateOf<MetricInfo?>(null) }
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
                "RSRP",
                "参考信号接收功率 (Reference Signal Received Power)",
                "这是衡量 4G/5G 信号强度的最直接指标。它表示手机接收到基站信号的功率大小。",
                "数值越接近 0，信号越强。如果数值太低，你会感觉到网页打不开，通话断断续续。",
                range = listOf(
                    RangeStep("极弱", "<-110", Color(0xFFBA1A1A)),
                    RangeStep("一般", "-100~-80", Color(0xFF6C5D00)),
                    RangeStep("极佳", ">-80dBm", Color(0xFF386B28))
                )
            ),
            "RSRQ" to MetricInfo(
                "RSRQ",
                "参考信号接收质量 (Reference Signal Received Quality)",
                "反映信号的\"纯净度\"。即使信号很强，如果干扰太多，质量也会很差。",
                "数值越低，说明环境干扰越大。这通常发生在人群密集的车站或商场。",
                range = listOf(
                    RangeStep("差", "<-20dB", Color(0xFFBA1A1A)),
                    RangeStep("中", "-15~-10", Color(0xFF6C5D00)),
                    RangeStep("优", ">-10dB", Color(0xFF386B28))
                )
            ),
            "SINR" to MetricInfo(
                "SINR",
                "信噪比 (Signal to Interference plus Noise Ratio)",
                "有用信号与噪声的比值。就像在嘈杂的房间里听人说话，比值越高听得越清楚。",
                "这是决定你下载速度快慢的最核心指标。SINR 越高，5G 测速跑分就越高。",
                range = listOf(
                    RangeStep("卡顿", "<0dB", Color(0xFFBA1A1A)),
                    RangeStep("流畅", "0~20dB", Color(0xFF6C5D00)),
                    RangeStep("极速", ">20dB", Color(0xFF386B28))
                )
            ),
            "RSSI" to MetricInfo(
                "RSSI",
                "接收信号强度指示",
                "手机接收到的总功率，包括有用信号、干扰和背景噪声。",
                "在 4G/5G 系统中，其参考意义通常弱于 RSRP。"
            ),
            "Band" to MetricInfo(
                "Band",
                "工作频段 (Frequency Band)",
                "无线电通信的\"车道\"。不同的频段对应不同的无线电频率。",
                "低频段穿墙能力强但网速慢；高频段（如 n78）网速极快但穿透力弱。",
                tip = "n78 是目前最主流的 5G 高速频段。"
            ),
            "PCI" to MetricInfo(
                "PCI",
                "物理小区标识 (Physical Cell ID)",
                "用来区分不同基站的\"身份证号\"。",
                "当 PCI 发生变化时，说明你的手机完成了基站切换。",
                tip = "如果 PCI 频繁跳变，说明你处于两个基站交界处。"
            ),
            "EARFCN" to MetricInfo(
                "EARFCN",
                "中心频点",
                "代表基站工作的精确频率位置。通过此数值计算基站确切的下行频率。",
                impact = "不同的运营商拥有不同的频点范围。"
            ),
            "TAC" to MetricInfo(
                "TAC",
                "跟踪区域代码 (Tracking Area Code)",
                "运营商将基站划分成不同的\"片区\"进行管理。",
                "跨区域移动时 TAC 会更新。"
            )
        )
    }

    val statusColor = when {
        signalData.dbm > -85 -> Color(0xFF386B28)
        signalData.dbm > -105 -> Color(0xFF6C5D00)
        else -> Color(0xFFBA1A1A)
    }
    val statusLabel = when {
        signalData.dbm > -85 -> "极佳"
        signalData.dbm > -105 -> "一般"
        else -> "较差"
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
                        "该卡槽没有插卡，无法切换显示",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        Surface(
            onClick = { selectedMetric = metricsDatabase["RSRP"]; showSheet = true },
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
                            "质量: $statusLabel",
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
                        "当前注册小区详情",
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
                                        selectedMetric = metricsDatabase[item.label]; showSheet = true
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
                            "点击格块查看科普释义与参考标准",
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
                        "邻小区列表",
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
                    listOf("PCI", "频点", "频段", "RSRP", "RSRQ", "SINR").forEach {
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
                            "暂无邻小区数据",
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
                        if (index < neighborCells.size - 1) Divider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }

    if (showSheet && selectedMetric != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            MetricExplainerContent(selectedMetric!!) { showSheet = false }
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
                    info.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    info.full,
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
                        "它是做什么的？",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    info.desc,
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
                "对体验的影响",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Text(
            info.impact,
            Modifier.padding(top = 6.dp, start = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        info.range?.let { ranges ->
            Spacer(Modifier.height(24.dp))
            Text(
                "参考标准 (左差右优)",
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
                                    range.label,
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

        info.tip?.let { tip ->
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
                        tip,
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
            Text("理解了", fontWeight = FontWeight.Bold)
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
