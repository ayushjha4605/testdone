package com.testdone.app.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.testdone.app.data.repository.CommunityRepository
import com.testdone.app.domain.model.PlanId
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.rememberAvatarBitmap
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.components.TdAvatar
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt
import androidx.compose.runtime.collectAsState

/**
 * v2.3.16 — premium navigation drawer (redesigned to the reference mock).
 *
 * • Panel: rounded right edge, ambient brand glows (subtle in light, richer in dark)
 * • Header: 56dp squircle avatar (real photo when set), verified chip for paid plans
 * • Gold-bordered "Upgrade to TestDone Pass" promo card with a 4-feature strip
 * • Menu rows: individual rounded cards with gradient icon squircles + subtitles
 * • Cascade entrance: each card slides in a beat after the panel (professional feel)
 * • Works in BOTH themes — every color resolves from MaterialTheme + TdExt
 */
@Composable
fun MenuDrawer(
    navController: NavHostController,
    vm: AppViewModel,
    open: Boolean,
    onClose: () -> Unit,
) {
    val activityRef = androidx.compose.ui.platform.LocalContext.current as? Activity
    val user by vm.user.collectAsState()

    // Drives both layers. MutableTransitionState keeps the exit animation
    // alive after `open` flips false.
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(open) { visibleState.targetState = open }

    BackHandler(enabled = open) { onClose() }

    if (visibleState.targetState || visibleState.currentState) {
        Box(Modifier.fillMaxSize()) {
            // scrim — fade only
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(240)),
                exit = fadeOut(tween(220)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.62f))
                        .clickable { onClose() },
                )
            }

            // panel — slides from the left with rounded right edge + glows
            AnimatedVisibility(
                visibleState = visibleState,
                enter = slideInHorizontally(
                    animationSpec = tween(340, easing = FastOutSlowInEasing),
                ) { -it } + fadeIn(tween(220)),
                exit = slideOutHorizontally(
                    animationSpec = tween(260, easing = FastOutLinearInEasing),
                ) { -it },
            ) {
                val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val panelShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.86f)
                        .clip(panelShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // ambient glows — contained by the panel clip
                    Box(
                        Modifier
                            .size(280.dp)
                            .offset(x = (-90).dp, y = (-70).dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF6366F1).copy(alpha = if (dark) 0.16f else 0.05f), Color.Transparent)
                                ),
                                CircleShape,
                            )
                    )
                    Box(
                        Modifier
                            .size(320.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 90.dp, y = 110.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF8B5CF6).copy(alpha = if (dark) 0.13f else 0.04f), Color.Transparent)
                                ),
                                CircleShape,
                            )
                    )
                    Box(
                        Modifier
                            .size(240.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-80).dp, y = 60.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFF59E0B).copy(alpha = if (dark) 0.10f else 0.04f), Color.Transparent)
                                ),
                                CircleShape,
                            )
                    )

                    Column(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        DrawerContent(navController, vm, activityRef, user, dark, onClose)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    navController: NavHostController,
    vm: AppViewModel,
    activityRef: Activity?,
    user: com.testdone.app.domain.model.UserProfile,
    dark: Boolean,
    onClose: () -> Unit,
) {
    val admin = CommunityRepository.isAdmin(user.email)

    DrawerStagger(0) {
        DrawerHeader(user = user, dark = dark, onOpenProfile = {
            navController.navigate(Routes.PROFILE) { launchSingleTop = true }; onClose()
        }, onClose = onClose)
    }

    Spacer(Modifier.height(14.dp))

    // plan state: active members see a status card, free users the gold promo
    if (user.planActive) {
        DrawerStagger(1) { PlanCardActive(user.plan, user.planExpiry, dark) { navigatePlan(navController, onClose) } }
    } else {
        DrawerStagger(1) { UpgradeCard(dark) { navigatePlan(navController, onClose) } }
    }

    Spacer(Modifier.height(16.dp))
    SectionLabel("GENERAL")
    Spacer(Modifier.height(8.dp))

    var i = 2
    DrawerItem(Icons.Rounded.Person, "Profile", "Your account & stats", "#3B82F6" to "#8B5CF6", stagger = i++) {
        navController.navigate(Routes.PROFILE) { launchSingleTop = true }; onClose()
    }
    DrawerItem(Icons.Rounded.WorkspacePremium, "Plans", "TestDone Pass & Ultra", "#F59E0B" to "#D97706",
        badge = "Best Value", stagger = i++) { navigatePlan(navController, onClose) }
    DrawerItem(Icons.Rounded.QuestionAnswer, "Doubts", "Ask the community", "#10B981" to "#059669", stagger = i++) {
        navController.navigate(Routes.DOUBTS) { launchSingleTop = true }; onClose()
    }
    DrawerItem(Icons.Rounded.RateReview, "Submit a Question", "Contribute to the bank", "#3B82F6" to "#2563EB", stagger = i++) {
        navController.navigate(Routes.SUBMIT) { launchSingleTop = true }; onClose()
    }
    DrawerItem(Icons.Rounded.Lock, "Change Password", "Account security", "#EF4444" to "#DC2626", stagger = i++) {
        navController.navigate(Routes.CHANGE_PASSWORD) { launchSingleTop = true }; onClose()
    }
    DrawerItem(Icons.Rounded.DarkMode, "Appearance", "Theme & language", "#8B5CF6" to "#7C3AED", stagger = i++) {
        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }; onClose()
    }

    Spacer(Modifier.height(18.dp))
    SectionLabel("PREFERENCES")
    Spacer(Modifier.height(8.dp))

    DrawerItem(Icons.Rounded.Settings, "Settings", "Preferences & sync", "#64748B" to "#475569", stagger = i++) {
        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }; onClose()
    }
    DrawerItem(Icons.Rounded.PrivacyTip, "Privacy Policy", "How we handle data", "#94A3B8" to "#64748B", stagger = i++) {
        navController.navigate(Routes.PRIVACY) { launchSingleTop = true }; onClose()
    }
    DrawerItem(Icons.AutoMirrored.Rounded.HelpOutline, "About", "TestDone v2.3.16 · India ka exam app", "#0EA5E9" to "#0284C7", stagger = i++) {
        navController.navigate(Routes.ABOUT) { launchSingleTop = true }; onClose()
    }

    // admin-only moderation entry (hidden from regular users)
    if (admin) {
        DrawerItem(Icons.Rounded.AdminPanelSettings, "Moderate", "Admin · doubts & questions", "#F59E0B" to "#EA580C", stagger = i++) {
            navController.navigate(Routes.ADMIN) { launchSingleTop = true }; onClose()
        }
    }

    Spacer(Modifier.height(20.dp))

    DrawerStagger(i) {
        // logout — destructive card, red tint in both themes
        Row(
            Modifier
                .fillMaxWidth()
                .pressableScale(0.97f) {
                    onClose()
                    vm.logout(activityRef) { }
                }
                .background(TdExt.colors.danger.copy(alpha = if (dark) 0.16f else 0.10f), RoundedCornerShape(16.dp))
                .border(1.dp, TdExt.colors.danger.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = TdExt.colors.danger, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Logout", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = TdExt.colors.danger)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = TdExt.colors.danger.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }

    Spacer(Modifier.height(24.dp))
}

private fun navigatePlan(navController: NavHostController, onClose: () -> Unit) {
    navController.navigate(Routes.plan("testdone-pass")) { launchSingleTop = true }
    onClose()
}

/** One menu card's entrance: slides in from the left a beat after the panel. */
@Composable
private fun DrawerStagger(index: Int, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    val delay = (index * 45).coerceAtMost(480)
    androidx.compose.animation.AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(300, delayMillis = delay)) +
            slideInHorizontally(
                animationSpec = tween(380, delayMillis = delay, easing = FastOutSlowInEasing),
                initialOffsetX = { -it / 7 },
            ),
        exit = fadeOut(tween(120)),
    ) { content() }
}

// ── header ───────────────────────────────────────────────────────────────────

@Composable
private fun DrawerHeader(
    user: com.testdone.app.domain.model.UserProfile,
    dark: Boolean,
    onOpenProfile: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // avatar + name block — tapping opens the profile page
        Row(
            Modifier
                .weight(1f)
                .pressableScale(0.98f, onOpenProfile),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                TdAvatar(
                    name = user.name,
                    avatarUrl = user.avatarUrl,
                    size = 56.dp,
                    shape = RoundedCornerShape(18.dp),
                    brush = TdExt.colors.brandBrush,
                )
                // crown chip for paid members
                if (user.planActive) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(20.dp)
                            .background(TdExt.colors.premiumBrush, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.name.ifBlank { "Future Topper" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (user.planActive) {
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = TdExt.colors.warning,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    user.email.ifBlank { "Not signed in" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (user.planActive) {
                        Chip(
                            text = if (user.plan == PlanId.ULTRA) "ULTRA PASS" else "PASS ACTIVE",
                            fg = Color(0xFFFBBF24),
                            bg = Color(0xFFF59E0B).copy(alpha = if (dark) 0.22f else 0.14f),
                        )
                    } else {
                        Chip(
                            text = "FREE PLAN",
                            fg = MaterialTheme.colorScheme.onSurfaceVariant,
                            bg = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    }
                    Chip(
                        text = "EARLY ACCESS",
                        fg = Color(0xFFA78BFA),
                        bg = Color(0xFF8B5CF6).copy(alpha = if (dark) 0.22f else 0.12f),
                    )
                }
            }
        }
        // close button
        Box(
            Modifier
                .size(34.dp)
                .pressableScale(0.9f, onClose)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Chip(text: String, fg: Color, bg: Color) {
    Box(
        Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, fontSize = 9.sp),
            color = fg,
            maxLines = 1,
        )
    }
}

// ── promo card ───────────────────────────────────────────────────────────────

/** Gold-bordered "Upgrade to TestDone Pass" card — the drawer's hero. */
@Composable
private fun UpgradeCard(dark: Boolean, onClick: () -> Unit) {
    val goldBorder = Brush.linearGradient(listOf(Color(0xFFFCD34D), Color(0xFFF97316), Color(0xFFD97706)))
    val cardBg = if (dark) Color(0xFF1F2937) else Color(0xFFFFFBEB)
    val titleOn = MaterialTheme.colorScheme.onSurface
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    val goldText = if (dark) Color(0xFFFBBF24) else Color(0xFFB45309)
    val featureIconBg = if (dark) Color(0xFF451A03) else Color(0xFFFEF3C7)

    Box(
        Modifier
            .fillMaxWidth()
            .pressableScale(0.98f, onClick)
            .background(cardBg, RoundedCornerShape(20.dp))
            .border(1.5.dp, goldBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "UPGRADE TO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp,
                            fontSize = 10.sp,
                        ),
                        color = goldText,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("TestDone ", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = titleOn)
                        Text("Pass", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = goldText)
                    }
                    Text("Unlimited mock tests & analytics", style = MaterialTheme.typography.labelMedium, color = sub)
                }
                Spacer(Modifier.width(8.dp))
                // circular arrow — gold ring
                Box(
                    Modifier
                        .size(34.dp)
                        .border(1.5.dp, goldText.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = goldText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // 4-feature strip
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                PromoFeature(Icons.Rounded.AllInclusive, "Unlimited Tests", featureIconBg, goldText, Modifier.weight(1f))
                PromoFeature(Icons.Rounded.Insights, "Advanced Analytics", featureIconBg, goldText, Modifier.weight(1f))
                PromoFeature(Icons.Rounded.Description, "Detailed Solutions", featureIconBg, goldText, Modifier.weight(1f))
                PromoFeature(Icons.Rounded.Bolt, "Ad-free Experience", featureIconBg, goldText, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PromoFeature(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconBg: Color,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(36.dp)
                .background(iconBg, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 10.sp, fontWeight = FontWeight.Medium),
            color = tint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlanCardActive(plan: PlanId, expiry: Long?, dark: Boolean, onClick: () -> Unit) {
    val label = if (plan == PlanId.ULTRA) "Ultra Pass Active" else "Pass Active"
    val expires = expiry?.let {
        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
    val goldBorder = Brush.linearGradient(listOf(Color(0xFFFCD34D), Color(0xFFF97316)))
    val cardBg = if (dark) Color(0xFF1F2937) else Color(0xFFFFFBEB)
    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(0.98f, onClick)
            .background(cardBg, RoundedCornerShape(18.dp))
            .border(1.5.dp, goldBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = TdExt.colors.warning, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            if (expires != null) {
                Text("Expires $expires", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TdExt.colors.warning.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
        )
    }
}

// ── menu rows ────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            fontSize = 10.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}

/** A menu card: gradient icon squircle + title/subtitle + optional badge. */
@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    gradient: Pair<String, String>,
    badge: String? = null,
    stagger: Int = 0,
    onClick: () -> Unit,
) {
    DrawerStagger(stagger) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .pressableScale(0.98f, onClick)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(android.graphics.Color.parseColor(gradient.first)), Color(android.graphics.Color.parseColor(gradient.second)))),
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (badge != null) {
                Box(
                    Modifier
                        .background(TdExt.colors.premiumBrush, RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = Color.White,
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
