package com.testdone.app.ui.ultra

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.testdone.app.R
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.LocalMenuOpener
import com.testdone.app.ui.MenuButton
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.InterDisplayFamily
import com.testdone.app.ui.theme.TdExt
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SupportAgent

/** Ultra tab — premium "coming soon" marketing page. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UltraScreen(navController: NavHostController, vm: AppViewModel) {
    val openMenu = LocalMenuOpener.current
    var email by rememberSaveable { mutableStateOf("") }
    var notified by rememberSaveable { mutableStateOf(false) }
    var emailError by rememberSaveable { mutableStateOf(false) }

    // v2.3.17 — REAL keyboard fix. imePadding viewport chhota karta hai, lekin
    // scroll-position apne aap nahi hilta tha — isliye email field keyboard ke
    // neeche chhupa reh jata tha. Ab IME dikhte hi list bottom tak scroll hoti
    // hai (email field last content hai) → field hamesha keyboard ke upar.
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible) scrollState.animateScrollTo(scrollState.maxValue)
    }

    val pulse = rememberInfiniteTransition(label = "ultraPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.85f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "ultraGlow",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            // v2.3.16 — viewport keyboard ke upar chadhta hai
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            MenuButton(onOpen = openMenu)
        }

        Spacer(Modifier.height(20.dp))

        // pulsing logo
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = glow
                        scaleY = glow
                        alpha = 0.5f
                    }
                    .background(
                        Brush.radialGradient(listOf(Color(0x668B5CF6), Color.Transparent)),
                    )
            )
            Box(
                Modifier
                    .size(100.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFD946EF), Color(0xFFEC4899))),
                        RoundedCornerShape(28.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_glyph),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .background(Color(0xFFFCD34D), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    "PREMIUM",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color.Black,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
            Text(
                "COMING SOON",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("TestDone ", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Ultra",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = InterDisplayFamily,
                color = Color(0xFFA78BFA),
            )
        }
        Text(
            "The premium tier for toppers — everything in Pass, plus the tools that give you the edge.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
        )

        // features
        listOf(
            Triple("24/7 AI Doubt Tutor", "Instant, unlimited doubt-solving — like a personal teacher in your pocket", Icons.Rounded.AutoAwesome),
            Triple("HD Video Lessons", "Topic-wise classes by India's top faculty (coming soon)", Icons.Rounded.PlayCircle),
            Triple("Live Tournaments", "Compete live with thousands of aspirants and win rewards", Icons.Rounded.EmojiEvents),
            Triple("Handwritten Notes", "Premium topper notes for every chapter", Icons.Rounded.MenuBook),
            Triple("Priority Support", "Your doubts answered first, always", Icons.Rounded.SupportAgent),
        ).forEach { (title, desc, icon) ->
            UltraFeature(icon, title, desc)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(22.dp))

        if (notified) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(TdExt.colors.successDim, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = TdExt.colors.success, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("You're on the list! We'll notify you at launch.", style = MaterialTheme.typography.labelLarge, color = TdExt.colors.success)
                }
            }
        } else {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError) emailError = false
                },
                placeholder = { Text("Email for early access", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                isError = emailError,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                supportingText = if (emailError) {
                    { Text("Valid email daalo — jaise name@example.com", style = MaterialTheme.typography.labelSmall) }
                } else null,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            TdButton(
                text = "Get early access",
                onClick = {
                    if (android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                        emailError = false
                        notified = true
                        // v2.3.17 — email ab Firestore waitlist mein jaata hai
                        // (ultra_waitlist/{uid}). Owner Moderate → Waitlist tab
                        // mein dekh sakta hai. Fire-and-forget: offline/fail pe
                        // user ko koi error nahi — local state phir bhi saved.
                        val submitted = email.trim()
                        scope.launch {
                            vm.container.communityRepository.joinUltraWaitlist(submitted)
                        }
                    } else {
                        emailError = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Already have Pass? You'll be upgraded automatically at launch.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun UltraFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0x2E8B5CF6), Color(0x2ED946EF))),
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}
