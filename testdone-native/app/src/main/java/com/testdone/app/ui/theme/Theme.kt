package com.testdone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── color schemes ────────────────────────────────────────────────────────────

/** DARK NEON — near-black canvas + electric indigo→cyan accents. */
private val DarkScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = Color.White,
    primaryContainer = Indigo500.copy(alpha = 0.25f),
    onPrimaryContainer = Indigo300,
    secondary = Violet400,
    onSecondary = Color.White,
    secondaryContainer = Violet500.copy(alpha = 0.22f),
    onSecondaryContainer = Violet300,
    tertiary = NeonCyan,
    onTertiary = Color(0xFF06202A),
    tertiaryContainer = NeonCyan.copy(alpha = 0.16f),
    onTertiaryContainer = Color(0xFFA5F3FC),
    background = NeonBg950,
    onBackground = TextPrimaryDark,
    surface = NeonBg850,
    onSurface = TextPrimaryDark,
    surfaceVariant = NeonBg800,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = NeonBg850,
    surfaceContainerHigh = NeonBg800,
    surfaceContainerHighest = NeonBg700,
    surfaceContainerLow = NeonBg900,
    surfaceContainerLowest = NeonBg950,
    outline = Color(0xFF262B40),
    outlineVariant = Color(0xFF1B1F31),
    error = Rose400,
    onError = Color.White,
    errorContainer = Rose500.copy(alpha = 0.18f),
    onErrorContainer = Rose300,
)

/** LIGHT CLEAN — soft cool-white canvas, confident indigo primary (default). */
private val LightScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF3730A3),
    secondary = Violet500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = Fuchsia500,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF86198F),
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = Color(0xFFF3F4FB),
    surfaceContainerHigh = SurfaceVariantLight,
    surfaceContainerHighest = Color(0xFFE6E8F2),
    surfaceContainerLow = Color(0xFFFAFBFF),
    surfaceContainerLowest = Color.White,
    outline = Color(0xFFCDD2E4),
    outlineVariant = Color(0xFFE2E5F0),
    error = Rose500,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF9F1239),
)

// ── extended palette exposed to composables ─────────────────────────────────

data class ExtendedColors(
    val success: Color,
    val successDim: Color,
    val warning: Color,
    val warningDim: Color,
    val danger: Color,
    val dangerDim: Color,
    val info: Color,
    val infoDim: Color,
    val premium: Color,
    val cardBorder: Color,
    val textMuted: Color,
    val brandBrush: Brush,
    val ultraBrush: Brush,
    val premiumBrush: Brush,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = Emerald400, successDim = Emerald500.copy(alpha = 0.15f),
        warning = Amber400, warningDim = Amber500.copy(alpha = 0.15f),
        danger = Rose400, dangerDim = Rose500.copy(alpha = 0.15f),
        info = Sky400, infoDim = Sky400.copy(alpha = 0.15f),
        premium = Amber400,
        cardBorder = Color(0x14FFFFFF),
        textMuted = TextMutedDark,
        brandBrush = Brush.linearGradient(listOf(Indigo500, Violet500)),
        ultraBrush = Brush.linearGradient(listOf(Violet500, Fuchsia500)),
        premiumBrush = Brush.linearGradient(listOf(Amber500, Color(0xFFF97316))),
        shimmerBase = Ink800, shimmerHighlight = Ink700,
    )
}

private val DarkExtended = ExtendedColors(
    success = Emerald400, successDim = Emerald500.copy(alpha = 0.15f),
    warning = Amber400, warningDim = Amber500.copy(alpha = 0.15f),
    danger = Rose400, dangerDim = Rose500.copy(alpha = 0.15f),
    info = NeonCyan, infoDim = NeonCyan.copy(alpha = 0.14f),
    premium = Amber400,
    cardBorder = Color(0x14FFFFFF),
    textMuted = TextMutedDark,
    brandBrush = Brush.linearGradient(listOf(Indigo400, NeonCyan)),
    ultraBrush = Brush.linearGradient(listOf(Violet400, Fuchsia500)),
    premiumBrush = Brush.linearGradient(listOf(Amber500, Color(0xFFF97316))),
    shimmerBase = NeonBg800, shimmerHighlight = NeonBg700,
)

private val LightExtended = ExtendedColors(
    success = Emerald500, successDim = Emerald500.copy(alpha = 0.12f),
    warning = Amber500, warningDim = Amber500.copy(alpha = 0.12f),
    danger = Rose500, dangerDim = Rose500.copy(alpha = 0.12f),
    info = Sky400, infoDim = Sky400.copy(alpha = 0.12f),
    premium = Amber500,
    cardBorder = Color(0x1A000000),
    textMuted = TextMutedLight,
    brandBrush = Brush.linearGradient(listOf(Indigo600, Violet500)),
    ultraBrush = Brush.linearGradient(listOf(Violet500, Fuchsia500)),
    premiumBrush = Brush.linearGradient(listOf(Amber500, Color(0xFFF97316))),
    shimmerBase = Color(0xFFE7EAF4), shimmerHighlight = Color(0xFFF4F6FC),
)

/** Convenience accessor: MaterialTheme.extended */
object TestDoneThemeDefaults

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TestDoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val extended = if (darkTheme) DarkExtended else LightExtended
    androidx.compose.runtime.CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = scheme,
            typography = TdTypography,
            shapes = Shapes,
            content = content,
        )
    }
}

/** Access the extended palette anywhere below TestDoneTheme. */
object TdExt {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
