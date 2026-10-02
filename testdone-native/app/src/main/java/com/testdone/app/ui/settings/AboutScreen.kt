package com.testdone.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.testdone.app.R
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.CountUpText
import com.testdone.app.ui.components.StaggerIn
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt

/**
 * v2.3.15 — full About screen. Reached from the menu drawer's "About" item
 * (previously a bare toast that read as "nothing happens on tap") and from
 * Settings → About → "TestDone". Everything opens in-app; support uses the
 * mail composer, share uses the system sheet.
 */
@Composable
fun AboutScreen(navController: NavHostController, vm: AppViewModel) {
    val context = LocalContext.current
    val exams by vm.container.contentRepository.exams.collectAsState()

    val totalQuestions = exams.sumOf { it.questionCount }
    val totalMocks = exams.sumOf { it.mockCount }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Column {
                Text("About", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("TestDone — exam prep companion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── hero ───────────────────────────────────────────────────────────
        StaggerIn(0) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                        ),
                        RoundedCornerShape(26.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.logo_full),
                        contentDescription = "TestDone logo",
                        modifier = Modifier.size(84.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("TestDone", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Mock tests · PYQs · Analytics · Community",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        Text(
                            "v2.3.16 · build 20023",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // ── live content stats ────────────────────────────────────────────
        StaggerIn(1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AboutStat(Icons.Rounded.School, exams.size, "Exams", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                AboutStat(Icons.Rounded.Book, totalQuestions, "Questions", TdExt.colors.success, Modifier.weight(1f))
                AboutStat(Icons.Rounded.QueryStats, totalMocks, "Mock tests", TdExt.colors.warning, Modifier.weight(1f))
            }
        }

        // ── what's inside ─────────────────────────────────────────────────
        StaggerIn(2) {
            TdCard(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                Column {
                    Text("What's inside", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    AboutBullet("Full mock tests aur previous-year questions — 18 exams ke liye, ek hi app me.")
                    AboutBullet("Instant detailed report har test ke baad: accuracy, time, subject-wise breakdown.")
                    AboutBullet("Community Doubts — sawaal poocho, jawab do; approved questions question bank me chalte hain.")
                    AboutBullet("OTA content updates — naye questions internet se aate hain, phir offline chalte hain.")
                    AboutBullet("Dark mode, haptics aur app sounds — jo pasand hai wahi rakho.")
                }
            }
        }

        // ── quick links ───────────────────────────────────────────────────
        StaggerIn(3) {
            TdCard(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp),
            ) {
                Column {
                    AboutLink(Icons.Rounded.PrivacyTip, "Privacy Policy", "In-app · no external site") {
                        navController.navigate(Routes.PRIVACY) { launchSingleTop = true }
                    }
                    AboutLink(Icons.Rounded.Share, "Share TestDone", "Spread the word") {
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "TestDone — India ka exam prep app. Mock tests, PYQs aur analytics, sab free:\nhttps://play.google.com/store/apps/details?id=com.testdone.app",
                                        )
                                    },
                                    "Share TestDone",
                                ),
                            )
                        }
                    }
                    AboutLink(Icons.Rounded.Mail, "Contact support", "testdoneadmin@gmail.com") {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:testdoneadmin@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "TestDone v2.3.16 — support")
                                },
                            )
                        }
                    }
                }
            }
        }

        // v2.3.16 — "Recent changes" card removed (user request); the About
        // screen closes on the quick links + made-in-india footer.

        Spacer(Modifier.height(10.dp))
        Text(
            "Made in India for Indian exam aspirants",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            "© 2026 TestDone",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(110.dp)) // clearance for the bottom nav bar
    }
}

@Composable
private fun AboutStat(icon: ImageVector, value: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(tint.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(8.dp))
            CountUpText(
                value = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AboutBullet(text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("•  ", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AboutLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(0.98f, onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp),
        )
    }
}
