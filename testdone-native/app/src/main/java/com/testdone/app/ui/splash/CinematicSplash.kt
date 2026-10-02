package com.testdone.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testdone.app.R
import com.testdone.app.ui.theme.InterDisplayFamily
import kotlinx.coroutines.delay

/**
 * Cinematic launch splash — native port of the web app's Netflix-style splash:
 *   0.0s  ambient glow breathes in behind the logo
 *   0.1s  logo zoom-settles from huge + blurred → sharp
 *   0.6s  "TestDone" letters stagger up
 *   1.2s  tagline fades in
 *   ~2.2s whole layer dissolves into the app
 */
@Composable
fun CinematicSplash(onDone: () -> Unit) {
    val bg = Color(0xFF09090B)

    val glow = remember { Animatable(0f) }
    val glowScale = remember { Animatable(0.55f) }
    val logoScale = remember { Animatable(2.4f) }
    val logoBlur = remember { Animatable(22f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordOffset = remember { Animatable(28f) }
    val tagAlpha = remember { Animatable(0f) }
    val exitScale = remember { Animatable(1f) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // ambient glow
        glow.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        glowScale.animateTo(1.12f, tween(1600, easing = FastOutSlowInEasing))
        // logo zoom-settle
        delay(100)
        logoAlpha.animateTo(1f, tween(220))
        logoScale.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        logoBlur.animateTo(0f, tween(650, easing = FastOutSlowInEasing))
        // staggered letters
        delay(450)
        wordOffset.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        wordAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        // tagline
        delay(550)
        tagAlpha.animateTo(1f, tween(500))
        // exit
        delay(700)
        exitScale.animateTo(1.08f, tween(500, easing = FastOutSlowInEasing))
        exitAlpha.animateTo(0f, tween(500))
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .graphicsLayer {
                scaleX = exitScale.value
                scaleY = exitScale.value
                alpha = exitAlpha.value
            },
        contentAlignment = Alignment.Center,
    ) {
        // ambient radial glow
        Box(
            Modifier
                .size(420.dp)
                .graphicsLayer {
                    scaleX = glowScale.value
                    scaleY = glowScale.value
                    alpha = glow.value * 0.9f
                }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x4D6366F1), Color(0x1F8B5CF6), Color.Transparent),
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // logo — zoom-settle
            Image(
                painter = painterResource(R.drawable.logo_full),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha = logoAlpha.value
                    }
                    .blur(logoBlur.value.dp)
                    .padding(4.dp),
            )

            // staggered word
            androidx.compose.foundation.layout.Row(
                Modifier
                    .padding(top = 26.dp)
                    .graphicsLayer {
                        translationY = wordOffset.value
                        alpha = wordAlpha.value
                    },
            ) {
                "TestDone".forEachIndexed { i, ch ->
                    val letterDelay = i * 40L
                    val lAlpha = remember { Animatable(0f) }
                    val lY = remember { Animatable(16f) }
                    LaunchedEffect(Unit) {
                        delay(450 + letterDelay)
                        lAlpha.animateTo(1f, tween(320))
                        lY.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
                    }
                    val gradient = Brush.verticalGradient(listOf(Color(0xFFA5B4FC), Color(0xFF818CF8)))
                    Text(
                        ch.toString(),
                        fontFamily = InterDisplayFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 42.sp,
                        letterSpacing = (-0.5).sp,
                        color = if (i >= 4) Color.Unspecified else Color(0xFFFAFAFA),
                        style = if (i >= 4) MaterialTheme.typography.displaySmall.copy(
                            brush = gradient,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ) else MaterialTheme.typography.displaySmall.copy(fontSize = 42.sp, fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier
                            .graphicsLayer {
                                translationY = lY.value
                                alpha = lAlpha.value
                            },
                    )
                }
            }

            Text(
                "Exam preparation, done right.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8B8FB8),
                modifier = Modifier
                    .padding(top = 14.dp)
                    .alpha(tagAlpha.value),
            )
        }
    }
}
