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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R

@Composable
fun RssiExplainer(currentRssi: Int, onClose: () -> Unit, skipOuterPadding: Boolean = false) {
    MetricExplainerShell(
        icon = Icons.Default.CellTower,
        labelResId = R.string.metric_rssi_label,
        fullResId = R.string.metric_rssi_full,
        tipResId = R.string.metric_rssi_tip,
        skipOuterPadding = skipOuterPadding,
        onClose = onClose
    ) {
        SectionCard(R.string.rssi_explain_title, Icons.Default.Search) {
            Text(stringResource(R.string.rssi_explain_basic), style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.metric_net_rssi),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.rssi_vs_rsrp_title, Icons.AutoMirrored.Filled.CompareArrows) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    R.string.rssi_vs_rsrp_large to R.string.rssi_vs_rsrp_large_desc,
                    R.string.rssi_vs_rsrp_small to R.string.rssi_vs_rsrp_small_desc
                ).forEachIndexed { i, (title, desc) ->
                    Surface(
                        color = if (i == 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.rssi_when_use_title, Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(R.string.rssi_when_use_wifi, R.string.rssi_when_use_2g, R.string.rssi_when_use_proximity).forEach { resId ->
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(resId), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.rssi_assessment_title, Icons.Default.Star) {
            if (currentRssi == Int.MAX_VALUE) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.metric_no_data), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.metric_not_available), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                val (bg, hint) = when {
                    currentRssi > -70 -> MaterialTheme.colorScheme.primaryContainer to R.string.rssi_assessment_good
                    currentRssi > -90 -> MaterialTheme.colorScheme.surfaceVariant to R.string.rssi_assessment_fair
                    else -> MaterialTheme.colorScheme.errorContainer to R.string.rssi_assessment_poor
                }
                Surface(color = bg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$currentRssi dBm", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
