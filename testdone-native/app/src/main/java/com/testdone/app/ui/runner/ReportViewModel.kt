package com.testdone.app.ui.runner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testdone.app.di.AppContainer
import com.testdone.app.domain.model.Question
import com.testdone.app.domain.model.QuestionAttempt
import com.testdone.app.domain.model.TestAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportViewModel(private val container: AppContainer) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val attempt: TestAttempt? = null,
        val reviewQuestions: List<Pair<Question, QuestionAttempt?>> = emptyList(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun load(attemptId: String) {
        if (_state.value.attempt?.attemptId == attemptId) return
        viewModelScope.launch {
            val attempt = container.attemptRepository.byId(attemptId)
            if (attempt == null) {
                _state.value = State(loading = false)
                return@launch
            }
            val test = container.contentRepository.testById(attempt.testId)
            val questions = if (test != null) {
                container.contentRepository.questionsByIds(test.questionIds)
            } else emptyList()
            _state.value = State(
                loading = false,
                attempt = attempt,
                reviewQuestions = questions.map { it to attempt.questionAttempts[it.id] },
            )
        }
    }
}
