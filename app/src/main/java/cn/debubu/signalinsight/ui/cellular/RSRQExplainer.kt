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
 * RSRQ（参考信号接收质量）详细科普弹窗内容
 */
@Composable
fun RsrqExplainer(currentRsrq: Int, onClose: () -> Unit) {
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
                    stringResource(R.string.metric_rsrq_label),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.metric_rsrq_full),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ====== Section 1: 基本概念 ======
        SectionCard(R.string.rsrq_explain_title, Icons.Default.Search) {
            Text(
                stringResource(R.string.rsrq_explain_basic),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // ====== Section 2: RSRQ 质量分级表 ======
        SectionCard(R.string.rsrq_range_title, Icons.Default.SignalCellularAlt) {
            RsrqRangeTable()
        }

        Spacer(Modifier.height(16.dp))

        // ====== Section 3: RSRP 与 RSRQ 联合判断 ======
        SectionCard(R.string.rsrq_joint_title, Icons.Default.Info) {
            RsrqJointJudgment()
        }

        Spacer(Modifier.height(16.dp))

        // ====== Section 4: 当前信号质量评估 ======
        SectionCard(R.string.rsrq_assessment_title, Icons.Default.Star) {
            RsrqAssessment(currentRsrq)
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
                    stringResource(R.string.metric_rsrq_tip),
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

private data class RsrqRangeInfo(
    val labelResId: Int,
    val descResId: Int,
    val color: Color
)

@Composable
private fun RsrqRangeTable() {
    val ranges = listOf(
        RsrqRangeInfo(R.string.rsrq_range_good, R.string.rsrq_range_good_desc, MaterialTheme.colorScheme.primaryContainer),
        RsrqRangeInfo(R.string.rsrq_range_fair, R.string.rsrq_range_fair_desc, MaterialTheme.colorScheme.tertiaryContainer),
        RsrqRangeInfo(R.string.rsrq_range_poor, R.string.rsrq_range_poor_desc, MaterialTheme.colorScheme.errorContainer)
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ranges.forEach { range ->
            Surface(
                color = range.color,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        stringResource(range.labelResId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(range.descResId),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private data class JointInfo(
    val titleResId: Int,
    val descResId: Int,
    val color: Color
)

@Composable
private fun RsrqJointJudgment() {
    val items = listOf(
        JointInfo(R.string.rsrq_joint_good_both, R.string.rsrq_joint_good_both_desc, MaterialTheme.colorScheme.primaryContainer),
        JointInfo(R.string.rsrq_joint_strong_noise, R.string.rsrq_joint_strong_noise_desc, MaterialTheme.colorScheme.tertiaryContainer),
        JointInfo(R.string.rsrq_joint_weak_both, R.string.rsrq_joint_weak_both_desc, MaterialTheme.colorScheme.errorContainer)
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Surface(
                color = item.color,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        stringResource(item.titleResId),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(item.descResId),
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
private fun RsrqAssessment(currentRsrq: Int) {
    val backgroundColor: Color
    val hintResId: Int

    when {
        currentRsrq > -10 -> {
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            hintResId = R.string.rsrq_assessment_good
        }
        currentRsrq > -15 -> {
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
            hintResId = R.string.rsrq_assessment_fair
        }
        else -> {
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            hintResId = R.string.rsrq_assessment_poor
        }
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "${currentRsrq} dB",
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
