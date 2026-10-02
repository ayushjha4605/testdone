package com.testdone.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.testdone.app.domain.logic.greeting
import com.testdone.app.domain.logic.timeAgo
import com.testdone.app.domain.model.TestKind
import com.testdone.app.domain.model.TestMeta
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.LocalMenuOpener
import com.testdone.app.ui.MenuButton
import com.testdone.app.ui.components.AutoSwipingBanner
import com.testdone.app.ui.components.CountUpText
import com.testdone.app.ui.components.ExamLogoBadge
import com.testdone.app.ui.components.SectionHeader
import com.testdone.app.ui.components.ShimmerBox
import com.testdone.app.ui.components.TdAvatar
import com.testdone.app.ui.components.StaggerIn
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.components.cssGradient
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.components.pulse
import com.testdone.app.ui.components.shimmerSweep
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt

@Composable
fun HomeScreen(navController: NavHostController, vm: AppViewModel) {
    val hvm: HomeViewModel = viewModel(factory = com.testdone.app.ui.simpleFactory { HomeViewModel(vm.container) })
    val state by hvm.state.collectAsState()
    val user by vm.user.collectAsState()
    val openMenu = LocalMenuOpener.current

    val listState = rememberLazyListState()
    // hero gradient drifts up at ~45% of scroll speed → soft parallax depth
    val parallax by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset * 0.45f else 900f
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ── fixed gradient hero backdrop (behind the scrolling list) ─────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .graphicsLayer { translationY = -parallax }
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color(0xFF6366F1).copy(alpha = 0.92f),
                            0.55f to Color(0xFF8B5CF6).copy(alpha = 0.38f),
                            1f to Color.Transparent,
                        )
                    )
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)),
                        radius = 340f,
                        center = Offset(size.width * 0.88f, size.height * 0.10f),
                    )
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0xFFD946EF).copy(alpha = 0.14f), Color.Transparent)),
                        radius = 300f,
                        center = Offset(size.width * 0.05f, size.height * 0.55f),
                    )
                }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 122.dp),
        ) {
            // ── header: menu · greeting · avatar ─────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MenuButton(onOpen = openMenu, onGradient = true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        val greetingWord = when (greeting()) {
                            0 -> "Good morning"
                            1 -> "Good afternoon"
                            else -> "Good evening"
                        }
                        Text(
                            "$greetingWord 👋",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                            ),
                            color = Color.White.copy(alpha = 0.82f),
                        )
                        val firstName = user.name.trim().split(" ").firstOrNull() ?: "Future Topper"
                        Text(
                            firstName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // v2.3.17 — header avatar ab user ki asli photo dikhata hai
                    // (pehle sirf [A] initial tha). Photo → perfect circle; agar
                    // photo nahi hai to branded gradient initial. Paid plan pe
                    // photo ke upar gold crown badge rehta hai.
                    Box(
                        Modifier
                            .pressableScale(0.92f) { navController.navigate(Routes.PROFILE) },
                    ) {
                        TdAvatar(
                            name = user.name,
                            avatarUrl = user.avatarUrl,
                            size = 42.dp,
                        )
                        if (user.planActive) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(16.dp)
                                    .background(Color(0xFFFCD34D), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.WorkspacePremium,
                                    contentDescription = "Premium",
                                    tint = Color(0xFF78350F),
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ── content download banner: REMOVED in v2.3.15 ──────────────────
            // The OTA sync still runs silently in the background; content lands
            // with a "New content added" toast. The old always-on banner read
            // as clutter (user feedback: "bohot bekar lagta hai dekhne me").

            // ── streak chip ─────────────────────────────────────────────────
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp).pulse(maxScale = 1.25f),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                if (state.streak > 0) "${state.streak}-day streak" else "Start your streak today",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White.copy(alpha = 0.95f),
                            )
                        }
                    }
                }
            }

            // ── auto-swiping promo banner ───────────────────────────────────
            item {
                val exam = state.selectedExam
                val slides = remember(state.streak, exam, user.planActive) {
                    buildList {
                        add(
                            BannerSlide(
                                eyebrow = "DAILY STREAK",
                                title = if (state.streak > 0) "${state.streak}-day streak!" else "Build a daily habit",
                                subtitle = if (state.streak > 0) "One mock today — keep it alive" else "Practice daily, watch accuracy rise",
                                emoji = "🔥",
                                brush = Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFD946EF))),
                                cta = "Keep going",
                            ) { navController.navigate(Routes.TESTS) }
                        )
                        add(
                            BannerSlide(
                                eyebrow = "PYQ VAULT",
                                title = "${exam?.pyqCount ?: 80} PYQ sets",
                                subtitle = "Previous year papers, solved",
                                emoji = "📜",
                                brush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF38BDF8))),
                                cta = "Practice",
                            ) { navController.navigate(Routes.TESTS) }
                        )
                        add(
                            BannerSlide(
                                eyebrow = "QUESTION BANK",
                                title = exam?.let { "${it.questionCount} questions" } ?: "Practice by topic",
                                subtitle = "Real exam-style questions",
                                emoji = "🧠",
                                brush = Brush.linearGradient(listOf(Color(0xFF0891B2), Color(0xFF10B981))),
                                cta = "Explore",
                            ) { navController.navigate(Routes.QBANK) }
                        )
                        if (!user.planActive) {
                            add(
                                BannerSlide(
                                    eyebrow = "TESTDONE PASS",
                                    title = "Unlock everything",
                                    subtitle = "All mocks & PYQs · just ₹49/mo",
                                    emoji = "⚡",
                                    brush = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFF43F5E))),
                                    cta = "Get Pass",
                                ) { navController.navigate(Routes.plan("testdone-pass")) }
                            )
                        }
                    }
                }
                StaggerIn(0, Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                    AutoSwipingBanner(pages = slides) { slide ->
                        BannerCard(slide)
                    }
                }
            }

            // ── continue / start CTA ────────────────────────────────────────
            item {
                val lastAttempt = state.attempts.firstOrNull()
                val exam = state.selectedExam
                StaggerIn(1, Modifier.padding(top = 8.dp, bottom = 14.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .pressableScale(0.97f) { navController.navigate(Routes.TESTS) }
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                cssGradient(
                                    exam?.gradientCss,
                                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                                )
                            )
                            .shimmerSweep()
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(54.dp)
                                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (lastAttempt != null) "CONTINUE LEARNING" else "GET STARTED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.75f),
                                )
                                Text(
                                    if (lastAttempt != null) lastAttempt.testTitle.ifBlank { "${exam?.shortName ?: ""} Mock Test" }
                                    else "Take your first mock test",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (lastAttempt != null) {
                                        if (lastAttempt.maxScore > 0) "You scored ${lastAttempt.scorePct}% last time" else "Keep going!"
                                    } else {
                                        exam?.let { "${it.mockCount} mocks · ${it.pyqCount} PYQ sets ready" } ?: "Pehla step: exam select karo (upar wala card dabao)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }

            // ── quick stats ─────────────────────────────────────────────────
            item {
                StaggerIn(2) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatMini(Icons.Rounded.LocalFireDepartment, state.streak, "", if (state.streak == 1) "Day" else "Days", Color(0xFFFB923C), Modifier.weight(1f), flame = true)
                        StatMini(Icons.Rounded.TrackChanges, state.attempts.size, "", "Tests", Color(0xFF818CF8), Modifier.weight(1f))
                        StatMini(Icons.Rounded.Insights, state.avgScore, "%", "Avg", Color(0xFF34D399), Modifier.weight(1f))
                        StatMini(Icons.Rounded.EmojiEvents, state.bestScore, "%", "Best", Color(0xFFFBBF24), Modifier.weight(1f))
                    }
                }
            }

            // ── quick actions ───────────────────────────────────────────────
            item {
                StaggerIn(3) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val qaExam = state.selectedExam
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickAction(Icons.Rounded.MenuBook, "QBank", "Real practice Qs", listOf(Color(0x338B5CF6), Color(0x33D946EF)), TdExt.colors.brandBrush, Modifier.weight(1f)) { navController.navigate(Routes.QBANK) }
                            QuickAction(Icons.Rounded.History, "PYQ Papers", qaExam?.let { "${it.pyqCount} sets ready" } ?: "Previous years", listOf(Color(0x33F59E0B), Color(0x33F97316)), TdExt.colors.premiumBrush, Modifier.weight(1f)) { navController.navigate(Routes.TESTS) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickAction(Icons.Rounded.TrackChanges, "Mock Tests", qaExam?.let { "${it.mockCount} tests ready" } ?: "Full length", listOf(Color(0x336366F1), Color(0x333B82F6)), TdExt.colors.brandBrush, Modifier.weight(1f)) { navController.navigate(Routes.TESTS) }
                            QuickAction(Icons.Rounded.QueryStats, "Analytics", "Your insights", listOf(Color(0x3310B981), Color(0x3314B8A6)), Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF14B8A6))), Modifier.weight(1f)) { navController.navigate(Routes.ANALYTICS) }
                        }
                    }
                }
            }

            // ── recommended tests ───────────────────────────────────────────
            item {
                StaggerIn(4) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        SectionHeader("Recommended for you", actionLabel = "View all") { navController.navigate(Routes.TESTS) }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            if (state.recommended.isEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(2) {
                            Column(
                                Modifier
                                    .width(248.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                            ) {
                                ShimmerBox(Modifier.fillMaxWidth().height(62.dp), RoundedCornerShape(0.dp))
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShimmerBox(Modifier.fillMaxWidth(0.9f).height(15.dp), RoundedCornerShape(7.dp))
                                    ShimmerBox(Modifier.fillMaxWidth(0.55f).height(11.dp), RoundedCornerShape(6.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 16.dp),
                    ) {
                        items(state.recommended, key = { it.id }) { test ->
                            RecommendedCard(test) {
                                navController.navigate(Routes.instructions(test.examId, test.id))
                            }
                        }
                    }
                }
            }

            // ── pass CTA (free users) ───────────────────────────────────────
            if (!user.planActive) {
                item {
                    StaggerIn(5) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .pressableScale(0.97f) { navController.navigate(Routes.plan("testdone-pass")) }
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(listOf(Color(0x26F59E0B), Color(0x14EF4444))))
                                .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(18.dp))
                                .padding(14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(42.dp)
                                        .background(TdExt.colors.premiumBrush, RoundedCornerShape(13.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Get TestDone Pass", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Unlock everything for just ₹49/mo", style = MaterialTheme.typography.labelSmall, color = TdExt.colors.warning)
                                }
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TdExt.colors.warning)
                            }
                        }
                    }
                }
            }

            // ── recent attempts ─────────────────────────────────────────────
            if (state.attempts.isNotEmpty()) {
                item {
                    StaggerIn(6) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            SectionHeader("Recent Attempts", actionLabel = "View all") { navController.navigate(Routes.ANALYTICS) }
                            Spacer(Modifier.height(6.dp))
                            TdCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                                Column {
                                    state.attempts.take(4).forEachIndexed { i, a ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .pressableScale(0.98f) { navController.navigate(Routes.report(a.attemptId)) }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(30.dp)
                                                    .background(
                                                        when {
                                                            a.scorePct >= 70 -> TdExt.colors.successDim
                                                            a.scorePct >= 40 -> TdExt.colors.warningDim
                                                            else -> TdExt.colors.dangerDim
                                                        },
                                                        RoundedCornerShape(9.dp),
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    "${a.scorePct}%",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = when {
                                                        a.scorePct >= 70 -> TdExt.colors.success
                                                        a.scorePct >= 40 -> TdExt.colors.warning
                                                        else -> TdExt.colors.danger
                                                    },
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    a.testTitle.ifBlank { "Test" },
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                )
                                                Text(
                                                    "${a.correct} correct · ${a.accuracy.toInt()}% accuracy",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            Text(
                                                timeAgo(a.completedAt ?: a.startedAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            )
                                        }
                                        if (i < state.attempts.take(4).size - 1) {
                                            Box(
                                                Modifier.fillMaxWidth().height(1.dp)
                                                    .background(MaterialTheme.colorScheme.outlineVariant)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── explore exams ───────────────────────────────────────────────
            item {
                StaggerIn(7) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        SectionHeader("Explore other exams", actionLabel = "View all") { navController.navigate(Routes.TESTS) }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 16.dp),
                ) {
                    items(state.otherExams, key = { it.id }) { exam ->
                        Column(
                            Modifier
                                .width(106.dp)
                                .pressableScale(0.93f) {
                                    hvm.selectExam(exam.id)
                                    navController.navigate(Routes.TESTS) {
                                        popUpTo(Routes.HOME)
                                        launchSingleTop = true
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.size(54.dp)) {
                                // gradient disc + real exam emblem (app-icon look)
                                ExamLogoBadge(
                                    examId = exam.id,
                                    size = 54.dp,
                                    ringBrush = examPalette(exam.id),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                exam.shortName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${exam.mockCount + exam.pyqCount} tests",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── pieces ──────────────────────────────────────────────────────────────────

private data class BannerSlide(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val brush: Brush,
    val cta: String,
    val onClick: () -> Unit,
)

@Composable
private fun BannerCard(slide: BannerSlide) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .pressableScale(0.97f, slide.onClick)
            .clip(RoundedCornerShape(24.dp))
            .background(slide.brush)
    ) {
        // decorative glows
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .size(170.dp)
                    .align(Alignment.TopEnd)
                    .graphicsLayer { translationX = 62f; translationY = -58f }
                    .background(Brush.radialGradient(listOf(Color.White.copy(0.17f), Color.Transparent)), CircleShape)
            )
            Box(
                Modifier
                    .size(130.dp)
                    .align(Alignment.BottomStart)
                    .graphicsLayer { translationX = -46f; translationY = 26f }
                    .background(Brush.radialGradient(listOf(Color.White.copy(0.10f), Color.Transparent)), CircleShape)
            )
        }
        Row(
            Modifier.matchParentSize().padding(16.dp).padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(slide.emoji, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    slide.eyebrow,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    ),
                    color = Color.White.copy(alpha = 0.75f),
                )
                Text(
                    slide.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    slide.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .background(Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    slide.cta,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF4338CA),
                )
            }
        }
    }
}

private val EXAM_PALETTES = listOf(
    Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF3B82F6))),
    Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFD946EF))),
    Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFF97316))),
    Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF14B8A6))),
    Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFF43F5E))),
    Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8))),
)

private fun examPalette(examId: String): Brush {
    val idx = kotlin.math.abs(examId.hashCode()) % EXAM_PALETTES.size
    return EXAM_PALETTES[idx]
}

@Composable
private fun StatMini(
    icon: ImageVector,
    value: Int,
    suffix: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    flame: Boolean = false,
) {
    TdCard(modifier, contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp), elevation = 4.dp) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(tint.copy(alpha = 0.14f), RoundedCornerShape(9.dp))
                    .let { if (flame) it.pulse(maxScale = 1.15f) else it },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                CountUpText(
                    value = value,
                    suffix = suffix,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    bgColors: List<Color>,
    iconBrush: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .pressableScale(0.96f, onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(bgColors))
            .padding(14.dp),
    ) {
        Column {
            Box(
                Modifier
                    .size(40.dp)
                    .background(iconBrush, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RecommendedCard(test: TestMeta, onClick: () -> Unit) {
    val pyq = test.kind == TestKind.PYQ
    Column(
        Modifier
            .width(248.dp)
            .pressableScale(0.95f, onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, TdExt.colors.cardBorder, RoundedCornerShape(20.dp)),
    ) {
        // gradient cover band
        Box(Modifier.fillMaxWidth().height(62.dp).background(if (pyq) TdExt.colors.premiumBrush else TdExt.colors.brandBrush)) {
            Icon(
                if (pyq) Icons.Rounded.History else Icons.Rounded.Bolt,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.30f),
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            )
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    if (pyq) "PYQ SET" else "MOCK TEST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    ),
                    color = Color.White,
                )
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                test.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetaChip(Icons.Rounded.Schedule, "${test.durationMin}m")
                MetaChip(Icons.Rounded.Quiz, "${test.totalQuestions}")
                MetaChip(Icons.Rounded.EmojiEvents, "${test.maxMarks.toInt()}")
            }
        }
    }
}

@Composable
private fun MetaChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
