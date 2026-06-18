package cn.debubu.signalinsight.ui.cellular

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R

/**
 * RSRP（参考信号接收功率）详细科普 — 使用 MetricExplainerShell 统一外壳
 */
@Composable
fun RsrpExplainer(currentRsrp: Int, onClose: () -> Unit, skipOuterPadding: Boolean = false) {
    MetricExplainerShell(
        icon = Icons.Default.CellTower,
        labelResId = R.string.metric_rsrp_label,
        fullResId = R.string.metric_rsrp_full,
        tipResId = R.string.metric_rsrp_tip,
        skipOuterPadding = skipOuterPadding,
        onClose = onClose
    ) {
        SectionCard(R.string.rsrp_explain_title, Icons.Default.Search) {
            Text(
                stringResource(R.string.rsrp_explain_basic),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.metric_net_rsrp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(16.dp))

        SectionCard(R.string.rsrp_range_title, Icons.Default.SignalCellularAlt) {
            RsrpRangeTable()
        }
        Spacer(Modifier.height(16.dp))

        SectionCard(R.string.rsrp_factor_title, Icons.Default.Info) {
            RsrpFactors()
        }
        Spacer(Modifier.height(16.dp))

        SectionCard(R.string.rsrp_assessment_title, Icons.Default.Star) {
            RsrpAssessment(currentRsrp)
        }
    }
}

/** 信号分级展示 */
private data class RsrpRangeInfo(
    val labelResId: Int, val descResId: Int, val color: Color
)

@Composable
private fun RsrpRangeTable() {
    val ranges = listOf(
        RsrpRangeInfo(R.string.rsrp_range_excellent, R.string.rsrp_range_excellent_desc, MaterialTheme.colorScheme.primaryContainer),
        RsrpRangeInfo(R.string.rsrp_range_good, R.string.rsrp_range_good_desc, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
        RsrpRangeInfo(R.string.rsrp_range_fair, R.string.rsrp_range_fair_desc, MaterialTheme.colorScheme.tertiaryContainer),
        RsrpRangeInfo(R.string.rsrp_range_poor, R.string.rsrp_range_poor_desc, MaterialTheme.colorScheme.errorContainer),
        RsrpRangeInfo(R.string.rsrp_range_weak, R.string.rsrp_range_weak_desc, MaterialTheme.colorScheme.errorContainer)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ranges.forEach { range ->
            Surface(
                color = range.color,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(stringResource(range.labelResId), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(range.descResId), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun RsrpFactors() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(R.string.rsrp_factor_distance, R.string.rsrp_factor_obstacle, R.string.rsrp_factor_weather, R.string.rsrp_factor_load)
            .forEach { resId ->
                Row {
                    Icon(Icons.Default.Info, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(resId), style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                }
            }
    }
}

@Composable
private fun RsrpAssessment(currentRsrp: Int) {
    if (currentRsrp == Int.MAX_VALUE) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("N/A", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.metric_not_available), style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
        return
    }
    val (bgColor, hintResId) = when {
        currentRsrp > -85 -> MaterialTheme.colorScheme.primaryContainer to R.string.rsrp_assessment_excellent
        currentRsrp > -95 -> MaterialTheme.colorScheme.primaryContainer to R.string.rsrp_assessment_good
        currentRsrp > -105 -> MaterialTheme.colorScheme.surfaceVariant to R.string.rsrp_assessment_fair
        currentRsrp > -115 -> MaterialTheme.colorScheme.errorContainer to R.string.rsrp_assessment_poor
        else -> MaterialTheme.colorScheme.errorContainer to R.string.rsrp_assessment_weak
    }
    Surface(color = bgColor, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${currentRsrp} dBm", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(hintResId), style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}
