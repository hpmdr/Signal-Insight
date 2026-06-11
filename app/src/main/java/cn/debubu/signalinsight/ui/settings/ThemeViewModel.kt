package cn.debubu.signalinsight.ui.settings

import androidx.lifecycle.ViewModel
import cn.debubu.signalinsight.data.theme.ThemeManager
import cn.debubu.signalinsight.data.theme.ThemePreset
import cn.debubu.signalinsight.data.theme.ThemeSettings
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel(
    private val themeManager: ThemeManager
) : ViewModel() {

    val settings: StateFlow<ThemeSettings> = themeManager.settings

    val presets: List<ThemePreset> = ThemePreset.entries

    fun selectPreset(presetId: String) {
        themeManager.setThemePreset(presetId)
    }

    fun toggleDynamicColors(enabled: Boolean) {
        themeManager.setDynamicColorsEnabled(enabled)
    }
}
