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
import androidx.compose.material.icons.filled.Speed
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

@Composable
fun SinrExplainer(currentSinr: Int, onClose: () -> Unit, skipOuterPadding: Boolean = false) {
    MetricExplainerShell(
        icon = Icons.Default.CellTower,
        labelResId = R.string.metric_sinr_label,
        fullResId = R.string.metric_sinr_full,
        tipResId = R.string.metric_sinr_tip,
        skipOuterPadding = skipOuterPadding,
        onClose = onClose
    ) {
        SectionCard(R.string.sinr_explain_title, Icons.Default.Search) {
            Text(stringResource(R.string.sinr_explain_basic), style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.metric_net_sinr),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.sinr_range_title, Icons.Default.Speed) { SinrSpeedTable() }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.sinr_factor_title, Icons.Default.Info) { SinrFactors() }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.sinr_assessment_title, Icons.Default.Star) { SinrAssessment(currentSinr) }
    }
}

private data class SinrSpeedInfo(val labelResId: Int, val speedResId: Int, val color: Color)

@Composable
private fun SinrSpeedTable() {
    val rows = listOf(
        SinrSpeedInfo(R.string.sinr_range_excellent, R.string.sinr_range_excellent_speed, MaterialTheme.colorScheme.primaryContainer),
        SinrSpeedInfo(R.string.sinr_range_good, R.string.sinr_range_good_speed, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
        SinrSpeedInfo(R.string.sinr_range_fair, R.string.sinr_range_fair_speed, MaterialTheme.colorScheme.tertiaryContainer),
        SinrSpeedInfo(R.string.sinr_range_poor, R.string.sinr_range_poor_speed, MaterialTheme.colorScheme.errorContainer),
        SinrSpeedInfo(R.string.sinr_range_weak, R.string.sinr_range_weak_speed, MaterialTheme.colorScheme.errorContainer)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Surface(color = row.color, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(stringResource(row.labelResId), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(row.speedResId), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun SinrFactors() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(R.string.sinr_factor_location, R.string.sinr_factor_crowd, R.string.sinr_factor_handover).forEach { resId ->
            Row {
                Icon(Icons.Default.Info, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(resId), style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun SinrAssessment(currentSinr: Int) {
    if (currentSinr == Int.MAX_VALUE) {
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
    val (bg, hint) = when {
        currentSinr > 20 -> MaterialTheme.colorScheme.primaryContainer to R.string.sinr_assessment_excellent
        currentSinr > 10 -> MaterialTheme.colorScheme.primaryContainer to R.string.sinr_assessment_good
        currentSinr > 5 -> MaterialTheme.colorScheme.surfaceVariant to R.string.sinr_assessment_fair
        currentSinr > 0 -> MaterialTheme.colorScheme.errorContainer to R.string.sinr_assessment_poor
        else -> MaterialTheme.colorScheme.errorContainer to R.string.sinr_assessment_weak
    }
    Surface(color = bg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${currentSinr} dB", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}
