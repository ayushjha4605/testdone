package com.testdone.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import com.testdone.app.ui.theme.TdExt
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Confetti burst (test report celebration) ────────────────────────────────

private data class Particle(
    var x: Float, var y: Float,
    val vx: Float, var vy: Float,
    val color: Color, val size: Float,
    var rotation: Float, val vr: Float,
    val life: Float,
)

@Composable
fun Confetti(modifier: Modifier = Modifier, durationMs: Int = 2600) {
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFD946EF),
        Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF38BDF8),
    )
    var playing by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val particles = remember {
        val rand = Random(42)
        List(90) {
            val angle = rand.nextFloat() * (2 * Math.PI).toFloat()
            val speed = 4f + rand.nextFloat() * 11f
            Particle(
                x = 0.5f, y = 0.15f,
                vx = cos(angle) * speed * 0.6f,
                vy = sin(angle) * speed - 6f,
                color = colors[rand.nextInt(colors.size)],
                size = 6f + rand.nextFloat() * 8f,
                rotation = rand.nextFloat() * 360f,
                vr = (rand.nextFloat() - 0.5f) * 24f,
                life = 1f,
            )
        }
    }

    LaunchedEffect(Unit) {
        val start = awaitFrame()
        var last = start
        val total = durationMs * 1_000_000f
        while (true) {
            val now = awaitFrame()
            val dt = ((now - last) / 1_000_000f).coerceIn(0f, 40f) / 16.6f // in frames
            last = now
            val t = (now - start) / total
            if (t >= 1f) break
            particles.forEach { p ->
                p.vy += 0.32f * dt          // gravity
                p.x += p.vx * dt * 0.006f
                p.y += p.vy * dt * 0.006f
                p.rotation += p.vr * dt
            }
            playing = t
        }
    }

    if (playing in 0.001f..0.999f) {
        Canvas(modifier.fillMaxSize()) {
            particles.forEach { p ->
                val alpha = (1f - playing).coerceIn(0f, 1f)
                withTransform({ rotate(p.rotation, Offset(p.x * size.width, p.y * size.height)) }) {
                    drawRoundRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(p.x * size.width, p.y * size.height),
                        size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                    )
                }
            }
        }
    }
}

// ── In-app toast (port of the web sonner toasts) ────────────────────────────

@Composable
fun ToastHost(
    toast: com.testdone.app.di.SessionCoordinator.Toast?,
    onDismiss: () -> Unit,
) {
    if (toast == null) return
    LaunchedEffect(toast) {
        kotlinx.coroutines.delay(if (toast.detail != null) 4200 else 2800)
        onDismiss()
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, TdExt.colors.cardBorder),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (icon, tint) = when (toast.kind) {
                    com.testdone.app.di.SessionCoordinator.Toast.Kind.SUCCESS -> Icons.Rounded.CheckCircle to TdExt.colors.success
                    com.testdone.app.di.SessionCoordinator.Toast.Kind.ERROR -> Icons.Rounded.Error to TdExt.colors.danger
                    else -> Icons.Rounded.Info to TdExt.colors.info
                }
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(toast.message, style = MaterialTheme.typography.titleSmall)
                    if (toast.detail != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            toast.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
