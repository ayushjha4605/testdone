package com.testdone.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.testdone.app.ui.theme.TdExt
import kotlinx.coroutines.delay

// ── shimmer ─────────────────────────────────────────────────────────────────

/** Shimmer placeholder box. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    val base = TdExt.colors.shimmerBase
    val highlight = TdExt.colors.shimmerHighlight
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(progress * 400f, 0f),
                    end = Offset(progress * 400f + 400f, 220f),
                )
            )
    )
}

/** Modifier shimmer used for images. */
fun Modifier.shimmerBackground(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerBg")
    val progress by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerBgProgress",
    )
    val base = TdExt.colors.shimmerBase
    val highlight = TdExt.colors.shimmerHighlight
    background(
        Brush.linearGradient(
            listOf(base, highlight, base),
            start = Offset(progress * 500f, 0f),
            end = Offset(progress * 500f + 500f, 300f),
        )
    )
}

// ── counters ────────────────────────────────────────────────────────────────

/** Number that counts up from 0 → value (port of the web CountUp). */
@Composable
fun CountUpText(
    value: Int,
    durationMs: Int = 900,
    suffix: String = "",
    prefix: String = "",
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    var display by remember { mutableStateOf(0) }
    LaunchedEffect(value) {
        if (value <= 0) { display = 0; return@LaunchedEffect }
        val startNs = withFrameNanos { it }
        var lastFrame = startNs
        while (lastFrame - startNs < durationMs * 1_000_000L) {
            val t = ((lastFrame - startNs).toFloat() / (durationMs * 1_000_000f)).coerceIn(0f, 1f)
            val eased = 1f - (1f - t) * (1f - t) * (1f - t) // easeOutCubic
            display = (value * eased).toInt()
            lastFrame = withFrameNanos { it }
        }
        display = value
    }
    Text("$prefix$display$suffix", style = style, color = color)
}

// ── entrance motion ─────────────────────────────────────────────────────────

/**
 * Fade + slide-up entrance that triggers the first time the content composes,
 * delayed by [index] steps — so list sections cascade in one after another.
 * Items further down animate when they scroll into view (first composition).
 */
@Composable
fun StaggerIn(
    index: Int,
    modifier: Modifier = Modifier,
    delayStepMs: Int = 55,
    content: @Composable () -> Unit,
) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    val delay = (index * delayStepMs).coerceAtMost(540)
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(340, delayMillis = delay)) +
            slideInVertically(
                animationSpec = tween(400, delayMillis = delay, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 6 },
            ),
        exit = fadeOut(tween(120)),
    ) { content() }
}

/** Pop-in scale entrance (badges, avatars) with a springy overshoot. */
@Composable
fun PopIn(
    delayMs: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(200, delayMillis = delayMs)) +
            scaleIn(
                animationSpec = spring(
                    dampingRatio = 0.45f,
                    stiffness = Spring.StiffnessMedium,
                    visibilityThreshold = 0.01f,
                ),
                initialScale = 0.6f,
            ),
        exit = fadeOut(tween(120)),
    ) { content() }
}

// ── continuous motion ───────────────────────────────────────────────────────

/** Gentle breathing pulse for streak flames / live indicators. */
fun Modifier.pulse(
    minScale: Float = 1f,
    maxScale: Float = 1.12f,
    durationMs: Int = 850,
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            tween(durationMs, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Diagonal light sweep that travels across gradient CTAs (subtle "premium" sheen). */
fun Modifier.shimmerSweep(periodMs: Int = 2800): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "sweep")
    val x by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "sweepX",
    )
    drawWithContent {
        drawContent()
        val w = size.width
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.13f), Color.Transparent),
                start = Offset(x * w, 0f),
                end = Offset(x * w + w * 0.45f, size.height),
            ),
        )
    }
}

// ── data-viz motion ─────────────────────────────────────────────────────────

/** Circular progress ring that animates from 0 to [progress] on first show. */
@Composable
fun AnimatedProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 10.dp,
    brush: Brush = TdExt.colors.brandBrush,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(1000, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Canvas(modifier) {
        val strokePx = stroke.toPx()
        val inset = strokePx / 2
        val arcSize = Size(size.width - strokePx, size.height - strokePx)
        drawArc(
            color = trackColor,
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            style = Stroke(strokePx, cap = StrokeCap.Round),
            topLeft = Offset(inset, inset), size = arcSize,
        )
        if (animated > 0.005f) {
            drawArc(
                brush = brush,
                startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                style = Stroke(strokePx, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset), size = arcSize,
            )
        }
    }
}

// ── auto-swiping banner ─────────────────────────────────────────────────────

/**
 * Horizontal pager that auto-advances every [autoAdvanceMs] (pausing while the
 * user drags) with a morphing dot indicator — the classic promo carousel.
 */
@Composable
fun <T> AutoSwipingBanner(
    pages: List<T>,
    modifier: Modifier = Modifier,
    autoAdvanceMs: Long = 4200,
    pageSpacing: Dp = 10.dp,
    page: @Composable (T) -> Unit,
) {
    if (pages.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { pages.size })
    LaunchedEffect(pages.size) {
        if (pages.size > 1) {
            while (true) {
                delay(autoAdvanceMs)
                if (!pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % pages.size)
                }
            }
        }
    }
    Box(modifier) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = pageSpacing,
        ) { idx ->
            page(pages[idx])
        }
        if (pages.size > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(pages.size) { i ->
                    val active = pagerState.currentPage == i
                    val width by animateDpAsState(
                        targetValue = if (active) 16.dp else 6.dp,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                        label = "dotWidth",
                    )
                    Box(
                        Modifier
                            .width(width)
                            .height(6.dp)
                            .background(
                                if (active) Color.White else Color.White.copy(alpha = 0.45f),
                                CircleShape,
                            )
                    )
                }
            }
        }
    }
}
