package com.testdone.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.testdone.app.ui.theme.TdExt
import androidx.compose.material.icons.rounded.Warning

/** Springy press-scale that all tappable elements use — bounces back on release. */
@Composable
fun Modifier.pressableScale(
    pressedScale: Float = 0.97f,
    onClick: (() -> Unit)? = null,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )
    val base = this.scale(scale)
    return if (onClick != null) {
        base.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else base
}

/** Parse the web app's CSS gradients ("linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%)") into a Brush. */
fun cssGradient(css: String?, fallback: List<Color>): Brush {
    if (css.isNullOrBlank()) return Brush.linearGradient(fallback)
    val hexes = Regex("#([0-9A-Fa-f]{6})").findAll(css).map { it.value }.toList()
    if (hexes.isEmpty()) return Brush.linearGradient(fallback)
    val colors = hexes.take(3).map { Color(android.graphics.Color.parseColor(it)) }
    return Brush.linearGradient(colors)
}

// ── Buttons ──────────────────────────────────────────────────────────────────

enum class TdButtonStyle { GRADIENT, SECONDARY, GHOST, DANGER }

@Composable
fun TdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TdButtonStyle = TdButtonStyle.GRADIENT,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(18.dp)
    val scaleMod = Modifier.pressableScale(0.96f, if (enabled && !loading) onClick else null)
    Box(
        modifier = modifier
            .then(scaleMod)
            .height(56.dp)
            .background(
                brush = when (style) {
                    TdButtonStyle.GRADIENT -> TdExt.colors.brandBrush
                    TdButtonStyle.SECONDARY -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
                    TdButtonStyle.GHOST -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    TdButtonStyle.DANGER -> Brush.linearGradient(listOf(TdExt.colors.danger.copy(alpha = 0.15f), TdExt.colors.danger.copy(alpha = 0.15f)))
                },
                shape = shape,
            )
            .border(
                width = when (style) {
                    TdButtonStyle.SECONDARY, TdButtonStyle.DANGER -> 1.dp
                    else -> 0.dp
                },
                color = when (style) {
                    TdButtonStyle.SECONDARY -> MaterialTheme.colorScheme.outline
                    TdButtonStyle.DANGER -> TdExt.colors.danger.copy(alpha = 0.4f)
                    else -> Color.Transparent
                },
                shape = shape,
            )
            .then(
                if (style == TdButtonStyle.GHOST) Modifier.border(0.dp, Color.Transparent, shape) else Modifier
            ),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (loading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = if (style == TdButtonStyle.GRADIENT) Color.White else MaterialTheme.colorScheme.primary,
                )
            } else {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = buttonContent(style), modifier = Modifier.size(20.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = buttonContent(style),
                )
            }
        }
    }
}

@Composable
private fun buttonContent(style: TdButtonStyle): Color = when (style) {
    TdButtonStyle.GRADIENT -> Color.White
    TdButtonStyle.SECONDARY -> MaterialTheme.colorScheme.onSurface
    TdButtonStyle.GHOST -> MaterialTheme.colorScheme.primary
    TdButtonStyle.DANGER -> TdExt.colors.danger
}

// ── Cards & badges ──────────────────────────────────────────────────────────

/**
 * Card surface. Padding order matters: background+border are applied BEFORE
 * [contentPadding], so padding lives INSIDE the card (content never touches
 * the edges) and the card itself spans the full modifier width.
 * Previously callers passed `.padding()` in [modifier], which landed BEFORE
 * the background — producing a shrunken, inset card with zero inner padding
 * (text visually colliding with the border). All call-sites now use
 * contentPadding instead.
 */
@Composable
fun TdCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false) else Modifier)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, TdExt.colors.cardBorder, shape)
            .padding(contentPadding),
    ) { content() }
}

@Composable
fun Pill(
    text: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1,
        )
    }
}

// ── Section header ──────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (onAction != null && actionLabel != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .pressableScale(0.94f, onAction)
                    .padding(4.dp),
            )
        }
    }
}

// ── States: loading / empty / error ─────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    cta: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            )
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (cta != null) {
            Spacer(Modifier.height(20.dp))
            TdButton(cta.first, onClick = cta.second, style = TdButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth(0.7f))
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(TdExt.colors.dangerDim, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Rounded.Warning,
                contentDescription = null,
                tint = TdExt.colors.danger,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Kuch galat ho gaya", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(18.dp))
            TdButton("Retry", onClick = onRetry, style = TdButtonStyle.SECONDARY)
        }
    }
}

/** Row of skeleton cards for lists. */
@Composable
fun SkeletonList(rows: Int = 5) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) { _ ->
            TdCard(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(Modifier.size(48.dp), RoundedCornerShape(14.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerBox(Modifier.fillMaxWidth(0.75f).height(14.dp), RoundedCornerShape(7.dp))
                        ShimmerBox(Modifier.fillMaxWidth(0.5f).height(11.dp), RoundedCornerShape(6.dp))
                    }
                }
            }
        }
    }
}
