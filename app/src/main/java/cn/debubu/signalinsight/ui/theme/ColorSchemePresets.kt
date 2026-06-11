package cn.debubu.signalinsight.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import cn.debubu.signalinsight.data.theme.ThemePreset

/**
 * 为指定主题预设生成完整的 Material3 ColorScheme
 *
 * 每个预设定义 3 个关键色（primary/secondary/tertiary），
 * 其余颜色槽使用通用的 Signal Insight 默认值。
 * 无法使用 ColorScheme.fromSeedColor()（非公开 API），
 * 故手动定义全色板。
 */
fun generateColorScheme(preset: ThemePreset, dark: Boolean) = when (preset) {
    ThemePreset.SIGNAL_BLUE -> if (dark) DarkSignalBlue else LightSignalBlue
    ThemePreset.TECH_CYAN -> if (dark) DarkTechCyan else LightTechCyan
    ThemePreset.DARK_NAVY -> if (dark) DarkDarkNavy else LightDarkNavy
    ThemePreset.DEEP_TEAL -> if (dark) DarkDeepTeal else LightDeepTeal
    ThemePreset.MATRIX_GREEN -> if (dark) DarkMatrixGreen else LightMatrixGreen
    ThemePreset.LIME -> if (dark) DarkLime else LightLime
    ThemePreset.GOLD -> if (dark) DarkGold else LightGold
    ThemePreset.AMBER -> if (dark) DarkAmber else LightAmber
    ThemePreset.ROSE_RED -> if (dark) DarkRoseRed else LightRoseRed
    ThemePreset.PINK -> if (dark) DarkPink else LightPink
    ThemePreset.PURPLE -> if (dark) DarkPurple else LightPurple
    ThemePreset.STEEL -> if (dark) DarkSteel else LightSteel
}

// ═══════════════════════════════════════════════════════════════
// 以下为 6 个预设的 Light / Dark 全色板定义
// ═══════════════════════════════════════════════════════════════

// ── 1. Signal Blue (默认) ──
private val LightSignalBlue = lightColorScheme(
    primary = LightPrimary, onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer, onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary, onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer, onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary, onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer, onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkSignalBlue = darkColorScheme(
    primary = DarkPrimary, onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer, onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary, onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer, onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary, onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer, onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

// ── 版本一：共用背景/表面色，只换关键色 ──
// 其余 5 个预设复用 Signal Blue 的背景/表面/轮廓等非关键色，
// 只替换 primary/secondary/tertiary 及其对比色

private val LightTechCyan = lightColorScheme(
    primary = Cyan700, onPrimary = White, primaryContainer = Cyan100, onPrimaryContainer = Cyan900,
    secondary = BlueGrey600, onSecondary = White,
    secondaryContainer = BlueGrey100, onSecondaryContainer = BlueGrey900,
    tertiary = Teal500, onTertiary = White,
    tertiaryContainer = Teal100, onTertiaryContainer = Teal900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkTechCyan = darkColorScheme(
    primary = Cyan300, onPrimary = Cyan900, primaryContainer = Cyan800, onPrimaryContainer = Cyan100,
    secondary = BlueGrey300, onSecondary = BlueGrey900,
    secondaryContainer = BlueGrey700, onSecondaryContainer = BlueGrey100,
    tertiary = Teal300, onTertiary = Teal900,
    tertiaryContainer = Teal700, onTertiaryContainer = Teal100,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

private val LightDarkNavy = lightColorScheme(
    primary = Navy600, onPrimary = White, primaryContainer = Navy100, onPrimaryContainer = Navy900,
    secondary = Slate500, onSecondary = White,
    secondaryContainer = Slate100, onSecondaryContainer = Slate900,
    tertiary = Coral500, onTertiary = White,
    tertiaryContainer = Coral100, onTertiaryContainer = Coral900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkDarkNavy = darkColorScheme(
    primary = Navy300, onPrimary = Navy900, primaryContainer = Navy700, onPrimaryContainer = Navy100,
    secondary = Slate300, onSecondary = Slate900,
    secondaryContainer = Slate700, onSecondaryContainer = Slate100,
    tertiary = Coral300, onTertiary = Coral900,
    tertiaryContainer = Coral700, onTertiaryContainer = Coral100,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

private val LightMatrixGreen = lightColorScheme(
    primary = Green700, onPrimary = White, primaryContainer = Green100, onPrimaryContainer = Green900,
    secondary = Lime600, onSecondary = White,
    secondaryContainer = Lime100, onSecondaryContainer = Lime900,
    tertiary = Cyan600, onTertiary = White,
    tertiaryContainer = Cyan100, onTertiaryContainer = Cyan900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkMatrixGreen = darkColorScheme(
    primary = Green300, onPrimary = Green900, primaryContainer = Green800, onPrimaryContainer = Green100,
    secondary = Lime300, onSecondary = Lime900,
    secondaryContainer = Lime700, onSecondaryContainer = Lime100,
    tertiary = Cyan300, onTertiary = Cyan900,
    tertiaryContainer = Cyan700, onTertiaryContainer = Cyan100,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

private val LightAmber = lightColorScheme(
    primary = Orange800, onPrimary = White, primaryContainer = Orange100, onPrimaryContainer = Orange900,
    secondary = Brown600, onSecondary = White,
    secondaryContainer = Brown100, onSecondaryContainer = Brown900,
    tertiary = DeepOrange500, onTertiary = White,
    tertiaryContainer = DeepOrange100, onTertiaryContainer = DeepOrange900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkAmber = darkColorScheme(
    primary = Orange300, onPrimary = Orange900, primaryContainer = Orange800, onPrimaryContainer = Orange100,
    secondary = Brown300, onSecondary = Brown900,
    secondaryContainer = Brown700, onSecondaryContainer = Brown100,
    tertiary = DeepOrange300, onTertiary = DeepOrange900,
    tertiaryContainer = DeepOrange700, onTertiaryContainer = DeepOrange100,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

private val LightPurple = lightColorScheme(
    primary = Purple600, onPrimary = White, primaryContainer = Purple100, onPrimaryContainer = Purple900,
    secondary = Pink600, onSecondary = White,
    secondaryContainer = Pink100, onSecondaryContainer = Pink900,
    tertiary = Indigo500, onTertiary = White,
    tertiaryContainer = Indigo100, onTertiaryContainer = Indigo900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkPurple = darkColorScheme(
    primary = Purple300, onPrimary = Purple900, primaryContainer = Purple700, onPrimaryContainer = Purple100,
    secondary = Pink300, onSecondary = Pink900,
    secondaryContainer = Pink700, onSecondaryContainer = Pink100,
    tertiary = Indigo300, onTertiary = Indigo900,
    tertiaryContainer = Indigo700, onTertiaryContainer = Indigo100,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

// ── 7. Deep Teal ──
private val LightDeepTeal = lightColorScheme(
    primary = DeepTeal800, onPrimary = White, primaryContainer = DeepTeal100, onPrimaryContainer = DeepTeal900,
    secondary = Cyan600, onSecondary = White,
    secondaryContainer = Cyan100, onSecondaryContainer = Cyan900,
    tertiary = DeepTeal600, onTertiary = White,
    tertiaryContainer = DeepTeal50, onTertiaryContainer = DeepTeal900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkDeepTeal = darkColorScheme(
    primary = DeepTeal300, onPrimary = DeepTeal900, primaryContainer = DeepTeal700, onPrimaryContainer = DeepTeal100,
    secondary = Cyan300, onSecondary = Cyan900,
    secondaryContainer = Cyan700, onSecondaryContainer = Cyan100,
    tertiary = DeepTeal300, onTertiary = DeepTeal900,
    tertiaryContainer = DeepTeal700, onTertiaryContainer = DeepTeal50,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

// ── 8. Lime (Olive) ──
private val LightLime = lightColorScheme(
    primary = Olive800, onPrimary = White, primaryContainer = Olive100, onPrimaryContainer = Olive900,
    secondary = Olive600, onSecondary = White,
    secondaryContainer = Olive100, onSecondaryContainer = Olive900,
    tertiary = Lime600, onTertiary = White,
    tertiaryContainer = Lime100, onTertiaryContainer = Lime900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkLime = darkColorScheme(
    primary = Olive400, onPrimary = Olive900, primaryContainer = Olive700, onPrimaryContainer = Olive100,
    secondary = Olive400, onSecondary = Olive900,
    secondaryContainer = Olive700, onSecondaryContainer = Olive100,
    tertiary = Lime300, onTertiary = Lime900,
    tertiaryContainer = Lime700, onTertiaryContainer = Lime100,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

// ── 9. Gold ──
private val LightGold = lightColorScheme(
    primary = Gold800, onPrimary = White, primaryContainer = Gold100, onPrimaryContainer = Gold900,
    secondary = Gold600, onSecondary = White,
    secondaryContainer = Gold100, onSecondaryContainer = Gold900,
    tertiary = DeepOrange500, onTertiary = White,
    tertiaryContainer = DeepOrange100, onTertiaryContainer = DeepOrange900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkGold = darkColorScheme(
    primary = Gold300, onPrimary = Gold900, primaryContainer = Gold700, onPrimaryContainer = Gold100,
    secondary = Gold300, onSecondary = Gold900,
    secondaryContainer = Gold700, onSecondaryContainer = Gold100,
    tertiary = DeepOrange300, onTertiary = DeepOrange900,
    tertiaryContainer = DeepOrange700, onTertiaryContainer = DeepOrange100,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

// ── 10. Rose Red ──
private val LightRoseRed = lightColorScheme(
    primary = Rose800, onPrimary = White, primaryContainer = Rose100, onPrimaryContainer = Rose900,
    secondary = Pink600, onSecondary = White,
    secondaryContainer = Pink100, onSecondaryContainer = Pink900,
    tertiary = Rose600, onTertiary = White,
    tertiaryContainer = Rose100, onTertiaryContainer = Rose900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkRoseRed = darkColorScheme(
    primary = Rose300, onPrimary = Rose900, primaryContainer = Rose700, onPrimaryContainer = Rose100,
    secondary = Pink300, onSecondary = Pink900,
    secondaryContainer = Pink700, onSecondaryContainer = Pink100,
    tertiary = Rose300, onTertiary = Rose900,
    tertiaryContainer = Rose700, onTertiaryContainer = Rose100,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

// ── 11. Pink ──
private val LightPink = lightColorScheme(
    primary = Pink800, onPrimary = White, primaryContainer = Pink100, onPrimaryContainer = Pink900,
    secondary = Purple600, onSecondary = White,
    secondaryContainer = Purple100, onSecondaryContainer = Purple900,
    tertiary = Pink600, onTertiary = White,
    tertiaryContainer = Pink100, onTertiaryContainer = Pink900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkPink = darkColorScheme(
    primary = Pink300, onPrimary = Pink900, primaryContainer = Pink700, onPrimaryContainer = Pink100,
    secondary = Purple300, onSecondary = Purple900,
    secondaryContainer = Purple700, onSecondaryContainer = Purple100,
    tertiary = Pink300, onTertiary = Pink900,
    tertiaryContainer = Pink700, onTertiaryContainer = Pink100,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)

// ── 12. Steel ──
private val LightSteel = lightColorScheme(
    primary = Steel700, onPrimary = White, primaryContainer = Steel100, onPrimaryContainer = Steel900,
    secondary = Steel600, onSecondary = White,
    secondaryContainer = Steel50, onSecondaryContainer = Steel900,
    tertiary = Indigo500, onTertiary = White,
    tertiaryContainer = Indigo100, onTertiaryContainer = Indigo900,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, error = LightError, onError = LightOnError
)
private val DarkSteel = darkColorScheme(
    primary = Steel300, onPrimary = Steel900, primaryContainer = Steel700, onPrimaryContainer = Steel100,
    secondary = Steel300, onSecondary = Steel900,
    secondaryContainer = Steel700, onSecondaryContainer = Steel50,
    tertiary = Indigo300, onTertiary = Indigo900,
    tertiaryContainer = Indigo700, onTertiaryContainer = Indigo100,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, error = DarkError, onError = DarkOnError
)
