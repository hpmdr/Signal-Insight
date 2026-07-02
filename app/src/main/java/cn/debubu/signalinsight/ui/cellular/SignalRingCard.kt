package cn.debubu.signalinsight.ui.cellular

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.data.cellular.MetricKey
import cn.debubu.signalinsight.data.cellular.SignalData

/**
 * 信号环卡片 — 环形进度 + dBm 数值 + 运营商/网络类型 + 质量标签。
 *
 * 支持 SharedTransition 动画：点击后容器缩放展开到详情页。
 *
 * @param signalData            当前 SIM 的信号数据
 * @param statusColor           根据信号强度计算的颜色
 * @param statusLabel           信号质量标签（优/一般/差）
 * @param onClick               点击回调 → 跳转综合诊断页
 * @param modifier              来自调用方的 Modifier（遵循 Compose API 规范，默认空）
 * @param sharedTransitionScope SharedTransition 作用域（非空时启用容器转换动画）
 * @param animatedVisibilityScope 动画可见性作用域
 * @param sharedContentStates   共享内容状态映射
 * @param ringSize              信号环尺寸（竖屏 180dp，横屏 compact 110dp）
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SignalRingCard(
    signalData: SignalData,
    statusColor: Color,
    statusLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedContentStates: Map<MetricKey, SharedTransitionScope.SharedContentState>? = null,
    ringSize: Dp = 180.dp,
) {
    val isCompact = ringSize < 150.dp
    val overviewSharedState = sharedContentStates?.get(MetricKey.OVERVIEW)

    // 将调用方 modifier 与 sharedBounds 叠加（遵循 Compose 规范：只在末尾追加）
    val surfaceModifier = if (sharedTransitionScope != null && overviewSharedState != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            modifier.sharedBounds(
                sharedContentState = overviewSharedState,
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                boundsTransform = { _, _ ->
                    tween(420, easing = CubicBezierEasing(0.1f, 0.8f, 0.1f, 1.0f))
                }
            )
        }
    } else {
        modifier
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(if (isCompact) 20.dp else 28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = surfaceModifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 16.dp else 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            val animatedRsrp by animateIntAsState(
                targetValue = if (signalData.dbm != Int.MAX_VALUE) signalData.dbm else -120,
                animationSpec = tween(1000),
            )
            val progress by animateFloatAsState(
                targetValue = signalData.progress,
                animationSpec = tween(1500),
            )

            // 环形进度
            Canvas(modifier = Modifier.size(ringSize)) {
                val strokeWidth = if (isCompact) 35f else 45f
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    style = Stroke(width = strokeWidth),
                )
                drawArc(
                    color = statusColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            // 中央文本
            val isDbmValid = signalData.dbm != Int.MAX_VALUE
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (isDbmValid) "$animatedRsrp" else stringResource(R.string.metric_no_data),
                        style = if (isCompact) MaterialTheme.typography.headlineSmall
                               else MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        letterSpacing = (-2).sp,
                    )
                    if (isDbmValid) {
                        Text(
                            "dBm",
                            Modifier.padding(bottom = if (isCompact) 4.dp else 10.dp, start = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Text(
                    text = "${signalData.operatorName} ${signalData.networkType}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.alpha(0.6f),
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)),
                ) {
                    Text(
                        stringResource(R.string.cellular_signal_quality, statusLabel),
                        Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                    )
                }
            }
        }
    }
}
