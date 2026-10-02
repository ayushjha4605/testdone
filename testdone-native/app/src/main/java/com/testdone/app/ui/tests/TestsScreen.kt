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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.testdone.app.domain.model.Exam
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.LocalMenuOpener
import com.testdone.app.ui.MenuButton
import com.testdone.app.ui.components.ExamLogoBadge
import com.testdone.app.ui.components.Pill
import com.testdone.app.ui.components.SkeletonList
import com.testdone.app.ui.components.cssGradient
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.nav.Routes
import com.testdone.app.ui.theme.TdExt
import androidx.compose.ui.unit.sp

/** Tests tab — exams browser (search + categories + your-exam card). */
@Composable
fun TestsScreen(navController: NavHostController, vm: AppViewModel) {
    val tvm: TestsViewModel = viewModel(factory = com.testdone.app.ui.simpleFactory { TestsViewModel(vm.container) })
    val state by tvm.state.collectAsState()
    val openMenu = LocalMenuOpener.current
    val totalMocks = vm.container.contentRepository.totalMockTests
    val totalPyq = vm.container.contentRepository.totalPyqSets

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
                Text("Mock Tests", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${state.exams.size} exams · $totalMocks mocks · $totalPyq PYQ sets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MenuButton(onOpen = openMenu)
        }

        // search
        OutlinedTextField(
            value = state.search,
            onValueChange = { tvm.setSearch(it) },
            placeholder = { Text("Search exams (JEE, NEET, UPSC, Banking…)", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = if (state.search.isNotEmpty()) {
                { Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.pressableScale(0.9f) { tvm.setSearch("") }) }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        // your exam card
        val userExam = state.userExamId?.let { id -> vm.container.contentRepository.examById(id) }
        if (userExam != null && state.search.isBlank()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .pressableScale(0.98f) {
                        tvm.selectExam(userExam.id)
                        navController.navigate(Routes.testList(userExam.id))
                    }
                    .background(cssGradient(userExam.gradientCss, listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))), RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExamLogoBadge(examId = userExam.id, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("YOUR EXAM", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Color.White.copy(alpha = 0.7f))
                        Text(userExam.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("${userExam.mockCount} mocks · ${userExam.pyqCount} PYQ sets", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                    }
                    Box(
                        Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Continue", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }
                }
            }
        }

        // category chips
        if (state.search.isBlank()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                items(listOf("all" to "All") + state.categories.map { it.id to "${it.icon} ${it.label}" }) { (id, label) ->
                    val active = state.selectedCategory == id
                    Box(
                        Modifier
                            .pressableScale(0.94f) { tvm.setCategory(id) }
                            .background(
                                if (active) TdExt.colors.brandBrush
                                else androidx.compose.ui.graphics.Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            Text(
                "${state.exams.size} exam${if (state.exams.size != 1) "s" else ""} found",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        // exam grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
        ) {
            items(state.exams, key = { it.id }) { exam ->
                ExamCard(exam) {
                    tvm.selectExam(exam.id)
                    navController.navigate(Routes.testList(exam.id))
                }
            }
        }
    }
}

@Composable
private fun ExamCard(exam: Exam, onClick: () -> Unit) {
    Box(
        Modifier
            .pressableScale(0.95f, onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column {
            ExamLogoBadge(examId = exam.id, size = 48.dp)
            Spacer(Modifier.height(8.dp))
            Text(exam.shortName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val (diffColor, diffBg) = when (exam.difficulty.lowercase()) {
                    "easy" -> TdExt.colors.success to TdExt.colors.successDim
                    "moderate" -> TdExt.colors.warning to TdExt.colors.warningDim
                    "hard" -> TdExt.colors.danger to TdExt.colors.dangerDim
                    else -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiaryContainer
                }
                Pill(exam.difficulty, diffColor, diffBg)
                Text("${exam.subjects.size} subj", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${exam.mockCount + exam.pyqCount} tests",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}
