package cn.debubu.signalinsight.data.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import cn.debubu.signalinsight.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 主题预设枚举
 * 每个预设用一个主色 seed，通过 Material3 的 fromSeedColor 生成完整色板
 */
enum class ThemePreset(
    val id: String,
    val displayNameResId: Int,
    val seedColor: Color
) {
    SIGNAL_BLUE("signal_blue", R.string.theme_signal_blue, Color(0xFF1A6D8A)),
    TECH_CYAN("tech_cyan", R.string.theme_tech_cyan, Color(0xFF00897B)),
    DARK_NAVY("dark_navy", R.string.theme_dark_navy, Color(0xFF1E3A5F)),
    DEEP_TEAL("deep_teal", R.string.theme_deep_teal, Color(0xFF00695C)),
    MATRIX_GREEN("matrix_green", R.string.theme_matrix_green, Color(0xFF2E7D32)),
    LIME("lime", R.string.theme_lime, Color(0xFF827717)),
    GOLD("gold", R.string.theme_gold, Color(0xFFF9A825)),
    AMBER("amber", R.string.theme_amber, Color(0xFFE65100)),
    ROSE_RED("rose_red", R.string.theme_rose_red, Color(0xFFC62828)),
    PINK("pink", R.string.theme_pink, Color(0xFFAD1457)),
    PURPLE("purple", R.string.theme_purple, Color(0xFF6A1B9A)),
    STEEL("steel", R.string.theme_steel, Color(0xFF455A64));

    companion object {
        fun fromId(id: String): ThemePreset =
            entries.find { it.id == id } ?: SIGNAL_BLUE
    }
}

/**
 * 主题设置数据类
 * @param presetId 当前选中的预设 ID
 * @param dynamicColorsEnabled 是否启用系统动态取色（仅非 MIUI 设备有效）
 * @param refreshIntervalMs 信号主动刷新间隔（毫秒），默认 5000ms（5 秒）
 */
data class ThemeSettings(
    val presetId: String = ThemePreset.SIGNAL_BLUE.id,
    val dynamicColorsEnabled: Boolean = true,
    val refreshIntervalMs: Int = 5_000
)

/**
 * 主题管理器 — 通过 SharedPreferences 持久化主题偏好
 * 使用 StateFlow 驱动 UI 即时刷新
 */
class ThemeManager(context: Context) {

    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    private fun load(): ThemeSettings = ThemeSettings(
        presetId = prefs.getString("preset_id", ThemePreset.SIGNAL_BLUE.id)
            ?: ThemePreset.SIGNAL_BLUE.id,
        dynamicColorsEnabled = prefs.getBoolean("dynamic_colors", true),
        refreshIntervalMs = prefs.getInt("refresh_interval_ms", 5_000)
    )

    private fun save(s: ThemeSettings) {
        prefs.edit()
            .putString("preset_id", s.presetId)
            .putBoolean("dynamic_colors", s.dynamicColorsEnabled)
            .putInt("refresh_interval_ms", s.refreshIntervalMs)
            .apply()
    }

    fun setThemePreset(presetId: String) {
        // 手动选择预设时自动关闭"使用系统主题色"
        val updated = _settings.value.copy(presetId = presetId, dynamicColorsEnabled = false)
        _settings.value = updated
        save(updated)
    }

    fun setDynamicColorsEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(dynamicColorsEnabled = enabled)
        _settings.value = updated
        save(updated)
    }

    fun setRefreshIntervalMs(intervalMs: Int) {
        val updated = _settings.value.copy(refreshIntervalMs = intervalMs)
        _settings.value = updated
        save(updated)
    }
}
