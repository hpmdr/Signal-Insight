package cn.debubu.signalinsight.ui.cellular

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.data.cellular.NeighborCellTableModel

/**
 * 邻小区列表卡片 — 表头 + 邻区行，带空状态提示。
 *
 * 5G 网络时 RSRP/RSRQ/SINR 列头自动加 SS- 前缀。
 *
 * @param neighborCells 邻小区数据列表（空列表显示占位文案）
 * @param is5gNetwork   当前是否为 5G 网络
 * @param modifier      Compose Modifier（遵循 API 规范）
 */
@Composable
internal fun NeighborCellsCard(
    neighborCells: List<NeighborCellTableModel>,
    is5gNetwork: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            // 标题行
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Radar,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.cellular_neighbor_title),
                    Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            // 表头
            val headers = listOf(
                stringResource(R.string.column_pci),
                stringResource(R.string.column_frequency),
                stringResource(R.string.column_band),
                if (is5gNetwork) "SS-RSRP" else "RSRP",
                if (is5gNetwork) "SS-RSRQ" else "RSRQ",
                if (is5gNetwork) "SS-SINR" else "SINR",
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(vertical = 8.dp),
            ) {
                headers.forEach { header ->
                    Text(
                        header,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            if (neighborCells.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.cellular_neighbor_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                neighborCells.forEachIndexed { index, cell ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TableCell(cell.pci.toString(), bold = true)
                        TableCell(cell.earfcn.toString(), fontSize = 9, singleLine = true)
                        TableCell(cell.band, bold = true, color = MaterialTheme.colorScheme.outline)
                        TableCell(cell.rsrp.toString(), bold = true, color = rsrpColor(cell.rsrp))
                        TableCell(cell.rsrq.toString())
                        TableCell(
                            if (cell.sinr != Int.MAX_VALUE) cell.sinr.toString()
                            else stringResource(R.string.metric_no_data),
                        )
                    }
                    if (index < neighborCells.size - 1) {
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}

/** 邻小区表格单元格（必须在 RowScope 内使用，自动平分列宽） */
@Composable
private fun RowScope.TableCell(
    text: String,
    bold: Boolean = false,
    singleLine: Boolean = false,
    fontSize: Int = 12,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        fontSize = fontSize.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color,
        maxLines = if (singleLine) 1 else 99,
        overflow = TextOverflow.Ellipsis,
    )
}

/** RSRP 值 → 颜色映射 */
private fun rsrpColor(rsrp: Int): Color = when {
    rsrp > -85 -> Color(0xFF386B28)
    rsrp > -105 -> Color(0xFF6C5D00)
    else -> Color(0xFFBA1A1A)
}
