package com.testdone.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.testdone.app.domain.model.ProfilePatch
import com.testdone.app.di.SessionCoordinator
import com.testdone.app.domain.logic.computeStreak
import com.testdone.app.domain.model.PlanId
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.AnimatedProgressRing
import com.testdone.app.ui.components.ExamLogoBadge
import com.testdone.app.ui.components.CountUpText
import com.testdone.app.ui.components.PopIn
import com.testdone.app.ui.components.StaggerIn
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.components.pulse
import com.testdone.app.ui.components.rememberAvatarBitmap
import com.testdone.app.ui.components.rememberGalleryAvatarPicker
import kotlinx.coroutines.launch
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val DAY_MS = 86_400_000L

/** Profile page: identity cover, stats, weekly goal, achievements, quick links. */
@Composable
fun ProfileScreen(navController: NavHostController, vm: AppViewModel) {
    val user by vm.user.collectAsState()
    var showEdit by remember { mutableStateOf(false) }
    var showAvatarSheet by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val attempts = vm.container.attemptRepository.observeAttempts()
        .collectAsState(initial = emptyList()).value
    val exam = user.selectedExamId?.let { vm.container.contentRepository.examById(it) }

    // gallery picker — system document picker, no permission needed
    val photoPicker = rememberGalleryAvatarPicker { uri ->
        scope.launch {
            val dataUri = com.testdone.app.ui.components.Avatars.toCompactDataUri(context, uri)
            if (dataUri != null) {
                // local first (instant), then cloud (survives logout/reinstall)
                vm.container.settings.updateUser { it.copy(avatarUrl = dataUri) }
                vm.updateCloudProfile(com.testdone.app.domain.model.ProfilePatch(avatarUrl = dataUri))
                vm.container.sessionCoordinator.toast(
                    "Photo saved",
                    "Har login ke baad yahi photo rahegi",
                    com.testdone.app.di.SessionCoordinator.Toast.Kind.SUCCESS,
                )
            } else {
                vm.container.sessionCoordinator.toast("Photo set nahi ho payi", "Dobara try karo — chhoti image lo")
            }
        }
    }

    val streak = remember(attempts) { computeStreak(attempts.map { it.startedAt }) }
    val activeDays = remember(attempts) { attempts.map { it.startedAt / DAY_MS }.toSet() }
    val today = remember { System.currentTimeMillis() / DAY_MS }
    val weekActive = remember(attempts, today) {
        (0..6).count { back -> activeDays.contains(today - back) }
    }
    val weekGoal = 5
    val weekProgress = (weekActive.toFloat() / weekGoal).coerceIn(0f, 1f)

    val total = attempts.size
    val avg = if (attempts.isEmpty()) 0 else attempts.map { it.scorePct }.average().toInt()
    val best = attempts.maxOfOrNull { it.scorePct } ?: 0

    // v2.3.16 — REAL member date: cloud createdAt → login seed → first attempt → today.
    // Never renders "Jan 1970".
    val memberSince = remember(user.joinedAt, attempts) {
        user.joinedAt.takeIf { it > 0L }
            ?: attempts.minOfOrNull { it.startedAt }?.takeIf { it > 0L }
            ?: System.currentTimeMillis()
    }

    val badges = remember(total, best, streak) {
        listOf(
            Badge("🎯", "First Mock", total >= 1),
            Badge("🔥", "5 Tests", total >= 5),
            Badge("🏆", "70% Club", best >= 70),
            Badge("🎖️", "10 Tests", total >= 10),
            Badge("💎", "3-Day Streak", streak >= 3),
        )
    }

    // edit-profile dialog (name + phone; email is the login identity, not editable here)
    if (showEdit) {
        EditProfileDialog(
            initialName = user.name,
            initialPhone = user.phone,
            onDismiss = { showEdit = false },
            onSave = { newName, newPhone ->
                vm.container.settings.updateUser {
                    it.copy(name = newName, phone = newPhone)
                }
                // mirror to Firestore when logged in (no-op otherwise)
                vm.updateCloudProfile(
                    ProfilePatch(
                        fullName = newName.ifBlank { null },
                        phone = newPhone.ifBlank { null },
                    )
                )
                vm.container.sessionCoordinator.toast("Profile updated", null, SessionCoordinator.Toast.Kind.SUCCESS)
                showEdit = false
            },
        )
    }

    // v2.3.16 — avatar/name chooser: gallery photo or edit name & phone
    if (showAvatarSheet) {
        AvatarSheet(
            hasPhoto = !user.avatarUrl.isNullOrBlank(),
            onDismiss = { showAvatarSheet = false },
            onPickPhoto = {
                showAvatarSheet = false
                photoPicker.launch("image/*")
            },
            onEditInfo = {
                showAvatarSheet = false
                showEdit = true
            },
            onRemovePhoto = {
                showAvatarSheet = false
                vm.container.settings.updateUser { it.copy(avatarUrl = null) }
                vm.updateCloudProfile(com.testdone.app.domain.model.ProfilePatch(avatarUrl = ""))
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── cover + identity ─────────────────────────────────────────────
        // Both children stack from the top of a wrapping Box: the cover is a
        // fixed-height gradient, the identity Column starts 190dp down so the
        // avatar straddles the cover edge. This is REAL layout — the Box grows
        // to fit all identity content, so nothing bleeds onto the sections
        // below. (The previous version used offset(), which only moves the
        // drawing and made the stats row overlap the weekly-goal card.)
        Box(Modifier.fillMaxWidth()) {
            // Cover — clipped to its own bounds so the decorative glows can
            // never bleed outside the gradient (previously the bottom-left
            // glow spilled 40dp past the left edge and read as an overlay).
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clipToBounds()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF6366F1),
                            1f to Color(0xFF8B5CF6),
                        )
                    )
            ) {
                // decorative glow — ONE soft highlight top-right only,
                // contained by the clip above (no left-side blob).
                Box(
                    Modifier
                        .size(170.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 48.dp, y = (-46).dp)
                        .background(
                            Brush.radialGradient(listOf(Color.White.copy(0.10f), Color.Transparent)),
                            CircleShape,
                        )
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }

            // identity block — starts 190dp down, avatar straddles the cover edge
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 190.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PopIn {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        // v2.3.16 — real gallery photo when set, else the gradient initial
                        val avatarBitmap by rememberAvatarBitmap(user.avatarUrl)
                        Box(
                            Modifier
                                .size(84.dp)
                                .background(TdExt.colors.brandBrush, CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            val bmp = avatarBitmap
                            if (bmp != null) {
                                // v2.3.17 — photo circular clip (pehle square bitmap
                                // circle-gradient ke upar draw hota tha)
                                androidx.compose.foundation.Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Profile photo",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape),
                                )
                            } else {
                                Text(
                                    user.name.trim().firstOrNull()?.uppercase() ?: "T",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                        // edit badge — pencil chip opens the photo/name sheet
                        Box(
                            Modifier
                                .offset(x = 2.dp, y = 2.dp)
                                .size(28.dp)
                                .background(TdExt.colors.brandBrush, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .pressableScale(0.88f) { showAvatarSheet = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Create,
                                contentDescription = "Edit profile",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    user.name.ifBlank { "Future Topper" },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Text(
                    user.email.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(8.dp))
                if (user.planActive) {
                    Row(
                        Modifier
                            .background(TdExt.colors.warningDim, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = TdExt.colors.warning,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (user.plan == PlanId.ULTRA) "ULTRA PASS" else "PASS ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = TdExt.colors.warning,
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "FREE PLAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Member since ${
                        Instant.ofEpochMilli(memberSince).atZone(ZoneId.systemDefault())
                            .toLocalDate().format(DateTimeFormatter.ofPattern("MMM yyyy"))
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // stats (v2.3.15 — entrance cascade continues into the stat cards)
                Spacer(Modifier.height(14.dp))
                StaggerIn(0) {
                    Row(
                        Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatCard(Icons.Rounded.TrackChanges, total, "Tests taken", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatCard(Icons.Rounded.QueryStats, avg, "Avg score %", TdExt.colors.success, Modifier.weight(1f))
                        StatCard(Icons.Rounded.EmojiEvents, best, "Best score %", TdExt.colors.warning, Modifier.weight(1f))
                    }
                }
            }
        }

        // ── weekly goal ring ────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        StaggerIn(0) {
            TdCard(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                elevation = 4.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedProgressRing(
                            progress = weekProgress,
                            modifier = Modifier.size(68.dp),
                            stroke = 8.dp,
                            brush = TdExt.colors.brandBrush,
                        )
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(26.dp).pulse(maxScale = 1.2f),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("This week's goal", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "$weekActive of $weekGoal active days · ${streak}-day streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (6 downTo 0).forEach { back ->
                                val active = activeDays.contains(today - back)
                                Box(
                                    Modifier
                                        .size(9.dp)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape,
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── achievements ────────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        StaggerIn(1) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(badges) { badge ->
                        BadgeTile(badge)
                    }
                }
            }
        }

        // ── target exam ─────────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        if (exam != null) {
            StaggerIn(2) {
                TdCard(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    elevation = 4.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExamLogoBadge(examId = exam.id, size = 50.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Target Exam", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                exam.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${exam.questionCount} questions · ${exam.mockCount} mocks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "Change",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .pressableScale(0.95f) { navController.navigate(Routes.TESTS) }
                                .padding(6.dp),
                        )
                    }
                }
            }
        }

        // ── quick links ─────────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        StaggerIn(3) {
            TdCard(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp),
                elevation = 4.dp,
            ) {
                Column {
                    ProfileLink(Icons.Rounded.Forum, "Doubts Community", "Ask & answer questions", Color(0xFF818CF8)) { navController.navigate(Routes.DOUBTS) }
                    ProfileLink(Icons.Rounded.Create, "Submit a Question", "Help grow the question bank", Color(0xFF34D399)) { navController.navigate(Routes.SUBMIT) }
                    ProfileLink(Icons.Rounded.Lock, "Change Password", "Update your password", Color(0xFFFBBF24)) { navController.navigate(Routes.CHANGE_PASSWORD) }
                    ProfileLink(Icons.Rounded.Settings, "Settings", "Theme, language, sync", Color(0xFF38BDF8)) { navController.navigate(Routes.SETTINGS) }
                }
            }
        }
        Spacer(Modifier.height(130.dp)) // clearance for the bottom nav bar
    }
}

// ── pieces ──────────────────────────────────────────────────────────────────

private data class Badge(val emoji: String, val label: String, val unlocked: Boolean)

/** v2.3.16 — avatar chooser: gallery photo, edit name & phone, remove photo. */
@Composable
private fun AvatarSheet(
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onEditInfo: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Text("Profile photo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                "Gallery se photo choose karo — har login ke baad yahi rahegi",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            SheetRow(icon = Icons.Rounded.PhotoLibrary, title = "Choose from gallery", tint = MaterialTheme.colorScheme.primary, onClick = onPickPhoto)
            SheetRow(icon = Icons.Rounded.Create, title = "Edit name & phone", tint = TdExt.colors.success, onClick = onEditInfo)
            if (hasPhoto) {
                SheetRow(icon = Icons.Rounded.DeleteOutline, title = "Remove photo", tint = TdExt.colors.danger, onClick = onRemovePhoto)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressableScale(0.97f, onDismiss)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Close", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(0.98f, onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Edit profile dialog — full name (required) + phone (optional). */
@Composable
private fun EditProfileDialog(
    initialName: String,
    initialPhone: String,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    val nameValid = name.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full name") },
                    singleLine = true,
                    isError = !nameValid,
                    supportingText = if (!nameValid) {
                        { Text("Name can't be empty") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), phone.trim()) },
                enabled = nameValid,
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun BadgeTile(badge: Badge) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp),
    ) {
        PopIn(delayMs = 90) {
            val badgeBg = if (badge.unlocked) TdExt.colors.brandBrush
            else Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                )
            )
            Box(
                Modifier
                    .size(54.dp)
                    .background(badgeBg, CircleShape)
                    .let {
                        if (badge.unlocked) it.border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape) else it
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    badge.emoji,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (badge.unlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            badge.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (badge.unlocked) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (badge.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    // v2.3.16 — fully centered stat: icon, big number and a 2-line label that
    // can never overflow/clip inside the narrow third-width card.
    TdCard(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp, horizontal = 6.dp), elevation = 6.dp) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .background(tint.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            CountUpText(
                value = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProfileLink(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(0.98f, onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp),
        )
    }
}
