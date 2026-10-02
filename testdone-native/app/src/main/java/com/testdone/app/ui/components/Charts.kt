package com.testdone.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.testdone.app.ui.theme.TdExt

/** Animated donut showing a percentage score. */
@Composable
fun ScoreDonut(
    percent: Int,
    modifier: Modifier = Modifier,
    stroke: Float = 10f,
    trackColor: Color? = null,
) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { played = true }
    val sweep by animateFloatAsState(
        targetValue = if (played) percent * 3.6f else 0f,
        animationSpec = tween(900),
        label = "donutSweep",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        val brush = TdExt.colors.brandBrush
        val track = trackColor ?: MaterialTheme.colorScheme.surfaceContainerHighest
        Canvas(Modifier.fillMaxSize()) {
            val sizePx = minOf(size.width, size.height)
            val topLeft = Offset((size.width - sizePx) / 2f, (size.height - sizePx) / 2f)
            val arcSize = Size(sizePx, sizePx)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = brush,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            "$percent%",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Simple animated line chart (score trend), drawn on Canvas. */
@Composable
fun TrendLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColors: List<Color> = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { played = true }
    val progress by animateFloatAsState(if (played) 1f else 0f, tween(900), label = "trendProgress")

    Canvas(modifier.fillMaxWidth().height(140.dp).padding(vertical = 8.dp)) {
        if (values.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val padding = 8f
        val stepX = (w - padding * 2) / (values.size - 1)
        val maxY = 100f
        fun pointAt(i: Int): Offset {
            val x = padding + i * stepX
            val y = (h - padding) - (values[i].coerceIn(0f, maxY) / maxY) * (h - padding * 2)
            return Offset(x, y)
        }

        // grid
        val gridColor = Color.White.copy(alpha = 0.05f)
        for (g in 1..3) {
            val y = padding + (h - padding * 2) * g / 4f
            drawLine(
                gridColor,
                Offset(padding, y),
                Offset(w - padding, y),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f)),
            )
        }

        // area fill + line up to `progress`
        val visibleCount = 1 + ((values.size - 1) * progress).toInt()
        val pts = (0 until visibleCount).map { pointAt(it) }

        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]
                val cur = pts[i]
                val midX = (prev.x + cur.x) / 2
                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            }
        }
        val areaPath = Path().apply {
            addPath(path)
            lineTo(pts.last().x, h - padding)
            lineTo(pts.first().x, h - padding)
            close()
        }
        drawPath(
            areaPath,
            Brush.verticalGradient(
                listOf(lineColors.first().copy(alpha = 0.28f), Color.Transparent),
                endY = h,
            ),
        )
        drawPath(
            path,
            Brush.horizontalGradient(lineColors),
            style = Stroke(5f, cap = StrokeCap.Round),
        )

        // end dot
        drawCircle(lineColors.last(), radius = 7f, center = pts.last())
        drawCircle(Color.White, radius = 3f, center = pts.last())
    }
}

/** Horizontal bar chart (subject-wise accuracy). */
@Composable
fun SubjectBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { played = true }

    androidx.compose.foundation.layout.Column(modifier) {
        data.take(6).forEachIndexed { index, (name, pct) ->
            val fill by animateFloatAsState(
                targetValue = if (played) pct / 100f else 0f,
                animationSpec = tween(700, delayMillis = index * 80),
                label = "barFill$index",
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(0.32f),
                )
                val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .weight(1f)
                        .height(10.dp),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val radius = CornerRadius(size.height / 2, size.height / 2)
                        drawRoundRect(
                            color = trackColor,
                            cornerRadius = radius,
                        )
                        drawRoundRect(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
                            size = Size(size.width * fill, size.height),
                            cornerRadius = radius,
                        )
                    }
                }
                Text(
                    "$pct%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}
