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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.testdone.app.domain.model.TestKind
import com.testdone.app.domain.model.TestMeta
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.ExamLogoBadge
import com.testdone.app.ui.components.Pill
import com.testdone.app.ui.components.SkeletonList
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.cssGradient
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.runtime.remember

/** Mock/PYQ test list for one exam. */
@Composable
fun TestListScreen(navController: NavHostController, vm: AppViewModel, examId: String) {
    val tvm: TestsViewModel = viewModel(factory = com.testdone.app.ui.simpleFactory { TestsViewModel(vm.container) })
    val state by tvm.state.collectAsState()
    var filter by rememberSaveable { mutableStateOf("mock") }
    val user by vm.user.collectAsState()

    val exam = vm.container.contentRepository.examById(examId)
    LaunchedEffect(examId, filter) {
        tvm.loadTests(examId, if (filter == "pyq") TestKind.PYQ else TestKind.MOCK)
    }

    val examAttempts = remember(state.attemptedTestIds) { state.attemptedTestIds }
    val visibleTests = state.tests
    val isFirstOfKind = remember(filter) {
        // free badge logic: first test of each kind is free for free users
        true
    }

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
                .padding(top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(Modifier.weight(1f)) {
                Text(exam?.shortName ?: "Tests", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${exam?.mockCount ?: 0} mocks · ${exam?.pyqCount ?: 0} PYQ sets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // filter tabs
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterTab("mock", "Mock Tests", filter == "mock") { filter = "mock" }
            FilterTab("pyq", "PYQ Papers", filter == "pyq") { filter = "pyq" }
        }

        if (state.loading) {
            SkeletonList(5)
        } else if (visibleTests.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (filter == "pyq") "No PYQ sets yet" else "No mock tests yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp),
            ) {
                items(visibleTests, key = { it.id }) { test ->
                    TestCard(
                        test = test,
                        examId = examId,
                        attempted = test.id in examAttempts,
                        showFreeBadge = !user.planActive,
                        isFirstOfKind = isFirstOfKind && visibleTests.firstOrNull { it.kind == test.kind }?.id == test.id,
                        onClick = { navController.navigate(Routes.instructions(examId, test.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTab(id: String, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .pressableScale(0.95f, onClick)
            .background(
                if (active) TdExt.colors.brandBrush
                else androidx.compose.ui.graphics.Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)),
                RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TestCard(
    test: TestMeta,
    examId: String,
    attempted: Boolean,
    showFreeBadge: Boolean,
    isFirstOfKind: Boolean,
    onClick: () -> Unit,
) {
    val pyq = test.kind == TestKind.PYQ
    Box(
        Modifier
            .fillMaxWidth()
            .pressableScale(0.97f, onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                // exam emblem on a gradient disc — real-logo feel
                ExamLogoBadge(
                    examId = examId,
                    size = 54.dp,
                    ringBrush = if (pyq) TdExt.colors.premiumBrush else TdExt.colors.brandBrush,
                )
                if (attempted) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .background(TdExt.colors.success, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Attempted", tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        test.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (showFreeBadge) {
                        Pill(
                            if (isFirstOfKind) "Free" else "Pro",
                            if (isFirstOfKind) TdExt.colors.success else TdExt.colors.warning,
                            if (isFirstOfKind) TdExt.colors.successDim else TdExt.colors.warningDim,
                        )
                    }
                    if (test.year != null) {
                        Spacer(Modifier.width(4.dp))
                        Pill("${test.year}", TdExt.colors.warning, TdExt.colors.warningDim)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    test.desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconLabel(Icons.Rounded.Schedule, "${test.durationMin}m", MaterialTheme.colorScheme.primary)
                    IconLabel(Icons.Rounded.MenuBook, "${test.totalQuestions} Qs", TdExt.colors.success)
                    IconLabel(Icons.Rounded.Bolt, "${test.maxMarks.toInt()}", TdExt.colors.warning)
                    if (test.neg > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.RemoveCircle, contentDescription = null, tint = TdExt.colors.danger.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                            Text(
                                " ${test.neg.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TdExt.colors.danger.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun IconLabel(icon: ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        Text(
            " $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
