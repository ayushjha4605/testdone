package com.testdone.app.ui.analytics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.testdone.app.domain.logic.timeAgo
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.LocalMenuOpener
import com.testdone.app.ui.MenuButton
import com.testdone.app.ui.components.ScoreDonut
import com.testdone.app.ui.components.SubjectBarChart
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.components.TrendLineChart
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt
import androidx.compose.material.icons.rounded.AutoAwesome

/** Analytics tab — AI insights, trends, subject strength, history. */
@Composable
fun AnalyticsScreen(navController: NavHostController, vm: AppViewModel) {
    val avm: AnalyticsViewModel = viewModel(factory = com.testdone.app.ui.simpleFactory { AnalyticsViewModel(vm.container) })
    val state by avm.state.collectAsState()
    val openMenu = LocalMenuOpener.current

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Analytics", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "AI-powered insights on your performance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MenuButton(onOpen = openMenu)
            }
        }

        if (state.attempts.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(88.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(26.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.QueryStats, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("No data yet", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Take your first mock test to unlock AI-powered insights, performance trends, and personalized recommendations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Spacer(Modifier.height(20.dp))
                        TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                            Column(Modifier.padding(4.dp)) {
                                Text("What you'll see here:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(8.dp))
                                listOf(
                                    "Performance trend over time",
                                    "Subject-wise strength & weakness",
                                    "AI-generated improvement insights",
                                    "Topper comparison & percentile",
                                ).forEach {
                                    Text("•  $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 3.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        com.testdone.app.ui.components.TdButton(
                            "Take a mock test",
                            onClick = { navController.navigate(Routes.TESTS) },
                            modifier = Modifier.fillMaxWidth(0.8f),
                        )
                    }
                }
            }
        } else {
            val ins = state.insights

            // hero card
            item {
                TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "AVERAGE SCORE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "${ins.avgScore}",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "%",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    if (ins.trend != 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (ins.trend > 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                                contentDescription = null,
                                                tint = if (ins.trend > 0) TdExt.colors.success else TdExt.colors.danger,
                                                modifier = Modifier.size(15.dp).padding(bottom = 6.dp),
                                            )
                                            Text(
                                                "${if (ins.trend > 0) "+" else ""}${ins.trend}%",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (ins.trend > 0) TdExt.colors.success else TdExt.colors.danger,
                                                modifier = Modifier.padding(bottom = 6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            Box(Modifier.size(84.dp)) {
                                ScoreDonut(percent = ins.avgScore, modifier = Modifier.fillMaxSize(), stroke = 9f)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MiniStat("${ins.totalTests}", "Tests")
                            MiniStat("${ins.avgAccuracy}%", "Accuracy")
                            MiniStat("${ins.bestScore}%", "Best")
                            MiniStat("${ins.totalTimeMin}m", "Time")
                        }
                    }
                }
            }

            // trend chart
            item {
                Spacer(Modifier.height(14.dp))
                TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    Column {
                        Text("Performance Trend", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("Score % across your last tests", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        TrendLineChart(values = state.attempts.reversed().map { it.scorePct.toFloat() })
                    }
                }
            }

            // AI insights
            item {
                Spacer(Modifier.height(14.dp))
                TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                androidx.compose.material.icons.Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(7.dp))
                            Text("AI Insights", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(10.dp))
                        generateInsights(ins, state.attempts).forEach { text ->
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                        .padding(top = 5.dp),
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // subject-wise
            if (ins.subjectPerf.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                        Column {
                            Text("Subject-wise Performance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(12.dp))
                            SubjectBarChart(data = ins.subjectPerf)
                        }
                    }
                }
            }

            // recent attempts
            item {
                Spacer(Modifier.height(14.dp))
                Text("Recent Attempts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
            }
            items(state.attempts.take(10), key = { it.attemptId }) { a ->
                TdCard(
                    Modifier
                        .fillMaxWidth()
                        .pressableScale(0.98f) { navController.navigate(Routes.report(a.attemptId)) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .background(
                                    when {
                                        a.scorePct >= 70 -> TdExt.colors.successDim
                                        a.scorePct >= 40 -> TdExt.colors.warningDim
                                        else -> TdExt.colors.dangerDim
                                    },
                                    RoundedCornerShape(12.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${a.scorePct}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    a.scorePct >= 70 -> TdExt.colors.success
                                    a.scorePct >= 40 -> TdExt.colors.warning
                                    else -> TdExt.colors.danger
                                },
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.testTitle.ifBlank { "Test" }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                            Text(
                                "${a.correct}/${a.totalQuestions} correct · ${a.accuracy.toInt()}% accuracy · ${a.percentile ?: 0.0} %ile",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Text(
                            timeAgo(a.completedAt ?: a.startedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Insight generator — port of the web app's generateInsights(). */
private fun generateInsights(
    ins: AnalyticsViewModel.Insights,
    attempts: List<com.testdone.app.domain.model.TestAttempt>,
): List<String> {
    val out = mutableListOf<String>()
    out.add(
        when {
            ins.avgScore >= 70 -> "Strong performance — you're averaging ${ins.avgScore}%. Keep the streak alive with daily mocks."
            ins.avgScore >= 40 -> "Solid progress at ${ins.avgScore}% average. Focus on accuracy to cross 70%."
            else -> "You're at ${ins.avgScore}% average. Start with subject-wise practice to build fundamentals."
        }
    )
    if (ins.trend > 0) out.add("Improving — last test was ${ins.trend}% better than the previous one. Momentum is with you!")
    else if (ins.trend < 0) out.add("Last test dipped ${-ins.trend}%. Review the solutions and retry the weak topics.")
    val weakest = ins.subjectPerf.filter { it.second < 50 }.sortedBy { it.second }.firstOrNull()
    if (weakest != null) out.add("${weakest.first} needs attention (${weakest.second}% accuracy) — practice it in QBank.")
    val strongest = ins.subjectPerf.maxByOrNull { it.second }
    if (strongest != null && strongest.second >= 60) out.add("${strongest.first} is your strongest subject (${strongest.second}%) — bank marks here on exam day.")
    if (attempts.size >= 5) out.add("${attempts.size} tests done — consistency like this beats last-minute cramming.")
    if (ins.streak >= 2) out.add("${ins.streak}-day streak! Daily practice compounds — don't break the chain.")
    return out
}
