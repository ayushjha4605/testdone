package com.testdone.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testdone.app.di.AppContainer
import com.testdone.app.domain.logic.computeStreak
import com.testdone.app.domain.model.Exam
import com.testdone.app.domain.model.TestAttempt
import com.testdone.app.domain.model.TestKind
import com.testdone.app.domain.model.TestMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    data class State(
        val attempts: List<TestAttempt> = emptyList(),
        val recommended: List<TestMeta> = emptyList(),
        val otherExams: List<Exam> = emptyList(),
        val selectedExam: Exam? = null,
        val streak: Int = 0,
        val avgScore: Int = 0,
        val bestScore: Int = 0,
        /** v2.3.5+: false until the OTA sync delivers the first exam pack. */
        val contentReady: Boolean = true,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        viewModelScope.launch {
            container.contentRepository.contentReady.collect { ready ->
                _state.value = _state.value.copy(contentReady = ready)
            }
        }
        viewModelScope.launch {
            container.attemptRepository.observeAttempts().collect { attempts ->
                _state.value = _state.value.copy(
                    attempts = attempts,
                    streak = computeStreak(attempts.map { it.startedAt }),
                    avgScore = if (attempts.isEmpty()) 0 else attempts.map { it.scorePct }.average().toInt(),
                    bestScore = attempts.maxOfOrNull { it.scorePct } ?: 0,
                )
            }
        }
        viewModelScope.launch {
            container.settings.user.collect { user ->
                val exam = user.selectedExamId?.let { container.contentRepository.examById(it) }
                val recommended = if (user.selectedExamId != null) {
                    container.contentRepository.testsForExam(user.selectedExamId, TestKind.MOCK).take(5)
                } else emptyList()
                _state.value = _state.value.copy(
                    selectedExam = exam,
                    recommended = recommended,
                    otherExams = container.contentRepository.exams.value.filter { it.id != user.selectedExamId },
                )
            }
        }
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
}
