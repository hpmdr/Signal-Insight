package cn.debubu.signalinsight.ui.cellular

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import cn.debubu.signalinsight.data.cellular.MetricKey
import cn.debubu.signalinsight.data.cellular.SignalQualityEvaluator
import cn.debubu.signalinsight.ui.theme.BlueGrey300
import cn.debubu.signalinsight.ui.theme.Gold300
import cn.debubu.signalinsight.ui.theme.Gold800
import cn.debubu.signalinsight.ui.theme.Green300
import cn.debubu.signalinsight.ui.theme.Green800
import cn.debubu.signalinsight.ui.theme.Orange300
import cn.debubu.signalinsight.ui.theme.Orange900
import cn.debubu.signalinsight.ui.theme.Rose300
import cn.debubu.signalinsight.ui.theme.Rose800

/**
 * 信号指标动态显色方案。
 *
 * 设计原则：
 * 1. 评分阈值复用 [SignalQualityEvaluator]（与详情页/综合评估同源），不重复定义。
 * 2. 配色词表与综合评分环（ScoreRing）一致：优秀绿 / 良好琥珀 / 一般橙 / 较差红 / 极弱灰。
 * 3. 同一档位在亮/暗两种 Surface 上都必须清晰，故明暗各取一档色值
 *    （亮色用 700/800 深档保证白底对比度，暗色用 300 浅档保证黑底可读性）。
 */

/** 仅这四类指标参与强度评分与动态显色，其余（Band/PCI/EARFCN/TAC）保持中性。 */
private val SCORED_KEYS = setOf(
    MetricKey.RSRP, MetricKey.RSRQ, MetricKey.SINR, MetricKey.RSSI
)

/** 五个强度档位（与 SignalQualityEvaluator.Rating 边界一致） */
private enum class Level { EXCELLENT, GOOD, FAIR, POOR, WEAK }

/** 评分 → 档位 */
private fun levelOf(score: Int): Level = when {
    score >= 80 -> Level.EXCELLENT
    score >= 60 -> Level.GOOD
    score >= 40 -> Level.FAIR
    score >= 20 -> Level.POOR
    else -> Level.WEAK
}

// 亮色 Surface（近白）使用深档色值，保证白底对比度
private val LevelColorsLight = mapOf(
    Level.EXCELLENT to Green800, // 0xFF2E7D32 优秀 绿
    Level.GOOD to Gold800,       // 0xFFF9A825 良好 琥珀
    Level.FAIR to Orange900,     // 0xFFE65100 一般 橙
    Level.POOR to Rose800,       // 0xFFC62828 较差 红
    Level.WEAK to Color(0xFF616161), // 极弱 中性灰（与 ScoreRing 一致）
)

// 暗色 Surface（近黑）使用浅档色值，提亮保证黑底可读性
private val LevelColorsDark = mapOf(
    Level.EXCELLENT to Green300, // 0xFF81C784
    Level.GOOD to Gold300,       // 0xFFFFE082
    Level.FAIR to Orange300,     // 0xFFFFB74D
    Level.POOR to Rose300,       // 0xFFE57373
    Level.WEAK to BlueGrey300,   // 0xFF90A4AE 极弱 灰
)

/**
 * 计算某指标数值的动态显色（仅数值文本颜色）。
 *
 * 明暗判定读取实际生效的 surface 亮度，对「系统主题色」这类非常规底色同样稳健。
 *
 * @return 数值文本应使用的颜色；非评分指标或 N/A 返回 MaterialTheme 默认 onSurface。
 */
@Composable
fun metricColor(key: MetricKey, value: Int): Color {
    val neutral = MaterialTheme.colorScheme.onSurface
    if (key !in SCORED_KEYS) return neutral
    if (value == Int.MAX_VALUE) return neutral

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return levelColor(levelOf(SignalQualityEvaluator.score(key, value)), isDark)
}

/** 档位 → 明暗色值 */
private fun levelColor(level: Level, isDark: Boolean): Color {
    val palette = if (isDark) LevelColorsDark else LevelColorsLight
    return palette[level] ?: Color(0xFF616161)
}

/**
 * 由综合评级取动态显色（供综合评分环 ScoreRing 使用）。
 * Rating 边界与 [levelOf] 完全一致，直接映射到同一套色阶。
 */
@Composable
fun ratingColor(rating: SignalQualityEvaluator.Rating): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val level = when (rating) {
        SignalQualityEvaluator.Rating.EXCELLENT -> Level.EXCELLENT
        SignalQualityEvaluator.Rating.GOOD -> Level.GOOD
        SignalQualityEvaluator.Rating.FAIR -> Level.FAIR
        SignalQualityEvaluator.Rating.POOR -> Level.POOR
        SignalQualityEvaluator.Rating.WEAK -> Level.WEAK
    }
    return levelColor(level, isDark)
}

/**
 * 由评分（0-100）取动态显色（供指标速览卡 MetricBriefCard 使用）。
 *
 * @param available 指标是否可用；不可用（N/A）返回中性 outline，不进色阶。
 */
@Composable
fun scoreColor(score: Int, available: Boolean): Color {
    if (!available) return MaterialTheme.colorScheme.outline
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return levelColor(levelOf(score), isDark)
}
