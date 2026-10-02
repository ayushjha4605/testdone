package com.testdone.app.ui.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.testdone.app.domain.logic.formatDuration
import com.testdone.app.domain.model.Question
import com.testdone.app.domain.model.QuestionAttempt
import com.testdone.app.domain.model.TestAttempt
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.Confetti
import com.testdone.app.ui.components.CountUpText
import com.testdone.app.ui.components.Pill
import com.testdone.app.ui.components.ScoreDonut
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt
import androidx.compose.runtime.setValue

/** Post-test report: score, sections, insights + full solution review. */
@Composable
fun ReportScreen(navController: NavHostController, vm: AppViewModel, attemptId: String) {
    val rvm: ReportViewModel = viewModel(factory = com.testdone.app.ui.simpleFactory { ReportViewModel(vm.container) })
    val state by rvm.state.collectAsState()

    LaunchedEffect(attemptId) { rvm.load(attemptId) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val attempt = state.attempt
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            attempt != null -> {
                Column(Modifier.fillMaxSize()) {
                    LazyColumn(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 30.dp),
                    ) {
                        // header
                        item {
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Spacer(Modifier.statusBarsPadding().height(20.dp))
                                Box(
                                    Modifier
                                        .size(62.dp)
                                        .background(
                                            Brush.radialGradient(listOf(Color(0x336366F1), Color.Transparent)),
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.EmojiEvents,
                                        contentDescription = null,
                                        tint = TdExt.colors.warning,
                                        modifier = Modifier.size(44.dp),
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Test Complete!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    attempt.testTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        // score hero card
                        item {
                            Spacer(Modifier.height(18.dp))
                            TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                                Column(
                                    Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        Modifier
                                            .size(120.dp)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        ScoreDonut(percent = attempt.scorePct, modifier = Modifier.fillMaxSize())
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        CountUpText(
                                            value = attempt.score.toInt(),
                                            style = MaterialTheme.typography.displaySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            " / ${attempt.maxScore.toInt()} marks",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
                                        StatCell("${attempt.percentile ?: 0.0}", "Percentile")
                                        StatCell("#${attempt.rank ?: "-"}", "Est. AIR")
                                        StatCell("${attempt.accuracy.toInt()}%", "Accuracy")
                                        StatCell(formatDuration(attempt.durationSec), "Time")
                                    }
                                }
                            }
                        }

                        // counts row
                        item {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CountCard(Icons.Rounded.CheckCircle, attempt.correct, "Correct", TdExt.colors.success, Modifier.weight(1f))
                                CountCard(Icons.Rounded.Close, attempt.incorrect, "Wrong", TdExt.colors.danger, Modifier.weight(1f))
                                CountCard(Icons.Rounded.Schedule, attempt.totalQuestions - attempt.attempted, "Skipped", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                            }
                        }

                        // section performance
                        if (attempt.sectionResults.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(18.dp))
                                Text("Section Performance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(8.dp))
                                TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                                    Column {
                                        attempt.sectionResults.forEach { sec ->
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    sec.subjectName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                )
                                                val pct = if (sec.total > 0) sec.correct * 100 / sec.total else 0
                                                Box(
                                                    Modifier
                                                        .width(110.dp)
                                                        .height(8.dp)
                                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(50)),
                                                ) {
                                                    Box(
                                                        Modifier
                                                            .fillMaxWidth(pct / 100f)
                                                            .height(8.dp)
                                                            .background(TdExt.colors.brandBrush, RoundedCornerShape(50)),
                                                    )
                                                }
                                                Text(
                                                    " ${sec.correct}/${sec.total}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // solutions review
                        item {
                            Spacer(Modifier.height(18.dp))
                            Text("Solutions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Review every question with detailed explanations",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        items(state.reviewQuestions, key = { it.first.id }) { (q, att) ->
                            SolutionCard(q = q, att = att)
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    // done button
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .navigationBarsPadding()
                            .padding(16.dp),
                    ) {
                        TdButton(
                            text = "Done — View Analytics",
                            icon = Icons.Rounded.TrendingUp,
                            onClick = {
                                navController.navigate(Routes.ANALYTICS) {
                                    popUpTo(Routes.HOME)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // celebrate 🎉
                Confetti()
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Report not found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    TdButton("Back", onClick = { navController.popBackStack() }, style = com.testdone.app.ui.components.TdButtonStyle.SECONDARY)
                }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CountCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(5.dp))
            Text("$value", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val LETTERS = listOf("A", "B", "C", "D", "E")

@Composable
private fun SolutionCard(q: Question, att: QuestionAttempt?) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val answered = att?.selectedOption
    val isCorrect = att?.isCorrect == true

    TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(
                            when {
                                isCorrect -> TdExt.colors.success
                                answered != null -> TdExt.colors.danger
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        when {
                            isCorrect -> Icons.Rounded.Check
                            answered != null -> Icons.Rounded.Close
                            else -> Icons.Rounded.Schedule
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Q${q.id.substringAfter("_q").toIntOrNull()?.plus(1) ?: ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    q.topic,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(q.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)

            Spacer(Modifier.height(10.dp))
            q.options.forEachIndexed { i, opt ->
                val isCorrectOption = i == q.correct
                val isSelected = answered == i
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .background(
                                when {
                                    isCorrectOption -> TdExt.colors.success
                                    isSelected -> TdExt.colors.danger
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            LETTERS.getOrElse(i) { "${i + 1}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCorrectOption || isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        opt,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isCorrectOption -> TdExt.colors.success
                            isSelected -> TdExt.colors.danger
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (expanded) "Hide solution" else "Show solution",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .pressableScale(0.96f) { expanded = !expanded }
                    .padding(4.dp),
            )
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Row {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = TdExt.colors.warning, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            q.solution,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
