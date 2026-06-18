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
import androidx.compose.material.icons.filled.Warning
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
fun PciExplainer(currentPci: Int, onClose: () -> Unit, skipOuterPadding: Boolean = false) {
    MetricExplainerShell(
        icon = Icons.Default.CellTower,
        labelResId = R.string.metric_pci_label,
        fullResId = R.string.metric_pci_full,
        tipResId = R.string.metric_pci_tip,
        skipOuterPadding = skipOuterPadding,
        onClose = onClose
    ) {
        SectionCard(R.string.pci_explain_title, Icons.Default.Search) {
            Text(stringResource(R.string.pci_explain_basic), style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.metric_net_pci),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.pci_handover_title, Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    R.string.pci_handover_normal to R.string.pci_handover_normal_desc,
                    R.string.pci_handover_frequent to R.string.pci_handover_frequent_desc
                ).forEachIndexed { i, (title, desc) ->
                    Surface(
                        color = if (i == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
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
        SectionCard(R.string.pci_mod3_title, Icons.Default.Warning) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.pci_mod3_desc), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionCard(R.string.pci_assessment_title, Icons.Default.Star) {
            if (currentPci == Int.MAX_VALUE) {
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
                val (bg, hint) = if (currentPci > 0) {
                    MaterialTheme.colorScheme.primaryContainer to R.string.pci_assessment_stable
                } else {
                    MaterialTheme.colorScheme.surfaceVariant to R.string.pci_assessment_changed
                }
                Surface(color = bg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$currentPci", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
