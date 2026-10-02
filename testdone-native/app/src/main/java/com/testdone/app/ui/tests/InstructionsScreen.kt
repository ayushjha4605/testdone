package com.testdone.app.ui.tests

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.testdone.app.domain.model.Question
import com.testdone.app.domain.model.TestMeta
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.ExamLogoBadge
import com.testdone.app.ui.components.cssGradient
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.TrackChanges

/** Pre-test instructions screen (port of the web Instructions view). */
@Composable
fun InstructionsScreen(navController: NavHostController, vm: AppViewModel, examId: String, testId: String) {
    var test by remember { mutableStateOf<TestMeta?>(null) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val exam = vm.container.contentRepository.examById(examId)
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(testId) {
        val t = vm.container.contentRepository.testById(testId)
        test = t
        questions = t?.let { vm.container.contentRepository.questionsByIds(it.questionIds) } ?: emptyList()
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Back · tests",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            return@Column
        }

        val t = test ?: return@Column

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // header card
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(cssGradient(exam?.gradientCss, listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))), RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExamLogoBadge(examId = exam?.id, size = 34.dp)
                        if (t.kind == com.testdone.app.domain.model.TestKind.PYQ) {
                            Box(
                                Modifier
                                    .background(Color(0xFFFCD34D), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    "PYQ ${t.year ?: ""}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(t.title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(exam?.name ?: "", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // info cards
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoCard(Icons.Rounded.Schedule, "${t.durationMin}m", "Duration", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                InfoCard(Icons.Rounded.MenuBook, "${t.totalQuestions}", "Questions", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoCard(Icons.Rounded.Bolt, "${t.maxMarks.toInt()}", "Max Marks", TdExt.colors.warning, Modifier.weight(1f))
                InfoCard(
                    if (t.neg > 0) Icons.Rounded.RemoveCircle else Icons.Rounded.Star,
                    if (t.neg > 0) "−${t.neg.toInt()}" else "None",
                    "Negative Marking",
                    if (t.neg > 0) TdExt.colors.danger else TdExt.colors.success,
                    Modifier.weight(1f),
                )
            }

            // attempt-any note
            if (t.attempt != null && t.attempt < t.totalQuestions) {
                Spacer(Modifier.height(12.dp))
                InfoNote("Attempt any ${t.attempt} of ${t.totalQuestions} questions in this paper.")
            }

            Spacer(Modifier.height(16.dp))

            // sections
            val subjectCounts = questions.groupingBy { it.subjectName }.eachCount()
            SectionCard("Sections in this test") {
                subjectCounts.entries.forEachIndexed { i, (name, count) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text("$count Qs", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard("General instructions") {
                Bullet("The timer starts as soon as you begin the test.")
                Bullet("Each question has 4–5 options. Only one is correct.")
                Bullet(
                    if (t.neg > 0) "Negative marking: ${t.neg} mark(s) deducted per wrong answer."
                    else "No negative marking — attempt every question.",
                )
                Bullet("You can mark questions for review and revisit them later.")
                Bullet("The test auto-submits when the timer runs out.")
                Bullet("Detailed solutions will be available after submission.")
            }

            Spacer(Modifier.height(24.dp))
        }

        // ── Start Test (v2.3.15 layout fix) ───────────────────────────────────
        // The old bottom = 100.dp padding was tuned for gesture-nav devices;
        // with 3-button nav (≈48dp inset) the app's bottom bar measured ~122dp
        // and swallowed the lower half of the button ("start test aadha chup
        // jata hai"). Instructions now hides the bottom bar (focused pre-test
        // flow, same as the runner) and the button clears the SYSTEM nav bar
        // via navigationBarsPadding — correct on every device.
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                        0.35f to MaterialTheme.colorScheme.background,
                        1f to MaterialTheme.colorScheme.background,
                    ),
                )
                .padding(top = 10.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp),
            ) {
                TdButton(
                    text = "Start Test",
                    icon = Icons.Rounded.PlayCircle,
                    onClick = { navController.navigate(Routes.runner(examId, testId)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column {
            Box(
                Modifier
                    .size(34.dp)
                    .background(tint.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoNote(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•  ", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope
