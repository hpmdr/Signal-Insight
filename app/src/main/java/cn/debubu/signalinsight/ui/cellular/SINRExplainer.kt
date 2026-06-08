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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

/**
 * SINR（信噪比）详细科普弹窗内容
 */
@Composable
fun SinrExplainer(currentSinr: Int, onClose: () -> Unit) {
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
                    stringResource(R.string.metric_sinr_label),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.metric_sinr_full),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ====== Section 1: 基本概念 ======
        SectionCard(R.string.sinr_explain_title, Icons.Default.Search) {
            Text(
                stringResource(R.string.sinr_explain_basic),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // ====== Section 2: SINR 与下载速度对应表 ======
        SectionCard(R.string.sinr_range_title, Icons.Default.Speed) {
            SinrSpeedTable()
        }

        Spacer(Modifier.height(16.dp))

        // ====== Section 3: 影响 SINR 的因素 ======
        SectionCard(R.string.sinr_factor_title, Icons.Default.Info) {
            SinrFactors()
        }

        Spacer(Modifier.height(16.dp))

        // ====== Section 4: 当前 SINR 评估 ======
        SectionCard(R.string.sinr_assessment_title, Icons.Default.Star) {
            SinrAssessment(currentSinr)
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
                    stringResource(R.string.metric_sinr_tip),
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

// SectionCard 定义移至 ExplainerUtils.kt 共享使用

private data class SinrSpeedInfo(
    val labelResId: Int,
    val speedResId: Int,
    val color: Color
)

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
            Surface(
                color = row.color,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        stringResource(row.labelResId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(row.speedResId),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SinrFactors() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Icon(Icons.Default.Info, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.sinr_factor_location),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
        Row {
            Icon(Icons.Default.Info, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.sinr_factor_crowd),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
        Row {
            Icon(Icons.Default.Info, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.sinr_factor_handover),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SinrAssessment(currentSinr: Int) {
    val backgroundColor: Color
    val hintResId: Int

    when {
        currentSinr > 20 -> {
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            hintResId = R.string.sinr_assessment_excellent
        }
        currentSinr > 10 -> {
            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            hintResId = R.string.sinr_assessment_good
        }
        currentSinr > 5 -> {
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
            hintResId = R.string.sinr_assessment_fair
        }
        currentSinr > 0 -> {
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            hintResId = R.string.sinr_assessment_poor
        }
        else -> {
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            hintResId = R.string.sinr_assessment_weak
        }
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "${currentSinr} dB",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(hintResId),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
