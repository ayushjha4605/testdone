package com.testdone.app.ui.doubts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.testdone.app.data.repository.CommunityRepository
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.TdButtonStyle
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.theme.TdExt
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * v2.3.14 — Doubts community, Firestore-backed (recovered from release APK):
 * real feed from `community_doubts`, real posting (logged-in users only).
 */
@Composable
fun DoubtsScreen(navController: NavHostController, vm: AppViewModel) {
    val user by vm.user.collectAsState()
    var showAsk by rememberSaveable { mutableStateOf(false) }
    var doubts by remember { mutableStateOf<List<CommunityRepository.Doubt>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            error = null
            vm.container.communityRepository.feedDoubts()
                .onSuccess { doubts = it }
                .onFailure { e ->
                    doubts = emptyList()
                    error = CommunityRepository.friendly(e).message
                }
        }
    }

    // v2.3.14 — posting requires login (viewing stays open)
    val loggedIn = user.email.isNotBlank() || user.phone.isNotBlank()

    LaunchedEffect(Unit) { load() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding() // v2.3.16 — doubt composer stays above the keyboard
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(Modifier.weight(1f)) {
                Text("Doubts Community", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Ask · Answer · Ace it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ask CTA
        TdCard(
            Modifier
                .fillMaxWidth()
                .pressableScale(0.98f) { showAsk = true },
            contentPadding = PaddingValues(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(TdExt.colors.brandBrush, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.QuestionAnswer, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Ask a Doubt", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("Get answers from toppers & mentors", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (showAsk) {
            Spacer(Modifier.height(12.dp))
            val currentExam = user.examLabel()
            AskDoubtCard(
                examLabel = currentExam,
                onPost = { text ->
                    if (!loggedIn) {
                        vm.container.sessionCoordinator.toast(
                            "Pehle login karo",
                            "Doubt post karne ke liye pehle login karo — tab tak dusron ke doubts padh sakte ho",
                            kind = com.testdone.app.di.SessionCoordinator.Toast.Kind.ERROR,
                        )
                        return@AskDoubtCard
                    }
                    scope.launch {
                        vm.container.communityRepository.postDoubt(text, currentExam)
                            .onSuccess {
                                showAsk = false
                                // v2.3.16 — doubts need admin approval before they
                                // show up in the public feed
                                vm.container.sessionCoordinator.toast(
                                    "Doubt bhej diya!",
                                    "Admin approve karega tab sabko dikhega",
                                    kind = com.testdone.app.di.SessionCoordinator.Toast.Kind.SUCCESS,
                                )
                            }
                            .onFailure { e ->
                                vm.container.sessionCoordinator.toast("Feed load nahi hua", CommunityRepository.friendly(e).message, kind = com.testdone.app.di.SessionCoordinator.Toast.Kind.ERROR)
                            }
                    }
                },
                onCancel = { showAsk = false },
            )
        }

        Spacer(Modifier.height(14.dp))

        when {
            doubts == null && error == null -> Box(
                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Exams load ho rahe hain", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            error != null -> Column(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Feed load nahi hua — Dobara try karo", style = MaterialTheme.typography.bodyMedium, color = TdExt.colors.danger)
                Spacer(Modifier.height(10.dp))
                TdButton("Dobara try karo", onClick = { load() }, style = TdButtonStyle.SECONDARY)
            }
            doubts!!.isEmpty() -> Box(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Abhi koi doubt nahi hai", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pehla doubt tum post karo — poora community yahi jawab dega.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 130.dp), // bottom-bar clearance
            ) {
                items(doubts!!, key = { it.id }) { d ->
                    DoubtCard(d)
                }
            }
        }
    }
}

@Composable
private fun AskDoubtCard(examLabel: String, onPost: (String) -> Unit, onCancel: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    TdCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column {
            Text("Your doubt", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Type your question. Be specific for faster answers…", style = MaterialTheme.typography.bodySmall) },
                minLines = 3,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    TdButton("Cancel", onClick = onCancel, style = TdButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth())
                }
                Box(Modifier.weight(1.4f)) {
                    TdButton("Post Doubt", onClick = { if (text.isNotBlank()) onPost(text.trim()) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun DoubtCard(d: CommunityRepository.Doubt) {
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
                        d.authorName.firstOrNull()?.toString() ?: "?",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.authorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${d.createdAt.timeAgo()} · ${d.exam.ifBlank { "General" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(d.question, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    Text(" ${d.answers.size} answers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ThumbUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text(" ${d.likes}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun Long.timeAgo(): String {
    if (this <= 0L) return "just now"
    val mins = (System.currentTimeMillis() - this) / 60000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 60 * 24 -> "${mins / 60}h ago"
        else -> "${mins / (60 * 24)}d ago"
    }
}

private fun com.testdone.app.domain.model.UserProfile.examLabel(): String {
    return selectedExamId?.takeIf { it.isNotBlank() } ?: "General"
}
