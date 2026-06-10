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
import androidx.compose.material.icons.filled.Place
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
fun TacExplainer(currentTac: Int, onClose: () -> Unit) {
    MetricExplainerShell(
        icon = Icons.Default.CellTower,
        labelResId = R.string.metric_tac_label,
        fullResId = R.string.metric_tac_full,
        tipResId = R.string.metric_tac_tip,
        onClose = onClose
    ) {
        SectionCard(R.string.tac_explain_title, Icons.Default.Search) {
            Text(stringResource(R.string.tac_explain_basic), style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.tac_update_title, Icons.Default.Place) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(R.string.tac_update_move, R.string.tac_update_highway, R.string.tac_update_subway, R.string.tac_update_airport).forEach { resId ->
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
        SectionCard(R.string.tac_purpose_title, Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(R.string.tac_purpose_paging, R.string.tac_purpose_location, R.string.tac_purpose_balance).forEach { resId ->
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(resId), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.tac_assessment_title, Icons.Default.Star) {
            val (bg, hint) = if (currentTac > 0) {
                MaterialTheme.colorScheme.primaryContainer to R.string.tac_assessment_stable
            } else {
                MaterialTheme.colorScheme.tertiaryContainer to R.string.tac_assessment_changed
            }
            Surface(color = bg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TAC $currentTac", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(hint), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
