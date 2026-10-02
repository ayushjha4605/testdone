package com.testdone.app.ui.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testdone.app.di.AppContainer
import com.testdone.app.domain.model.Exam
import com.testdone.app.domain.model.TestKind
import com.testdone.app.domain.model.TestMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TestsViewModel(private val container: AppContainer) : ViewModel() {

    data class State(
        val exams: List<Exam> = emptyList(),
        val categories: List<com.testdone.app.domain.model.ExamCategory> = emptyList(),
        val selectedCategory: String = "all",
        val search: String = "",
        val userExamId: String? = null,
        val tests: List<TestMeta> = emptyList(),
        val attemptedTestIds: Set<String> = emptySet(),
        val loading: Boolean = false,
        val failed: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        viewModelScope.launch {
            container.contentRepository.exams.collect { exams ->
                _state.value = _state.value.copy(exams = applyFilter(exams))
            }
        }
        viewModelScope.launch {
            container.contentRepository.categories.collect { _state.value = _state.value.copy(categories = it) }
        }
        viewModelScope.launch {
            container.settings.user.collect { user ->
                _state.value = _state.value.copy(userExamId = user.selectedExamId)
                user.selectedExamId?.let { loadTests(it) }
            }
        }
        viewModelScope.launch {
            container.attemptRepository.observeAttempts().collect { attempts ->
                _state.value = _state.value.copy(attemptedTestIds = attempts.map { it.testId }.toSet())
            }
        }
    }

    private fun applyFilter(exams: List<Exam>): List<Exam> {
        val st = _state.value
        var list = if (st.selectedCategory == "all") exams else exams.filter { it.category == st.selectedCategory }
        if (st.search.isNotBlank()) {
            val q = st.search.lowercase().trim()
            list = list.filter {
                it.name.lowercase().contains(q) || it.shortName.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
        }
        return list
    }

    fun setSearch(q: String) {
        _state.value = _state.value.copy(search = q)
        _state.value = _state.value.copy(exams = applyFilter(container.contentRepository.exams.value))
    }

    fun setCategory(cat: String) {
        _state.value = _state.value.copy(selectedCategory = cat)
        _state.value = _state.value.copy(exams = applyFilter(container.contentRepository.exams.value))
    }

    fun selectExam(examId: String) {
        container.settings.updateUser { it.copy(selectedExamId = examId) }
        val session = container.sessionStore.session.value
        if (session != null) {
            viewModelScope.launch {
                container.profileRepository.updateProfile(
                    session.userId,
                    com.testdone.app.domain.model.ProfilePatch(selectedExamId = examId),
                )
            }
        }
    }

    fun loadTests(examId: String, kind: TestKind = TestKind.MOCK) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, failed = false)
            try {
                val tests = container.contentRepository.testsForExam(examId, kind)
                _state.value = _state.value.copy(tests = tests, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, failed = true)
            }
        }
    }

    fun loadAllTests(examId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, failed = false)
            try {
                val tests = container.contentRepository.testsForExam(examId, null)
                _state.value = _state.value.copy(tests = tests, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, failed = true)
            }
        }
    }
}
