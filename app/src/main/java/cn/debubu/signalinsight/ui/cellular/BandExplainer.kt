package cn.debubu.signalinsight.ui.cellular

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R

/**
 * 频段 (Band) 详细科普弹窗内容
 */
@Composable
fun BandExplainer(currentBand: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 48.dp)
    ) {
        // ====== 标题区 ======
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.CellTower,
                    null,
                    Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    stringResource(R.string.metric_band_label),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.metric_band_full),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ====== Section 1: 基本概念 ======
        SectionCard(R.string.band_explain_title, Icons.Default.Search) {
            Text(
                stringResource(R.string.band_explain_basic),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // ====== Section 2: 当前连接频段 ======
        SectionCard(R.string.band_current_title, Icons.Default.SignalCellularAlt) {
            CurrentBandDisplay(currentBand)
        }

        Spacer(Modifier.height(16.dp))

        // ====== Section 3: 频段特性对比 ======
        SectionCard(R.string.band_comparison_title, Icons.Default.Info) {
            BandComparisonTable()
        }

        Spacer(Modifier.height(16.dp))

        // ====== Section 4: 运营商频段分布 ======
        SectionCard(R.string.band_operator_title, Icons.Default.CellTower) {
            OperatorBandTable()
        }

        Spacer(Modifier.height(16.dp))

        // ====== Section 5: 频段评价 ======
        SectionCard(R.string.band_assessment, Icons.Default.Star) {
            BandAssessment(currentBand)
        }

        Spacer(Modifier.height(16.dp))

        // ====== Tip ======
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.metric_band_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // ====== "理解了"按钮 ======
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

// ========== 辅助组件 ==========

    // ====== 共享工具组件已在 ExplainerUtils.kt 中定义 ======
    // SectionCard(titleResId, icon) { content }

@Composable
private fun CurrentBandDisplay(currentBand: String) {
    val isLowBand = currentBand.any { it.isDigit() } && (
            currentBand.startsWith("B5") || currentBand.startsWith("B8") ||
                    currentBand.startsWith("B28") || currentBand.startsWith("n5") ||
                    currentBand.startsWith("n8") || currentBand.startsWith("n28")
            )
    val isHighBand = currentBand.startsWith("n78") || currentBand.startsWith("n79")
    val isMidBand = !isLowBand && !isHighBand

    val typeResId = when {
        isHighBand -> R.string.band_type_high
        isMidBand -> R.string.band_type_mid
        else -> R.string.band_type_low
    }

    val backgroundColor = when {
        isHighBand -> MaterialTheme.colorScheme.primaryContainer
        isMidBand -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                currentBand.ifEmpty { "--" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(typeResId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun BandComparisonTable() {
    val bands = listOf(
        Triple(R.string.band_low_freq, R.string.band_low_desc,
            listOf(R.string.band_wide, R.string.band_strong, R.string.band_slow)),
        Triple(R.string.band_mid_freq, R.string.band_mid_desc,
            listOf(R.string.band_medium, R.string.band_medium, R.string.band_medium)),
        Triple(R.string.band_high_freq, R.string.band_high_desc,
            listOf(R.string.band_narrow, R.string.band_weak, R.string.band_fast))
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 表头
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.band_type_name), Modifier.weight(1.2f),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
            Text(stringResource(R.string.band_feature_coverage), Modifier.weight(0.8f),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline, fontSize = 10.sp,
                textAlign = TextAlign.Center)
            Text(stringResource(R.string.band_feature_penetration), Modifier.weight(0.8f),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline, fontSize = 10.sp,
                textAlign = TextAlign.Center)
            Text(stringResource(R.string.band_feature_speed), Modifier.weight(0.8f),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline, fontSize = 10.sp,
                textAlign = TextAlign.Center)
        }
        HorizontalDivider()
        // 数据行
        bands.forEachIndexed { index, (name, desc, props) ->
            val rowColor = when (index) {
                0 -> MaterialTheme.colorScheme.primaryContainer
                1 -> MaterialTheme.colorScheme.tertiaryContainer
                2 -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowColor, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(name), Modifier.weight(1.2f),
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    props.forEach { prop ->
                        val isGood = prop == R.string.band_wide || prop == R.string.band_strong || prop == R.string.band_fast
                        Text(stringResource(prop), Modifier.weight(0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(desc),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun OperatorBandTable() {
    data class OpInfo(val nameResId: Int, val band5gResId: Int, val band4gResId: Int, val featureResId: Int)

    val operators = listOf(
        OpInfo(R.string.operator_china_mobile, R.string.band_china_mobile_5g, R.string.band_china_mobile_4g, R.string.band_china_mobile_feature),
        OpInfo(R.string.operator_china_unicom, R.string.band_china_unicom_5g, R.string.band_china_unicom_4g, R.string.band_china_unicom_feature),
        OpInfo(R.string.operator_china_telecom, R.string.band_china_telecom_5g, R.string.band_china_telecom_4g, R.string.band_china_telecom_feature),
        OpInfo(R.string.operator_china_broadcast, R.string.band_china_broadcast_5g, R.string.band_china_broadcast_4g, R.string.band_china_broadcast_feature)
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        operators.forEachIndexed { index, op ->
            val bgColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            else Color.Transparent
            Surface(
                color = bgColor,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(stringResource(op.nameResId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("5G:", style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline)
                            Text(stringResource(op.band5gResId),
                                style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("4G:", style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline)
                            Text(stringResource(op.band4gResId),
                                style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(op.featureResId),
                        style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BandAssessment(currentBand: String) {
    val isHighBand = currentBand.startsWith("n78") || currentBand.startsWith("n79")
    val isMidBand = !isHighBand && currentBand.any { it.isDigit() } && (
            currentBand.startsWith("n41") || currentBand.startsWith("B1") ||
                    currentBand.startsWith("B3") || currentBand.startsWith("B39") ||
                    currentBand.startsWith("B40")
            )
    val isLowBand = !isHighBand && !isMidBand && currentBand.any { it.isDigit() }

    val backgroundColor: Color
    val hintResId: Int

    when {
        isHighBand -> {
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            hintResId = R.string.band_excellent_hint
        }
        isMidBand -> {
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
            hintResId = R.string.band_good_hint
        }
        isLowBand -> {
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            hintResId = R.string.band_normal_hint
        }
        else -> {
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            hintResId = R.string.operator_unknown
        }
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (currentBand.any { it.isDigit() }) {
                stringResource(hintResId, currentBand)
            } else {
                stringResource(R.string.operator_unknown)
            },
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
