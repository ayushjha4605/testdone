package com.testdone.app.ui.submit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.testdone.app.data.repository.CommunityRepository
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.theme.TdExt
import kotlinx.coroutines.launch

/**
 * v2.3.14 — Submit a Question, Firestore-backed (recovered from release APK):
 * submissions land in `community_submissions` (status=pending) and the user's
 * own review queue shows here with Pending / LIVE / Rejected status.
 */
@Composable
fun SubmitQuestionScreen(navController: NavHostController, vm: AppViewModel) {
    val exams by vm.container.contentRepository.exams.collectAsState()
    var submitted by rememberSaveable { mutableStateOf(false) }
    var submitting by rememberSaveable { mutableStateOf(false) }
    var submitError by rememberSaveable { mutableStateOf<String?>(null) }
    var mySubs by remember { mutableStateOf<List<CommunityRepository.Submission>?>(null) }
    val scope = rememberCoroutineScope()

    var question by rememberSaveable { mutableStateOf("") }
    var optionA by rememberSaveable { mutableStateOf("") }
    var optionB by rememberSaveable { mutableStateOf("") }
    var optionC by rememberSaveable { mutableStateOf("") }
    var optionD by rememberSaveable { mutableStateOf("") }
    var correct by rememberSaveable { mutableStateOf("A") }
    var explanation by rememberSaveable { mutableStateOf("") }
    var selectedExam by rememberSaveable { mutableStateOf("") }

    val valid = question.isNotBlank() && optionA.isNotBlank() && optionB.isNotBlank() && optionC.isNotBlank() && optionD.isNotBlank()

    fun loadMine() {
        scope.launch {
            vm.container.communityRepository.mySubmissions()
                .onSuccess { mySubs = it }
                .onFailure { mySubs = emptyList() }
        }
    }
    LaunchedEffect(Unit) { loadMine() }

    fun doSubmit() {
        val user = vm.container.settings.user.value
        val loggedIn = user.email.isNotBlank() || user.phone.isNotBlank()
        if (!loggedIn) {
            // v2.3.14 — exact release wording
            submitError = "Question submit karne ke liye pehle login karo"
            return
        }
        submitting = true
        submitError = null
        scope.launch {
            vm.container.communityRepository.submitQuestion(
                question = question,
                options = listOf(optionA, optionB, optionC, optionD),
                correct = correct,
                explanation = explanation,
                examId = selectedExam,
            ).onSuccess {
                submitting = false
                submitted = true
                loadMine()
            }.onFailure { e ->
                submitting = false
                submitError = CommunityRepository.friendly(e).message
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding() // v2.3.16 — fields stay above the keyboard
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
            Column {
                Text("Submit a Question", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Help 18,000+ aspirants practice better", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (submitted) {
            Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = TdExt.colors.success, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("Question submitted!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Our experts will review it. Once approved, it goes live with your name credited.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    TdButton(
                        "Submit another",
                        onClick = {
                            submitted = false
                            question = ""; optionA = ""; optionB = ""; optionC = ""; optionD = ""; explanation = ""
                        },
                        style = com.testdone.app.ui.components.TdButtonStyle.SECONDARY,
                    )
                }
            }
        } else {
            // v2.3.14 — my review queue (status of previously submitted questions)
            if (!mySubs.isNullOrEmpty()) {
                MySubmissionsCard(mySubs!!)
                Spacer(Modifier.height(14.dp))
            } else if (mySubs != null) {
                Text(
                    "Abhi tak koi question submit nahi kiya. Pehla submit karne ke baad yahan status dikhega.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
                )
            }
            TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Column {
                    Text("Question details", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

                    Spacer(Modifier.height(12.dp))
                    Text("Exam", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                    ExamDropdown(exams = exams, selected = selectedExam, onSelect = { selectedExam = it })

                    Spacer(Modifier.height(12.dp))
                    Text("Question *", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        placeholder = { Text("Type your question here…") },
                        minLines = 3,
                        shape = RoundedCornerShape(13.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("Options (all 4 required) *", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                    listOf("A" to optionA, "B" to optionB, "C" to optionC, "D" to optionD).forEachIndexed { i, pair ->
                        val letter = pair.first
                        val value = pair.second
                        val setters = listOf({ v: String -> optionA = v }, { v: String -> optionB = v }, { v: String -> optionC = v }, { v: String -> optionD = v })
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .background(
                                        if (correct == letter) TdExt.colors.brandBrush
                                        else androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh),
                                        ),
                                        RoundedCornerShape(9.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    letter,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (correct == letter) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.size(8.dp))
                            Box(Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = setters[i],
                                    placeholder = { Text("Option $letter") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(11.dp),
                                    colors = fieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.size(4.dp))
                            Text(
                                if (correct == letter) "✓" else "Set ✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (correct == letter) TdExt.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickableNoRipple { if (value.isNotBlank()) correct = letter },
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Explanation (optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        placeholder = { Text("Why is the answer correct?") },
                        minLines = 2,
                        shape = RoundedCornerShape(13.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))
                    if (submitError != null) {
                        Text(
                            submitError!!, style = MaterialTheme.typography.labelMedium, color = TdExt.colors.danger,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    TdButton(
                        text = "Submit question",
                        onClick = { doSubmit() },
                        enabled = valid && !submitting,
                        loading = submitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Reviewed by experts · Credited to you once live",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
            Spacer(Modifier.height(130.dp)) // clearance for the bottom nav bar
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
)

@Composable
private fun ExamDropdown(exams: List<com.testdone.app.domain.model.Exam>, selected: String, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val label = exams.firstOrNull { it.id == selected }?.let { "${it.icon} ${it.name}" } ?: "Select exam (optional)"
    Column {
        TdCard(
            Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp, horizontal = 16.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            exams.forEach { e ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("${e.icon} ${e.name}", style = MaterialTheme.typography.bodySmall) },
                    onClick = { onSelect(e.id); expanded = false },
                )
            }
        }
    }
}

/** Clickable without ripple for text links. */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )

/** v2.3.14 — this user's submitted questions + their review status. */
@Composable
private fun MySubmissionsCard(subs: List<CommunityRepository.Submission>) {
    TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Column {
            Text("Tumhare bheje hue questions — review queue mein yahan dikhte hain", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            subs.take(5).forEach { s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        s.question.take(70) + if (s.question.length > 70) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                    )
                    Spacer(Modifier.width(8.dp))
                    when (s.status) {
                        "live" -> com.testdone.app.ui.components.Pill("LIVE", TdExt.colors.success, TdExt.colors.successDim)
                        "rejected" -> com.testdone.app.ui.components.Pill("Not approved", TdExt.colors.danger, TdExt.colors.dangerDim)
                        else -> com.testdone.app.ui.components.Pill("Pending", TdExt.colors.warning, TdExt.colors.warning.copy(alpha = 0.12f))
                    }
                }
                if (s.status == "live") {
                    Text(
                        "Question approved · LIVE — Q-Bank me sabko dikhta hai",
                        style = MaterialTheme.typography.labelSmall,
                        color = TdExt.colors.success,
                    )
                }
            }
        }
    }
}
