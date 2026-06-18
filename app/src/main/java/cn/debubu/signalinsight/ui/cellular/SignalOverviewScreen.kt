package cn.debubu.signalinsight.ui.cellular

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.data.cellular.SignalData
import cn.debubu.signalinsight.data.cellular.SignalQualityEvaluator

/**
 * 信号综合分析页 — 点击信号环时打开。
 *
 * 展示综合评分、自然语言诊断、各参数指标速览。
 * 与 8 个单点详情页（RsrpExplainer 等）定位不同：
 * 综合页回答"信号怎么样"，单点页回答"这个参数是什么"。
 */
@Composable
fun SignalOverviewScreen(signalData: SignalData, onClose: () -> Unit, skipOuterPadding: Boolean = false) {
    val evaluation = SignalQualityEvaluator.evaluate(signalData)

    val modifier = if (skipOuterPadding) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp + 20.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp + 20.dp, start = 24.dp, end = 24.dp)
    }

    Column(modifier = modifier) {
        // ── 综合评分环 ──
        ScoreRing(evaluation = evaluation)

        Spacer(Modifier.height(24.dp))

        // ── 诊断说明 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info, null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.overview_diagnosis_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    evaluation.diagnosis,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
                if (evaluation.weaknessParam != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "💡 ${evaluation.weaknessHint}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 关键指标速览 ──
        Text(
            stringResource(R.string.overview_metrics_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(12.dp))

        evaluation.paramScores.forEach { param ->
            MetricBriefCard(
                param = param,
                onClick = { /* TODO: 默认先不做跳转，仅展示 */ }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(32.dp))

        // ── "知道了" 按钮 ──
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

@Composable
private fun ScoreRing(evaluation: SignalQualityEvaluator.Evaluation) {
    // ── ARC 动画：从 0 增长到实际分数（1500ms） ──
    val arcProgress = remember { Animatable(0f) }
    LaunchedEffect(evaluation.totalScore) {
        arcProgress.animateTo(
            targetValue = evaluation.totalScore / 100f,
            animationSpec = tween(1500)
        )
    }

    // ── 数字跳动：0 → 实际分数（500ms 计数 + 弹性弹跳） ──
    val displayScore by animateIntAsState(
        targetValue = evaluation.totalScore,
        animationSpec = tween(500)
    )
    var bounceTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(evaluation.totalScore) {
        kotlinx.coroutines.delay(500)
        bounceTrigger++
    }
    val textScale by animateFloatAsState(
        targetValue = if (bounceTrigger > 0) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scoreBounce"
    )

    // ── 能量脉冲波纹：3 圈依次扩散 ──
    var ripple1 by remember { mutableIntStateOf(0) }
    var ripple2 by remember { mutableIntStateOf(0) }
    var ripple3 by remember { mutableIntStateOf(0) }
    LaunchedEffect(evaluation.totalScore) {
        kotlinx.coroutines.delay(500)
        ripple1++
        kotlinx.coroutines.delay(150)
        ripple2++
        kotlinx.coroutines.delay(150)
        ripple3++
    }
    val ripple1P by animateFloatAsState(if (ripple1 > 0) 1f else 0f, tween(600), label = "r1")
    val ripple2P by animateFloatAsState(if (ripple2 > 0) 1f else 0f, tween(600), label = "r2")
    val ripple3P by animateFloatAsState(if (ripple3 > 0) 1f else 0f, tween(600), label = "r3")

    // ── 状态标签延迟入场 ──
    var showLabel by remember { mutableStateOf(false) }
    LaunchedEffect(evaluation.totalScore) {
        kotlinx.coroutines.delay(950)
        showLabel = true
    }
    val labelAlpha by animateFloatAsState(
        targetValue = if (showLabel) 1f else 0f,
        animationSpec = tween(300),
        label = "labelAlpha"
    )

    // ── 配色 ──
    val ringColor = when (evaluation.rating) {
        SignalQualityEvaluator.Rating.EXCELLENT -> Color(0xFF2E7D32)
        SignalQualityEvaluator.Rating.GOOD -> Color(0xFFF9A825)
        SignalQualityEvaluator.Rating.FAIR -> Color(0xFFE65100)
        SignalQualityEvaluator.Rating.POOR -> Color(0xFFC62828)
        SignalQualityEvaluator.Rating.WEAK -> Color(0xFF616161)
    }

    // 与主页信号环完全一致的像素值（Canvas 内直接使用 px，不用 dp）
    val strokePx = 45f    // 主页圆环宽度
    val rippleMaxPx = 80f // 波纹最大扩散距离

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        key(evaluation.totalScore) {
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    val radius = (size.minDimension - strokePx) / 2f
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // 背景弧（细灰线，半透明）
                    drawArc(
                        color = ringColor.copy(alpha = 0.15f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    // 前景弧（能量环）
                    drawArc(
                        color = ringColor,
                        startAngle = 135f,
                        sweepAngle = 270f * arcProgress.value,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    // ── 脉冲波纹 ──
                    fun drawRipple(progress: Float, delayFraction: Float) {
                        if (progress <= 0f) return
                        val p = (progress * (1f + delayFraction) - delayFraction).coerceIn(0f, 1f)
                        val rippleR = radius + strokePx * 0.6f + rippleMaxPx * p
                        val alpha = (1f - p).coerceAtLeast(0f) * 0.45f
                        drawCircle(
                            color = ringColor.copy(alpha = alpha),
                            radius = rippleR,
                            center = Offset(cx, cy),
                            style = Stroke(width = strokePx * 0.6f)
                        )
                    }
                    drawRipple(ripple1P, 0f)
                    drawRipple(ripple2P, 0.25f)
                    drawRipple(ripple3P, 0.4f)
                }

                // 中心数字 + 弹跳
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        scaleX = textScale
                        scaleY = textScale
                    }
                ) {
                    Text(
                        text = "$displayScore",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = ringColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = evaluation.rating.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ringColor,
                        modifier = Modifier.graphicsLayer {
                            alpha = labelAlpha
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBriefCard(
    param: SignalQualityEvaluator.ParamScore,
    onClick: () -> Unit
) {
    val scoreColor = when {
        param.score >= 80 -> Color(0xFF2E7D32)
        param.score >= 60 -> Color(0xFFF9A825)
        param.score >= 40 -> Color(0xFFE65100)
        param.value == Int.MAX_VALUE -> MaterialTheme.colorScheme.outline
        else -> Color(0xFFC62828)
    }

    val scoreLabel = when {
        param.value == Int.MAX_VALUE -> "不支持"
        param.score >= 80 -> "优秀"
        param.score >= 60 -> "良好"
        param.score >= 40 -> "一般"
        else -> "较差"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = param.value != Int.MAX_VALUE, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 评分圆点
            Surface(
                color = scoreColor,
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(10.dp)
            ) {}
            Spacer(Modifier.width(12.dp))

            // 参数名 + 一句话说明
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${param.label}  ${param.valueStr} ${param.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    param.briefExplanation,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    lineHeight = 16.sp
                )
            }

            // 评分标签
            Text(
                scoreLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
        }
    }
}
