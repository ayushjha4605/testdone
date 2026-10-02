package com.testdone.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.testdone.app.data.repository.CommunityRepository
import com.testdone.app.data.repository.CommunityRepository.Doubt
import com.testdone.app.data.repository.CommunityRepository.Submission
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.Pill
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.TdButtonStyle
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.theme.TdExt
import kotlinx.coroutines.launch

/**
 * v2.3.16 — admin console (owner accounts only), now with THREE sections:
 *
 *  • Questions — community question submissions (pending → live/rejected)
 *  • Doubts    — community doubts; a doubt reaches the public feed ONLY after
 *                approval here (user request: admin approve gate)
 *  • Config    — payments switch (Razorpay on/off + Key ID, written to
 *                app_config/payments) + force-update status/info card
 */
@Composable
fun AdminReviewScreen(navController: NavHostController, vm: AppViewModel) {
    val user by vm.user.collectAsState()
    val admin = CommunityRepository.isAdmin(user.email)
    var section by remember { mutableStateOf("questions") }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Column {
                Text("Moderate", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Admin console · doubts, questions & app config",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!admin) {
            // gate — non-owner accounts get exactly this
            Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .background(TdExt.colors.warning.copy(alpha = 0.13f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = TdExt.colors.warning, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Sirf project admin ke liye — owner account se login karo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        } else {
            // section switcher
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "questions" to "Questions",
                    "doubts" to "Doubts",
                    "waitlist" to "Waitlist",
                    "config" to "Config",
                ).forEach { (key, label) ->
                    val selected = section == key
                    Box(
                        Modifier
                            .pressableScale(0.96f) { section = key }
                            .background(
                                if (selected) TdExt.colors.brandBrush
                                else androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh),
                                ),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            when (section) {
                "doubts" -> DoubtsAdmin(vm)
                "waitlist" -> WaitlistAdmin(vm)
                "config" -> ConfigAdmin(vm)
                else -> QuestionsAdmin(vm)
            }
        }
    }
}

// ── questions moderation (v2.3.14 behavior, unchanged contract) ──────────────

private val TABS = listOf("pending", "live", "rejected")

@Composable
private fun QuestionsAdmin(vm: AppViewModel) {
    var tab by remember { mutableStateOf("pending") }
    var subs by remember { mutableStateOf<List<Submission>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val toast = vm.container.sessionCoordinator

    fun load() {
        scope.launch {
            error = null
            subs = null
            val result = vm.container.communityRepository.listSubmissions(tab)
            result
                .onSuccess { subs = it }
                .onFailure { e ->
                    subs = emptyList()
                    error = CommunityRepository.friendly(e).message
                }
        }
    }

    LaunchedEffect(tab) { load() }

    // tab chips
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TABS.forEach { t ->
            val selected = tab == t
            Box(
                Modifier
                    .pressableScale(0.96f) { tab = t }
                    .background(
                        if (selected) TdExt.colors.brandBrush
                        else androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh),
                        ),
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Text(
                    t.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    when {
        subs == null && error == null -> LoadingRow("Questions load ho rahe hain…")
        error != null -> ErrorRow(error!!) { load() }
        subs!!.isEmpty() -> Box(Modifier.fillMaxWidth().padding(top = 40.dp)) { EmptyTab(tab) }
        else -> LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 130.dp),
        ) {
            items(subs!!, key = { it.id }) { sub ->
                SubmissionCard(
                    sub = sub,
                    busy = busyId == sub.id,
                    onApprove = {
                        busyId = sub.id
                        scope.launch {
                            vm.container.communityRepository.reviewSubmission(sub.id, approve = true)
                                .onSuccess {
                                    toast.toast("LIVE in Q-Bank", "Ye question ab us exam ke Q-Bank me LIVE hai — sab users ko dikhega")
                                }
                                .onFailure { e -> toast.toast("Load nahi ho paya", CommunityRepository.friendly(e).message) }
                            busyId = null
                            load()
                        }
                    },
                    onReject = {
                        busyId = sub.id
                        scope.launch {
                            vm.container.communityRepository.reviewSubmission(sub.id, approve = false)
                                .onSuccess { toast.toast("Question rejected", "Author ko 'Not approved' status dikhega") }
                                .onFailure { e -> toast.toast("Load nahi ho paya", CommunityRepository.friendly(e).message) }
                            busyId = null
                            load()
                        }
                    },
                    onBan = {
                        busyId = sub.id
                        scope.launch {
                            vm.container.communityRepository.setUserSuspended(sub.authorId, suspend = true)
                                .onSuccess { toast.toast("Moderate", "Account suspended") }
                                .onFailure { e -> toast.toast("Account ban nahi paya", CommunityRepository.friendly(e).message) }
                            busyId = null
                        }
                    },
                )
            }
        }
    }
}

// ── doubts moderation (v2.3.16) ──────────────────────────────────────────────

@Composable
private fun DoubtsAdmin(vm: AppViewModel) {
    var tab by remember { mutableStateOf("pending") }
    var doubts by remember { mutableStateOf<List<Doubt>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val toast = vm.container.sessionCoordinator

    fun load() {
        scope.launch {
            error = null
            doubts = null
            vm.container.communityRepository.listDoubts(tab)
                .onSuccess { doubts = it }
                .onFailure { e ->
                    doubts = emptyList()
                    error = CommunityRepository.friendly(e).message
                }
        }
    }

    LaunchedEffect(tab) { load() }

    // status chips
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TABS.forEach { t ->
            val selected = tab == t
            Box(
                Modifier
                    .pressableScale(0.96f) { tab = t }
                    .background(
                        if (selected) TdExt.colors.brandBrush
                        else androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh),
                        ),
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Text(
                    t.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Text(
        "Approve karne ke baad hi doubt sab users ko dikhta hai.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    when {
        doubts == null && error == null -> LoadingRow("Doubts load ho rahe hain…")
        error != null -> ErrorRow(error!!) { load() }
        doubts!!.isEmpty() -> Box(Modifier.fillMaxWidth().padding(top = 40.dp)) { EmptyDoubts(tab) }
        else -> LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 130.dp),
        ) {
            items(doubts!!, key = { it.id }) { doubt ->
                DoubtCard(
                    doubt = doubt,
                    busy = busyId == doubt.id,
                    onApprove = {
                        busyId = doubt.id
                        scope.launch {
                            vm.container.communityRepository.reviewDoubt(doubt.id, approve = true)
                                .onSuccess { toast.toast("Doubt approved", "Ab sab users ko Doubts feed me dikhega") }
                                .onFailure { e -> toast.toast("Approve nahi hua", CommunityRepository.friendly(e).message) }
                            busyId = null
                            load()
                        }
                    },
                    onReject = {
                        busyId = doubt.id
                        scope.launch {
                            vm.container.communityRepository.reviewDoubt(doubt.id, approve = false)
                                .onSuccess { toast.toast("Doubt rejected", "Ye doubt feed me nahi dikhega") }
                                .onFailure { e -> toast.toast("Reject nahi hua", CommunityRepository.friendly(e).message) }
                            busyId = null
                            load()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DoubtCard(
    doubt: Doubt,
    busy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    TdCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(TdExt.colors.brandBrush, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        doubt.authorName.firstOrNull()?.toString() ?: "A",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(doubt.authorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        StatusPill(doubt.status)
                    }
                    Text(
                        doubt.exam.ifBlank { "general" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(doubt.question, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (doubt.status == "pending") {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1.4f)) {
                        TdButton("Approve", onClick = onApprove, style = TdButtonStyle.GRADIENT, loading = busy, modifier = Modifier.fillMaxWidth())
                    }
                    Box(Modifier.weight(1f)) {
                        TdButton("Reject", onClick = onReject, style = TdButtonStyle.DANGER, loading = busy, modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (doubt.status == "live") "LIVE — sab users ko Doubts feed me dikhta hai"
                    else "Rejected — feed me nahi dikhega",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (doubt.status == "live") TdExt.colors.success else TdExt.colors.danger,
                )
            }
        }
    }
}

// ── waitlist (v2.3.17): Ultra early-access emails — KAUN join kiya, kab ─────

@Composable
private fun WaitlistAdmin(vm: AppViewModel) {
    var entries by remember { mutableStateOf<List<com.testdone.app.data.repository.CommunityRepository.UltraWaitlistEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        vm.container.communityRepository.listUltraWaitlist()
            .onSuccess { list ->
                entries = list
                error = null
            }
            .onFailure { t ->
                error = com.testdone.app.data.repository.CommunityRepository.friendly(t).message
            }
    }
    LaunchedEffect(Unit) { load() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Ultra early-access waitlist",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        )
        Text(
            "Har user jo Ultra tab pe email daal kar join hua",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        when {
            error != null -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                Text(
                    error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TdExt.colors.danger,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            entries == null -> Box(Modifier.fillMaxWidth().padding(top = 40.dp)) { LoadingRow("Waitlist load ho rahi hai…") }
            entries!!.isEmpty() -> EmptyWaitlist()
            else -> {
                Text(
                    "${entries!!.size} users waitlist pe hain",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                entries!!.forEach { entry ->
                    TdCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .background(TdExt.colors.brandBrush, RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.email,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "${entry.userName} · ${com.testdone.app.domain.logic.timeAgo(entry.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyWaitlist() {
    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Abhi koi join nahi hua",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Jaise hi koi user Ultra tab pe email daalega, yahan dikhega",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── config (v2.3.16): payments + force update ───────────────────────────────

@Composable
private fun ConfigAdmin(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    val toast = vm.container.sessionCoordinator

    // live config (re-fetched after each save)
    var config by remember { mutableStateOf<com.testdone.app.data.repository.PaymentsRepository.PaymentsConfig?>(null) }
    LaunchedEffect(Unit) {
        config = vm.container.paymentsRepository.config().getOrDefault(
            com.testdone.app.data.repository.PaymentsRepository.PaymentsConfig()
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── payments card ───────────────────────────────────────────────────
        val cfg = config
        if (cfg == null) {
            LoadingRow("Config load ho rahi hai…")
        } else {
            var enabled by remember(cfg) { mutableStateOf(cfg.enabled) }
            var keyId by remember(cfg) { mutableStateOf(cfg.razorpayKeyId ?: "") }
            var saving by remember { mutableStateOf(false) }

            TdCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(TdExt.colors.warning.copy(alpha = 0.13f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Payments, contentDescription = null, tint = TdExt.colors.warning, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Razorpay payments", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (cfg.enabled) "ON — users se payment le sakte ho" else "OFF — app me sab free hai",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (cfg.enabled) TdExt.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = TdExt.colors.success,
                            ),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = keyId,
                        onValueChange = { keyId = it },
                        label = { Text("Razorpay Key ID (rzp_live_…)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Key ID Razorpay Dashboard → Settings → API Keys se milegi. Secret key kabhi app me nahi daalte — wo dashboard me hi rehti hai.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    TdButton(
                        "Save payments config",
                        onClick = {
                            saving = true
                            scope.launch {
                                vm.container.paymentsRepository.saveConfig(enabled, keyId)
                                    .onSuccess {
                                        toast.toast(
                                            "Config saved",
                                            if (enabled) "Payments ON — Plan screen ab charge karega" else "Payments OFF — sab free",
                                            com.testdone.app.di.SessionCoordinator.Toast.Kind.SUCCESS,
                                        )
                                        // re-read to confirm persisted state
                                        config = vm.container.paymentsRepository.config().getOrDefault(
                                            com.testdone.app.data.repository.PaymentsRepository.PaymentsConfig()
                                        )
                                    }
                                    .onFailure { e ->
                                        toast.toast("Save nahi hua", e.message ?: "Firestore rules check karo (FIXES doc)")
                                    }
                                saving = false
                            }
                        },
                        loading = saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── force update card ────────────────────────────────────────────
            TdCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Force update", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("Remote Config se control hota hai", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Installed build: v${com.testdone.app.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Force update lagane ke liye: Firebase Console → Remote Config → naya parameter force_update_min_version = \"2.4.0\" (jaise bhi version chahiye) → Publish. Jis version se neeche wale devices hain unhe update screen lagegi — launch se pehle blank chhod do.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp,
                    )
                    Text(
                        "Optional: force_update_message (custom line) aur force_update_apk_url (sideload users ke liye direct APK link).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(130.dp))
        }
    }
}

// ── shared pieces ────────────────────────────────────────────────────────────

@Composable
private fun LoadingRow(message: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorRow(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Load nahi ho paya — $message", style = MaterialTheme.typography.bodyMedium, color = TdExt.colors.danger)
        Spacer(Modifier.height(12.dp))
        TdButton("Dobara try karo", onClick = onRetry, style = TdButtonStyle.SECONDARY)
    }
}

@Composable
private fun EmptyTab(tab: String) {
    val message = when (tab) {
        "pending" -> "Koi pending question nahi — sab review ho chuka"
        "live" -> "Abhi koi approved question nahi — Pending tab se approve karo"
        else -> "Koi rejected question nahi"
    }
    EmptyState(message)
}

@Composable
private fun EmptyDoubts(tab: String) {
    val message = when (tab) {
        "pending" -> "Koi pending doubt nahi — sab review ho chuka"
        "live" -> "Abhi koi approved doubt nahi — Pending tab se approve karo"
        else -> "Koi rejected doubt nahi"
    }
    EmptyState(message)
}

@Composable
private fun EmptyState(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubmissionCard(
    sub: Submission,
    busy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onBan: () -> Unit,
) {
    TdCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(TdExt.colors.brandBrush, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        sub.authorName.firstOrNull()?.toString() ?: "A",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sub.authorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        StatusPill(sub.status)
                    }
                    Text(
                        sub.examId.ifBlank { "general" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(sub.question, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (sub.options.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                sub.options.forEachIndexed { i, opt ->
                    val letter = ('A' + i)
                    Text(
                        "$letter. $opt" + if (sub.correct == letter.toString()) "  ✓" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (sub.correct == letter.toString()) TdExt.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (sub.explanation.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(sub.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            }
            if (sub.status == "pending") {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1.4f)) {
                        TdButton(
                            "Approve",
                            onClick = onApprove,
                            style = TdButtonStyle.GRADIENT,
                            loading = busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        TdButton(
                            "Reject",
                            onClick = onReject,
                            style = TdButtonStyle.DANGER,
                            loading = busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    IconButton(onClick = onBan, enabled = !busy) {
                        Icon(Icons.Rounded.Block, contentDescription = "Suspend author", tint = TdExt.colors.danger, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    "LIVE — Q-Bank me sabko dikhta hai",
                    style = MaterialTheme.typography.labelSmall,
                    color = TdExt.colors.success,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    when (status) {
        "live" -> Pill("LIVE", TdExt.colors.success, TdExt.colors.successDim)
        "rejected" -> Pill("Rejected", TdExt.colors.danger, TdExt.colors.dangerDim)
        else -> Pill("Pending", TdExt.colors.warning, TdExt.colors.warning.copy(alpha = 0.12f))
    }
}
