package com.testdone.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.ui.unit.sp

/** Settings: theme, language, vibration, sync, account, about. */
@Composable
fun SettingsScreen(navController: NavHostController, vm: AppViewModel) {
    val activityRef = LocalContext.current as? Activity
    val theme by vm.theme.collectAsState()
    val language by vm.language.collectAsState()
    val vibration by vm.container.settings.vibration.collectAsState(initial = true)
    val sounds by vm.container.settings.sounds.collectAsState(initial = true)
    val user by vm.user.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var syncing by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    // v2.3.15 — About-section dialogs: the three rows used to have empty
    // onClick handlers ({}), so tapping them did literally nothing.
    var showVersionDialog by remember { mutableStateOf(false) }
    var showContentDialog by remember { mutableStateOf(false) }
    if (showVersionDialog) {
        VersionInfoDialog(
            onCopy = {
                clipboard.setText(AnnotatedString("TestDone v2.3.16"))
                vm.container.sessionCoordinator.toast("Copied", "Version info clipboard me copy ho gaya", com.testdone.app.di.SessionCoordinator.Toast.Kind.SUCCESS)
            },
            onDismiss = { showVersionDialog = false },
        )
    }
    if (showContentDialog) {
        ContentInfoDialog(
            vm = vm,
            onCheckUpdates = {
                showContentDialog = false
                scope.launch {
                    syncing = true
                    val result = vm.container.sessionCoordinator.manualContentCheck()
                    syncing = false
                    if (result == null) {
                        vm.container.sessionCoordinator.toast("You're up to date", "All exam packs are latest")
                    } else {
                        vm.container.sessionCoordinator.toast(
                            "New content added",
                            "${result.updatedExams.size} exam(s) updated · +${result.addedQuestions} questions · works offline",
                            com.testdone.app.di.SessionCoordinator.Toast.Kind.SUCCESS,
                        )
                    }
                }
            },
            onDismiss = { showContentDialog = false },
        )
    }

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
                .padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }

        // ── appearance ───────────────────────────────────────────────
        SectionLabel("APPEARANCE")
        TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)) {
            Column {
                ToggleRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Easy on the eyes at night",
                    checked = theme == com.testdone.app.data.local.prefs.ThemeMode.DARK,
                    onToggle = { vm.toggleTheme() },
                )
                var showLangPicker by remember { mutableStateOf(false) }
                SettingRow(Icons.Rounded.Language, "Language", currentLanguageLabel(vm, language)) {
                    showLangPicker = true
                }
                if (showLangPicker) {
                    LanguagePickerDialog(
                        languages = vm.container.i18n.languages,
                        current = language,
                        onPick = { code ->
                            vm.setLanguage(code)
                            showLangPicker = false
                        },
                        onDismiss = { showLangPicker = false },
                    )
                }
                ToggleRow(
                    icon = Icons.Rounded.Vibration,
                    title = "Vibration",
                    subtitle = "Haptic feedback on answers",
                    checked = vibration,
                    onToggle = { vm.container.settings.setVibration(!vibration) },
                )
                ToggleRow(
                    icon = Icons.Rounded.VolumeUp,
                    title = "App Sounds",
                    subtitle = "Chime when the app opens",
                    checked = sounds,
                    onToggle = { vm.container.settings.setSounds(!sounds) },
                )
            }
        }

        Spacer(Modifier.size(16.dp))

        // ── content ──────────────────────────────────────────────────
        SectionLabel("CONTENT & SYNC")
        TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)) {
            Column {
                SettingRow(
                    Icons.Rounded.CloudSync,
                    "Check for Updates",
                    if (syncing) "Checking…" else "Sync new questions over the air",
                ) {
                    scope.launch {
                        syncing = true
                        val result = vm.container.sessionCoordinator.manualContentCheck()
                        syncing = false
                        if (result == null) {
                            vm.container.sessionCoordinator.toast("You're up to date", "All exam packs are latest")
                        } else {
                            vm.container.sessionCoordinator.toast(
                                "New content added",
                                "${result.updatedExams.size} exam(s) updated · +${result.addedQuestions} questions · works offline",
                                com.testdone.app.di.SessionCoordinator.Toast.Kind.SUCCESS,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.size(16.dp))

        // ── account ──────────────────────────────────────────────────
        SectionLabel("ACCOUNT")
        TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)) {
            Column {
                SettingRow(Icons.Rounded.Lock, "Change Password", "Update your password") {
                    navController.navigate(Routes.CHANGE_PASSWORD)
                }
                // v2.3.15 — opens the IN-APP privacy screen (previously a toast
                // showing a URL the user couldn't open in-app).
                SettingRow(Icons.Rounded.PrivacyTip, "Privacy Policy", "How we handle your data") {
                    navController.navigate(Routes.PRIVACY) { launchSingleTop = true }
                }
            }
        }

        Spacer(Modifier.size(16.dp))

        // ── about ────────────────────────────────────────────────────
        SectionLabel("ABOUT")
        TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)) {
            Column {
                // v2.3.15 — every row now does something real on tap:
                //   App Version → version dialog (+ copy to clipboard)
                //   TestDone    → full About screen
                //   Content     → live content stats (+ check-for-updates)
                SettingRow(Icons.Rounded.Notifications, "App Version", "2.3.16 (native)") {
                    showVersionDialog = true
                }
                SettingRow(Icons.Rounded.WorkspacePremium, "TestDone", "18 exams · 18,600+ questions · 162 mocks") {
                    navController.navigate(Routes.ABOUT) { launchSingleTop = true }
                }
                SettingRow(Icons.Rounded.Update, "Content", "TestDone · OTA updates over the air") {
                    showContentDialog = true
                }
            }
        }

        Spacer(Modifier.size(20.dp))

        // logout
        Row(
            Modifier
                .fillMaxWidth()
                .pressableScale(0.98f) { vm.logout(activityRef) { } }
                .background(TdExt.colors.dangerDim, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = TdExt.colors.danger, modifier = Modifier.size(20.dp))
            Text("Logout", style = MaterialTheme.typography.labelLarge, color = TdExt.colors.danger)
            Spacer(Modifier.weight(1f))
            Text(
                user.email.ifBlank { "not signed in" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(130.dp)) // clearance for the bottom nav bar
    }
}

@Composable
private fun currentLanguageLabel(vm: AppViewModel, code: String): String {
    return vm.container.i18n.languages.firstOrNull { it.first == code }?.third ?: code
}

/** v2.3.15 — App Version dialog: full build info + one-tap copy. */
@Composable
private fun VersionInfoDialog(onCopy: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("App Version", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(10.dp))
            Text(
                "TestDone",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "v2.3.16",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            // v2.3.16 — package/minSdk/OTA lines removed (user request): only
            // the TestDone + version line stays.
            Spacer(Modifier.size(16.dp))
            TdButton(
                text = "Copy version info",
                onClick = {
                    onCopy()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(8.dp))
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

/** v2.3.15 — Content stats dialog: live exam/question counts + update action. */
@Composable
private fun ContentInfoDialog(vm: AppViewModel, onCheckUpdates: () -> Unit, onDismiss: () -> Unit) {
    val exams by vm.container.contentRepository.exams.collectAsState()
    val totalQuestions = exams.sumOf { it.questionCount }
    val totalMocks = exams.sumOf { it.mockCount }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Text("Exam content", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${exams.size}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        Text("Exams", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${totalQuestions}", style = MaterialTheme.typography.titleLarge, color = TdExt.colors.success, fontWeight = FontWeight.ExtraBold)
                        Text("Questions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${totalMocks}", style = MaterialTheme.typography.titleLarge, color = TdExt.colors.warning, fontWeight = FontWeight.ExtraBold)
                        Text("Mocks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
            // v2.3.16 — CDN/OTA technical line removed; premium "coming soon"
            // gold strip instead (user request).
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(TdExt.colors.warning.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
                    .border(1.dp, TdExt.colors.warning.copy(alpha = 0.35f), RoundedCornerShape(13.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = TdExt.colors.warning,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "More exams, mocks and questions coming soon.",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TdExt.colors.warning,
                )
            }
            Spacer(Modifier.size(14.dp))
            TdButton(
                text = "Check for updates",
                onClick = onCheckUpdates,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(8.dp))
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
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
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

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}


@Composable
private fun LanguagePickerDialog(
    languages: List<Triple<String, String, String>>,
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                .padding(18.dp),
        ) {
            Column {
                Text("App Language", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(12.dp))
                languages.forEach { (code, label, native) ->
                    val active = code == current
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pressableScale(0.98f) { onPick(code) }
                            .background(
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(13.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(native, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.weight(1f))
                        if (active) {
                            Icon(
                                androidx.compose.material.icons.Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
