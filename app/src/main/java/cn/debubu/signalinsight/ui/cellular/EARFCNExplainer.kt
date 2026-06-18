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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R

@Composable
fun EarfcnExplainer(currentEarfcn: Int, onClose: () -> Unit, skipOuterPadding: Boolean = false) {
    MetricExplainerShell(
        icon = Icons.Default.CellTower,
        labelResId = R.string.metric_earfcn_label,
        fullResId = R.string.metric_earfcn_full,
        tipResId = R.string.metric_earfcn_tip,
        skipOuterPadding = skipOuterPadding,
        onClose = onClose
    ) {
        SectionCard(R.string.earfcn_explain_title, Icons.Default.Search) {
            Text(stringResource(R.string.earfcn_explain_basic), style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.metric_net_earfcn),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.earfcn_formula_title, Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.earfcn_formula_4g), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                }
                Text(stringResource(R.string.earfcn_formula_4g_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.earfcn_operator_title, Icons.Default.CellTower) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(R.string.earfcn_operator_mobile, R.string.earfcn_operator_unicom, R.string.earfcn_operator_telecom, R.string.earfcn_operator_broadcast)
                    .forEachIndexed { i, resId ->
                        Surface(color = if (i % 2 == 0) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else Color.Transparent, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(resId), modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.earfcn_assessment_title, Icons.Default.Star) {
            if (currentEarfcn == Int.MAX_VALUE) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("N/A", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.metric_not_available), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EARFCN $currentEarfcn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.earfcn_assessment_text), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
