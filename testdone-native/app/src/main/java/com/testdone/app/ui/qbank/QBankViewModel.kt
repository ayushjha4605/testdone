package com.testdone.app.ui.qbank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testdone.app.di.AppContainer
import com.testdone.app.domain.model.Exam
import com.testdone.app.domain.model.Question
import com.testdone.app.ui.tests.TestsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QBankViewModel(private val container: AppContainer) : ViewModel() {

    data class Topic(val name: String, val count: Int)

    data class State(
        val exams: List<Exam> = emptyList(),
        val search: String = "",
        val activeExamId: String? = null,
        val subjects: List<Pair<String, String>> = emptyList(), // id → name
        val selectedSubject: String? = null,
        val topics: List<Topic> = emptyList(),
        val selectedTopic: String? = null,
        val questions: List<Question> = emptyList(),
        val total: Int = 0,
        val page: Int = 0,
        val loading: Boolean = false,
    ) {
        val pageSize = 10
        val hasMore: Boolean get() = (page + 1) * pageSize < total
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        viewModelScope.launch {
            container.settings.user.collect { user ->
                if (_state.value.activeExamId == null && user.selectedExamId != null) {
                    openExam(user.selectedExamId!!)
                }
            }
        }
    }

    fun setSearch(q: String) { _state.value = _state.value.copy(search = q) }

    fun searchExams(): List<Exam> = container.contentRepository.searchExams(_state.value.search)

    fun closeExam() {
        _state.value = _state.value.copy(
            activeExamId = null, selectedSubject = null, selectedTopic = null,
            questions = emptyList(), topics = emptyList(), page = 0, total = 0,
        )
    }

    fun openExam(examId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(activeExamId = examId, selectedSubject = null, selectedTopic = null, questions = emptyList(), page = 0)
            _state.value = _state.value.copy(subjects = container.contentRepository.subjectsForExam(examId))
            // All-questions default: the bank is populated immediately on
            // open — the subject chips act as optional filters on top.
            loadQuestions()
        }
    }

    fun selectSubject(subjectId: String?) {
        val examId = _state.value.activeExamId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(selectedSubject = subjectId, selectedTopic = null, page = 0, questions = emptyList())
            if (subjectId != null) {
                val topics = container.contentRepository.subjectTopics(examId, subjectId)
                _state.value = _state.value.copy(topics = topics.map { Topic(it.name, it.count) })
            } else {
                _state.value = _state.value.copy(topics = emptyList())
            }
            loadQuestions()
        }
    }

    fun selectTopic(topic: String?) {
        _state.value = _state.value.copy(selectedTopic = topic, page = 0, questions = emptyList())
        loadQuestions()
    }

    fun loadMore() {
        if (!_state.value.hasMore) return
        _state.value = _state.value.copy(page = _state.value.page + 1)
        loadQuestions(append = true)
    }

    private fun loadQuestions(append: Boolean = false) {
        val s = _state.value
        val examId = s.activeExamId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val page = container.contentRepository.qbankQuestions(
                examId = examId,
                subjectId = s.selectedSubject, // null → all questions for the exam
                topic = s.selectedTopic,
                limit = s.pageSize,
                offset = if (append) (s.page) * s.pageSize else 0,
            )
            _state.value = _state.value.copy(
                questions = if (append) _state.value.questions + page.questions else page.questions,
                total = page.total,
                loading = false,
            )
        }
    }

    fun refresh() { loadQuestions() }
}
