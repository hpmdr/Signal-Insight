package cn.debubu.signalinsight.ui.cellular

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * RSRQ（参考信号接收质量）详细科普
 */
@Composable
fun RsrqExplainer(currentRsrq: Int, onClose: () -> Unit) {
    MetricExplainerShell(
        icon = Icons.Default.CellTower,
        labelResId = R.string.metric_rsrq_label,
        fullResId = R.string.metric_rsrq_full,
        tipResId = R.string.metric_rsrq_tip,
        onClose = onClose
    ) {
        SectionCard(R.string.rsrq_explain_title, Icons.Default.Search) {
            Text(stringResource(R.string.rsrq_explain_basic), style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.rsrq_range_title, Icons.Default.SignalCellularAlt) { RsrqRangeTable() }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.rsrq_joint_title, Icons.Default.Info) { RsrqJointJudgment() }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.rsrq_assessment_title, Icons.Default.Star) { RsrqAssessment(currentRsrq) }
    }
}

private data class RsrqRangeInfo(val labelResId: Int, val descResId: Int, val color: Color)

@Composable
private fun RsrqRangeTable() {
    val ranges = listOf(
        RsrqRangeInfo(R.string.rsrq_range_good, R.string.rsrq_range_good_desc, MaterialTheme.colorScheme.primaryContainer),
        RsrqRangeInfo(R.string.rsrq_range_fair, R.string.rsrq_range_fair_desc, MaterialTheme.colorScheme.tertiaryContainer),
        RsrqRangeInfo(R.string.rsrq_range_poor, R.string.rsrq_range_poor_desc, MaterialTheme.colorScheme.errorContainer)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ranges.forEach { range ->
            Surface(color = range.color, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(stringResource(range.labelResId), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(range.descResId), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                }
            }
        }
    }
}

private data class JointInfo(val titleResId: Int, val descResId: Int, val color: Color)

@Composable
private fun RsrqJointJudgment() {
    val items = listOf(
        JointInfo(R.string.rsrq_joint_good_both, R.string.rsrq_joint_good_both_desc, MaterialTheme.colorScheme.primaryContainer),
        JointInfo(R.string.rsrq_joint_strong_noise, R.string.rsrq_joint_strong_noise_desc, MaterialTheme.colorScheme.tertiaryContainer),
        JointInfo(R.string.rsrq_joint_weak_both, R.string.rsrq_joint_weak_both_desc, MaterialTheme.colorScheme.errorContainer)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Surface(color = item.color, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(stringResource(item.titleResId), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(item.descResId), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun RsrqAssessment(currentRsrq: Int) {
    val (bg, hint) = when {
        currentRsrq > -10 -> MaterialTheme.colorScheme.primaryContainer to R.string.rsrq_assessment_good
        currentRsrq > -15 -> MaterialTheme.colorScheme.tertiaryContainer to R.string.rsrq_assessment_fair
        else -> MaterialTheme.colorScheme.errorContainer to R.string.rsrq_assessment_poor
    }
    Surface(color = bg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${currentRsrq} dB", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}
