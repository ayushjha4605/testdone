package com.testdone.app.ui.qbank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.testdone.app.domain.model.Question
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.LocalMenuOpener
import com.testdone.app.ui.MenuButton
import com.testdone.app.ui.components.Pill
import com.testdone.app.ui.components.ExamLogoBadge
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.theme.TdExt

/** Question Bank tab: exam → subject → topic → paginated practice questions. */
@Composable
fun QBankScreen(navController: NavHostController, vm: AppViewModel) {
    val qvm: QBankViewModel = viewModel(factory = com.testdone.app.ui.simpleFactory { QBankViewModel(vm.container) })
    val state by qvm.state.collectAsState()
    val openMenu = LocalMenuOpener.current
    val totalQ = vm.container.contentRepository.totalQuestions

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        // header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Question Bank", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "18 exams · ${"%.1f".format(totalQ / 1000.0)}k+ real questions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MenuButton(onOpen = openMenu)
        }

        val activeExam = state.activeExamId?.let { vm.container.contentRepository.examById(it) }

        if (activeExam == null) {
            // ── exam picker ─────────────────────────────────────────────
            QBankExamPicker(qvm = qvm, vm = vm)
        } else {
            // ── subject → topic → questions ─────────────────────────────
            Text(
                "${activeExam.icon} ${activeExam.name}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Change exam",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .pressableScale(0.95f) { qvm.closeExam() }
                    .padding(4.dp),
            )
            Spacer(Modifier.height(10.dp))

            // subject chips — first chip = All (no filter); others filter
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                item(key = "all") {
                    val active = state.selectedSubject == null
                    Box(
                        Modifier
                            .pressableScale(0.94f) { qvm.selectSubject(null) }
                            .background(
                                if (active) TdExt.colors.brandBrush
                                else androidx.compose.ui.graphics.Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                    ) {
                        Text(
                            "All",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.subjects, key = { it.first }) { (sid, name) ->
                    val active = state.selectedSubject == sid
                    Box(
                        Modifier
                            .pressableScale(0.94f) { qvm.selectSubject(if (active) null else sid) }
                            .background(
                                if (active) TdExt.colors.brandBrush
                                else androidx.compose.ui.graphics.Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // topic dropdown
            if (state.topics.isNotEmpty()) {
                TopicDropdown(qvm = qvm, topics = state.topics)
                Spacer(Modifier.height(10.dp))
            }

            // questions — shown for the WHOLE exam by default; chips filter
            val filterLabel = when {
                state.selectedTopic != null -> state.selectedTopic!!
                state.selectedSubject != null -> state.subjects.firstOrNull { it.first == state.selectedSubject }?.second ?: ""
                else -> "all subjects"
            }
            Text(
                "${state.total} questions · $filterLabel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (state.loading && state.questions.isEmpty()) {
                // first-load skeletons
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    com.testdone.app.ui.components.SkeletonList(rows = 3)
                }
            } else if (state.questions.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No questions found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
                ) {
                    items(state.questions, key = { it.id }) { q ->
                        PracticeCard(q)
                    }
                    if (state.hasMore) {
                        item {
                            TdButton(
                                text = "Load more",
                                onClick = { qvm.loadMore() },
                                style = com.testdone.app.ui.components.TdButtonStyle.SECONDARY,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QBankExamPicker(qvm: QBankViewModel, vm: AppViewModel) {
    var search by remember { mutableStateOf("") }
    OutlinedTextField(
        value = search,
        onValueChange = { search = it; qvm.setSearch(it) },
        placeholder = { Text("Search exams…", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
    ) {
        val exams = qvm.searchExams()
        items(exams, key = { it.id }) { exam ->
            TdCard(
                Modifier
                    .fillMaxWidth()
                    .pressableScale(0.97f) { qvm.openExam(exam.id) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExamLogoBadge(examId = exam.id, size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(exam.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "${exam.questionCount} questions · ${exam.subjects.size} subjects",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicDropdown(qvm: QBankViewModel, topics: List<QBankViewModel.Topic>) {
    var expanded by remember { mutableStateOf(false) }
    val current = qvm.state.value.selectedTopic ?: "All topics"
    Box {
        Row(
            Modifier
                .pressableScale(0.97f) { expanded = true }
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                current,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All topics") },
                onClick = { qvm.selectTopic(null); expanded = false },
            )
            topics.forEach { t ->
                DropdownMenuItem(
                    text = { Text("${t.name} (${t.count})", style = MaterialTheme.typography.bodySmall) },
                    onClick = { qvm.selectTopic(t.name); expanded = false },
                )
            }
        }
    }
}

private val LETTERS = listOf("A", "B", "C", "D", "E")

@Composable
private fun PracticeCard(q: Question) {
    var showSolution by remember { mutableStateOf(false) }
    TdCard(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(q.difficulty.label, TdExt.colors.warning, TdExt.colors.warningDim)
                Spacer(Modifier.width(6.dp))
                Pill(q.topic, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                Spacer(Modifier.weight(1f))
                Text("+${q.marks.toInt()}", style = MaterialTheme.typography.labelMedium, color = TdExt.colors.success)
            }
            Spacer(Modifier.height(12.dp))
            Text(q.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(14.dp))
            q.options.forEachIndexed { i, opt ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(13.dp))
                        .defaultMinSize(minHeight = 50.dp)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            LETTERS.getOrElse(i) { "${i + 1}" },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        opt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(4.dp))
            // solution toggle as a proper wide pill button (was a thin text link)
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressableScale(0.97f) { showSolution = !showSolution }
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        RoundedCornerShape(13.dp),
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(13.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (showSolution) "Hide solution" else "Show solution",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (showSolution) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = TdExt.colors.warning, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Correct: ${LETTERS.getOrElse(q.correct) { "${q.correct + 1}" }} · ${q.options.getOrNull(q.correct) ?: ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TdExt.colors.success,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(q.solution, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
