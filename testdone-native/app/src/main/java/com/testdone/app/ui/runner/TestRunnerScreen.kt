@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.testdone.app.ui.runner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.testdone.app.domain.logic.formatDuration
import com.testdone.app.domain.model.QuestionStatus
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.TdButtonStyle
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt

private val LETTERS = listOf("A", "B", "C", "D", "E")

/** Full-screen test runner: header + question + bottom bar + overlays. */
@Composable
fun TestRunnerScreen(navController: NavHostController, vm: AppViewModel, examId: String, testId: String) {
    val rvm: RunnerViewModel = viewModel(factory = com.testdone.app.ui.simpleFactory { RunnerViewModel(vm.container) })
    val state by rvm.state.collectAsState()

    LaunchedEffect(testId) { rvm.load(examId, testId) }

    // route to report when finished
    LaunchedEffect(state.stage) {
        if (state.stage == RunnerViewModel.Stage.DONE && state.savedAttemptId != null) {
            val id = state.savedAttemptId!!
            navController.navigate(Routes.report(id)) {
                popUpTo(Routes.instructions(examId, testId)) { inclusive = true }
            }
        }
    }

    // back = exit dialog
    BackHandler(enabled = state.stage == RunnerViewModel.Stage.RUNNING) {
        rvm.setExit(true)
    }

    when {
        state.stage == RunnerViewModel.Stage.LOADING && !state.loadFailed -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .background(TdExt.colors.brandBrush, RoundedCornerShape(16.dp)),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Loading test…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        state.loadFailed -> {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Test load nahi hua", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    TdButton("Back", onClick = { navController.popBackStack() }, style = TdButtonStyle.SECONDARY)
                }
            }
        }
        else -> {
            RunnerContent(rvm = rvm, navController = navController)
        }
    }
}

@Composable
private fun RunnerContent(rvm: RunnerViewModel, navController: NavHostController) {
    val state by rvm.state.collectAsState()
    val test = state.test ?: return

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── header: timer + section chips ───────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { rvm.setExit(true) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Exit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    test.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    "Question ${state.currentIndex + 1} of ${state.questions.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // timer
            val low = state.remainingSec < 300
            Box(
                Modifier
                    .background(
                        if (low) TdExt.colors.dangerDim else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = if (low) TdExt.colors.danger else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        formatDuration(state.remainingSec),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (low) TdExt.colors.danger else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // section chips
        val sections = state.subjectOrder
        if (sections.size > 1) {
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            ) {
                items(sections) { (sid, name) ->
                    val active = state.currentSectionId == sid
                    Box(
                        Modifier
                            .pressableScale(0.94f) {
                                val idx = state.questions.indexOfFirst { it.subjectId == sid }
                                if (idx >= 0) rvm.goTo(idx)
                            }
                            .background(
                                if (active) TdExt.colors.brandBrush
                                else androidx.compose.ui.graphics.Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh)),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ── question area ───────────────────────────────────────────────
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = state.currentIndex,
                transitionSpec = {
                    val forward = targetState >= initialState
                    (slideInHorizontally(tween(260)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(tween(200)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(160)))
                },
                label = "questionSlide",
            ) { index ->
                val q = state.questions.getOrNull(index)
                if (q != null) {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = 24.dp),
                    ) {
                        item {
                            // question meta row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Q${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                if (q.isPyq) {
                                    com.testdone.app.ui.components.Pill("PYQ", TdExt.colors.warning, TdExt.colors.warningDim)
                                }
                                com.testdone.app.ui.components.Pill(
                                    q.difficulty.label,
                                    when (q.difficulty) {
                                        com.testdone.app.domain.model.Difficulty.EASY -> TdExt.colors.success
                                        com.testdone.app.domain.model.Difficulty.MODERATE -> TdExt.colors.warning
                                        else -> TdExt.colors.danger
                                    },
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "+${q.marks.toInt()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TdExt.colors.success,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                q.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp,
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                        // options
                        items(q.options.size) { oi ->
                            val att = state.attempts[q.id]
                            val selected = att?.selectedOption == oi
                            OptionRow(
                                letter = LETTERS.getOrElse(oi) { "${oi + 1}" },
                                text = q.options[oi],
                                selected = selected,
                                onSelect = { rvm.selectOption(q.id, oi) },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        item {
                            val att = state.attempts[q.id]
                            val isLast = state.currentIndex == state.questions.lastIndex
                            Spacer(Modifier.height(6.dp))

                            // v2.3.16 — 4-button action grid (user request):
                            //   [ Previous ] [ Next ]      ← navigation row
                            //   [   Mark  ] [ Clear ]      ← answer-state row
                            // Previous sits above Mark, Next above Clear; the
                            // last question's Next becomes Submit.
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // previous — disabled on the first question
                                val canPrev = state.currentIndex > 0
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .pressableScale(0.97f) { if (canPrev) rvm.prev() }
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(13.dp))
                                        .border(
                                            1.dp,
                                            if (canPrev) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            RoundedCornerShape(13.dp),
                                        )
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.ArrowBackIos,
                                            contentDescription = "Previous question",
                                            tint = if (canPrev) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Previous",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (canPrev) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        )
                                    }
                                }
                                // next — becomes Submit on the last question
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .pressableScale(0.97f) { if (isLast) rvm.setSubmit(true) else rvm.next() }
                                        .background(
                                            if (isLast) TdExt.colors.brandBrush
                                            else androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
                                            ),
                                            RoundedCornerShape(13.dp),
                                        )
                                        .border(
                                            1.dp,
                                            if (isLast) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(13.dp),
                                        )
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (isLast) "Submit" else "Next",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                            ),
                                            color = if (isLast) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            if (isLast) Icons.Rounded.Flag else Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                            contentDescription = if (isLast) "Submit test" else "Next question",
                                            tint = if (isLast) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(if (isLast) 15.dp else 14.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // mark for review
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .pressableScale(0.97f) { rvm.toggleMark(q.id) }
                                        .background(
                                            if (att?.status?.isMarked == true) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(13.dp),
                                        )
                                        .border(
                                            1.dp,
                                            if (att?.status?.isMarked == true) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(13.dp),
                                        )
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = if (att?.status?.isMarked == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Mark",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (att?.status?.isMarked == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                // clear
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .pressableScale(0.97f) { rvm.clearOption(q.id) }
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(13.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp))
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "Clear",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── bottom bar ──────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // prev
            Box(
                Modifier
                    .size(48.dp, 46.dp)
                    .pressableScale(0.94f) { rvm.prev() }
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "Previous",
                    tint = if (state.currentIndex == 0) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
            // palette
            Box(
                Modifier
                    .weight(1f)
                    .pressableScale(0.97f) { rvm.setPalette(true) }
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(13.dp))
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Grid3x3, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Question Palette", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // submit
            Box(
                Modifier
                    .pressableScale(0.96f) { rvm.setSubmit(true) }
                    .background(TdExt.colors.brandBrush, RoundedCornerShape(13.dp))
                    .padding(horizontal = 20.dp, vertical = 13.dp),
            ) {
                Text(
                    "Submit",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }
    }

    // ── palette bottom sheet ────────────────────────────────────────────────
    if (state.showPalette) {
        PaletteSheet(
            state = state,
            onJump = { rvm.jumpToQuestion(it) },
            onClose = { rvm.setPalette(false) },
        )
    }

    // ── submit dialog ───────────────────────────────────────────────────────
    if (state.showSubmit) {
        ConfirmDialog(
            title = "Submit test?",
            message = "Once submitted you can't change your answers.",
            confirmLabel = "Submit now",
            dismissLabel = "Keep going",
            onConfirm = { rvm.submit() },
            onDismiss = { rvm.setSubmit(false) },
        )
    }

    // ── exit dialog ─────────────────────────────────────────────────────────
    if (state.showExit) {
        ConfirmDialog(
            title = "Exit test?",
            message = "Your progress in this test will be lost.",
            confirmLabel = "Discard",
            dismissLabel = "Continue test",
            danger = true,
            onConfirm = {
                rvm.discard {
                    navController.popBackStack()
                }
            },
            onDismiss = { rvm.setExit(false) },
        )
    }
}

// ── pieces ──────────────────────────────────────────────────────────────────

@Composable
private fun OptionRow(letter: String, text: String, selected: Boolean, onSelect: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .fillMaxWidth()
            .pressableScale(0.98f, onSelect)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(15.dp),
            )
            .border(1.5.dp, border, RoundedCornerShape(15.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(
                        if (selected) TdExt.colors.brandBrush else androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh),
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letter,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun PaletteSheet(
    state: RunnerViewModel.State,
    onJump: (String) -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Question Palette", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(32.dp)
                        .pressableScale(0.9f, onClose)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            // legend
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                LegendChip("Answered", TdExt.colors.success)
                LegendChip("Not answered", TdExt.colors.danger)
                LegendChip("Marked", MaterialTheme.colorScheme.secondary)
                LegendChip("Visited", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(44.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(340.dp),
            ) {
                items(state.questions.size) { i ->
                    val q = state.questions[i]
                    val att = state.attempts[q.id]
                    val number = i + 1
                    val (bg, fg) = when {
                        att == null || att.status == QuestionStatus.NOT_VISITED -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
                        att.status.isMarked -> MaterialTheme.colorScheme.secondary to Color.White
                        att.status.isAnswered -> TdExt.colors.success to Color.White
                        att.status == QuestionStatus.NOT_ANSWERED -> TdExt.colors.danger to Color.White
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val isCurrent = state.currentIndex == i
                    Box(
                        Modifier
                            .size(44.dp)
                            .pressableScale(0.88f) {
                                onJump(q.id)
                                onClose()
                            }
                            .background(bg, RoundedCornerShape(13.dp))
                            .border(
                                2.dp,
                                if (isCurrent) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(13.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$number",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = fg,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .padding(22.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(if (danger) TdExt.colors.dangerDim else TdExt.colors.warningDim, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (danger) Icons.Rounded.Close else Icons.Rounded.Flag,
                        contentDescription = null,
                        tint = if (danger) TdExt.colors.danger else TdExt.colors.warning,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                TdButton(confirmLabel, onClick = onConfirm, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(
                    dismissLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .pressableScale(0.96f, onDismiss)
                        .padding(10.dp),
                )
            }
        }
    }
}
