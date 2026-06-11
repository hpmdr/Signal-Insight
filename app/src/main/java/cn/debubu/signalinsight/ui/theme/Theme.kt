package cn.debubu.signalinsight.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cn.debubu.signalinsight.data.theme.ThemeManager
import cn.debubu.signalinsight.data.theme.ThemePreset

@Composable
fun SignalInsightTheme(
    themeManager: ThemeManager,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val settings by themeManager.settings.collectAsState()

    val colorScheme = when {
        // 用户启用"使用系统主题色" + Android 12+ → 系统壁纸取色
        // 不限制 MIUI：让用户自己决定是否开启，即使效果不好也可以关掉
        settings.dynamicColorsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 使用预设主题
        else -> {
            val preset = ThemePreset.fromId(settings.presetId)
            generateColorScheme(preset, dark = darkTheme)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
