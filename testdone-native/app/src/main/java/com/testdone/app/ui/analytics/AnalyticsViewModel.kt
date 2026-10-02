package com.testdone.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testdone.app.di.AppContainer
import com.testdone.app.domain.model.TestAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(private val container: AppContainer) : ViewModel() {

    data class Insights(
        val avgScore: Int = 0,
        val bestScore: Int = 0,
        val trend: Int = 0, // last - prev
        val totalTests: Int = 0,
        val totalTimeMin: Int = 0,
        val avgAccuracy: Int = 0,
        val subjectPerf: List<Pair<String, Int>> = emptyList(),
        val streak: Int = 0,
    )

    data class State(
        val attempts: List<TestAttempt> = emptyList(),
        val insights: Insights = Insights(),
        val selectedAttemptId: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        viewModelScope.launch {
            container.attemptRepository.observeAttempts().collect { attempts ->
                val subjectPerf = com.testdone.app.domain.logic.subjectPerformance(attempts)
                val scores = attempts.map { it.scorePct }
                val insights = Insights(
                    avgScore = if (scores.isEmpty()) 0 else scores.average().toInt(),
                    bestScore = scores.maxOrNull() ?: 0,
                    trend = if (scores.size >= 2) scores[0] - scores[1] else 0,
                    totalTests = attempts.size,
                    totalTimeMin = attempts.sumOf { it.durationSec } / 60,
                    avgAccuracy = if (attempts.isEmpty()) 0 else attempts.map { it.accuracy }.average().toInt(),
                    subjectPerf = subjectPerf,
                    streak = com.testdone.app.domain.logic.computeStreak(attempts.map { it.startedAt }),
                )
                _state.value = State(attempts = attempts, insights = insights)
            }
        }
    }

    fun selectAttempt(id: String?) {
        _state.value = _state.value.copy(selectedAttemptId = id)
    }
}
