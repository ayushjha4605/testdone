package com.testdone.app.ui.runner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testdone.app.di.AppContainer
import com.testdone.app.domain.logic.estimateAir
import com.testdone.app.domain.logic.estimatePercentile
import com.testdone.app.domain.model.Question
import com.testdone.app.domain.model.QuestionAttempt
import com.testdone.app.domain.model.QuestionStatus
import com.testdone.app.domain.model.SectionResult
import com.testdone.app.domain.model.TestAttempt
import com.testdone.app.domain.model.TestKind
import com.testdone.app.domain.model.TestMeta
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Full test-session state machine: questions, per-question attempts & timing,
 * palette navigation, negative-marking scoring, auto-submit, and result saving.
 */
class RunnerViewModel(private val container: AppContainer) : ViewModel() {

    enum class Stage { LOADING, RUNNING, SUBMITTING, DONE }

    data class State(
        val stage: Stage = Stage.LOADING,
        val test: TestMeta? = null,
        val questions: List<Question> = emptyList(),
        val subjectOrder: List<Pair<String, String>> = emptyList(), // id → name
        val attemptId: String = "",
        val startedAt: Long = 0,
        val durationSec: Int = 0,
        val elapsedSec: Int = 0,
        val currentIndex: Int = 0,
        val attempts: Map<String, QuestionAttempt> = emptyMap(),
        val showPalette: Boolean = false,
        val showSubmit: Boolean = false,
        val showExit: Boolean = false,
        val savedAttemptId: String? = null,
        val loadFailed: Boolean = false,
    ) {
        val remainingSec: Int get() = (durationSec - elapsedSec).coerceAtLeast(0)
        val currentQuestion: Question? get() = questions.getOrNull(currentIndex)
        val currentSectionId: String? get() = currentQuestion?.subjectId
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private var timerJob: Job? = null
    private val perQuestionSec = HashMap<String, Int>()
    private var lastTickAt = 0L

    fun load(examId: String, testId: String) {
        if (_state.value.stage != Stage.LOADING) return
        viewModelScope.launch {
            try {
                val test = container.contentRepository.testById(testId)
                    ?: throw IllegalStateException("test not found")
                val questions = container.contentRepository.questionsByIds(test.questionIds)
                if (questions.isEmpty()) throw IllegalStateException("no questions")
                val subjects = container.contentRepository.subjectsForExam(examId)
                _state.value = State(
                    stage = Stage.RUNNING,
                    test = test,
                    questions = questions,
                    subjectOrder = subjects,
                    attemptId = "att_${System.currentTimeMillis()}",
                    startedAt = System.currentTimeMillis(),
                    durationSec = test.durationMin * 60,
                    attempts = questions.associate { it.id to QuestionAttempt(it.id, null, QuestionStatus.NOT_VISITED) },
                )
                startTimer()
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadFailed = true)
            }
        }
    }

    private fun startTimer() {
        lastTickAt = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.stage == Stage.RUNNING) {
                delay(1000)
                val now = System.currentTimeMillis()
                val delta = ((now - lastTickAt) / 1000L).toInt().coerceIn(1, 5)
                lastTickAt = now
                val s = _state.value
                if (s.stage != Stage.RUNNING) break
                val qid = s.currentQuestion?.id
                if (qid != null) perQuestionSec[qid] = (perQuestionSec[qid] ?: 0) + delta
                val newElapsed = s.elapsedSec + delta
                if (newElapsed >= s.durationSec) {
                    _state.value = s.copy(elapsedSec = s.durationSec)
                    submit(auto = true)
                    break
                }
                _state.value = s.copy(elapsedSec = newElapsed)
            }
        }
    }

    // ── interactions ────────────────────────────────────────────────────────

    fun selectOption(questionId: String, option: Int) {
        mutateAttempt(questionId) { cur ->
            val wasMarked = cur.status.isMarked
            cur.copy(selectedOption = option, status = if (wasMarked) QuestionStatus.MARKED_ANSWERED else QuestionStatus.ANSWERED)
        }
    }

    fun clearOption(questionId: String) {
        mutateAttempt(questionId) { cur ->
            val wasMarked = cur.status.isMarked
            cur.copy(selectedOption = null, status = if (wasMarked) QuestionStatus.MARKED else QuestionStatus.NOT_ANSWERED)
        }
    }

    fun toggleMark(questionId: String) {
        mutateAttempt(questionId) { cur ->
            val next = when {
                cur.status == QuestionStatus.MARKED ->
                    if (cur.selectedOption != null) QuestionStatus.ANSWERED else QuestionStatus.NOT_ANSWERED
                cur.status == QuestionStatus.MARKED_ANSWERED -> QuestionStatus.ANSWERED
                cur.status == QuestionStatus.ANSWERED -> QuestionStatus.MARKED_ANSWERED
                else ->
                    if (cur.selectedOption != null) QuestionStatus.MARKED_ANSWERED else QuestionStatus.MARKED
            }
            cur.copy(status = next)
        }
    }

    private fun mutateAttempt(questionId: String, transform: (QuestionAttempt) -> QuestionAttempt) {
        _state.value = _state.value.copy(
            attempts = _state.value.attempts.toMutableMap().apply {
                get(questionId)?.let { put(questionId, transform(it)) }
            },
        )
    }

    fun goTo(index: Int, closePalette: Boolean = true) {
        val s = _state.value
        if (index in s.questions.indices) {
            // mark visited
            val qid = s.questions[index].id
            val att = s.attempts[qid]
            if (att?.status == QuestionStatus.NOT_VISITED) {
                mutateAttempt(qid) { it.copy(status = QuestionStatus.NOT_ANSWERED) }
            }
            _state.value = _state.value.copy(
                currentIndex = index,
                showPalette = if (closePalette) false else s.showPalette,
            )
        }
    }

    fun next() = goTo(_state.value.currentIndex + 1)
    fun prev() = goTo(_state.value.currentIndex - 1)
    fun setPalette(show: Boolean) { _state.value = _state.value.copy(showPalette = show) }
    fun setSubmit(show: Boolean) { _state.value = _state.value.copy(showSubmit = show) }
    fun setExit(show: Boolean) { _state.value = _state.value.copy(showExit = show) }

    fun jumpToQuestion(questionId: String) {
        val idx = _state.value.questions.indexOfFirst { it.id == questionId }
        if (idx >= 0) goTo(idx)
    }

    // ── scoring ─────────────────────────────────────────────────────────────

    private fun computeResult(): TestAttempt? {
        val s = _state.value
        val test = s.test ?: return null
        var correct = 0
        var incorrect = 0
        var attempted = 0
        var score = 0.0
        val sectionAgg = LinkedHashMap<String, Triple<String, Int, Int>>() // subjectId → (name, correct, total)
        for ((sid, name) in s.subjectOrder) sectionAgg[sid] = Triple(name, 0, 0)
        val sectionTime = HashMap<String, Int>()
        val merged = HashMap<String, QuestionAttempt>()

        for (q in s.questions) {
            val att = s.attempts[q.id] ?: continue
            val time = perQuestionSec[q.id] ?: 0
            val sec = sectionAgg[q.subjectId]
            if (sec != null) sectionAgg[q.subjectId] = sec.copy(third = sec.third + 1)
            sectionTime[q.subjectId] = (sectionTime[q.subjectId] ?: 0) + time
            val answered = att.selectedOption != null
            val isCorrect = answered && att.selectedOption == q.correct
            merged[q.id] = att.copy(timeSpent = time, isCorrect = if (answered) isCorrect else null)
            if (answered) {
                attempted++
                if (isCorrect) {
                    correct++
                    score += q.marks
                    sectionAgg[q.subjectId]?.let { sectionAgg[q.subjectId] = it.copy(second = it.second + 1) }
                } else {
                    incorrect++
                    score -= test.neg
                }
            }
        }

        val totalMarks = s.questions.sumOf { it.marks }
        val finalScore = (Math.round(score * 100) / 100.0).coerceAtLeast(0.0)
        val scorePct = if (totalMarks > 0) finalScore / totalMarks * 100 else 0.0
        val accuracy = if (attempted > 0) correct.toDouble() / attempted * 100 else 0.0
        val percentile = estimatePercentile(scorePct)
        val exam = container.contentRepository.examById(test.examId)
        val air = estimateAir(exam?.candidateBase ?: 100_000L, percentile)

        return TestAttempt(
            attemptId = s.attemptId,
            testId = test.id,
            testTitle = test.title,
            testKind = test.kind,
            examId = test.examId,
            startedAt = s.startedAt,
            completedAt = System.currentTimeMillis(),
            durationSec = s.elapsedSec,
            totalQuestions = s.questions.size,
            attempted = attempted,
            correct = correct,
            incorrect = incorrect,
            score = finalScore,
            maxScore = totalMarks,
            accuracy = accuracy,
            percentile = percentile,
            rank = air,
            sectionResults = sectionAgg.map { (sid, t) ->
                SectionResult(sid, t.first, t.second, t.third, sectionTime[sid] ?: 0)
            },
            questionAttempts = merged,
        )
    }

    fun submit(auto: Boolean = false) {
        val s = _state.value
        if (s.stage != Stage.RUNNING) return
        _state.value = s.copy(stage = Stage.SUBMITTING, showPalette = false, showSubmit = false, showExit = false)
        timerJob?.cancel()
        viewModelScope.launch {
            val result = computeResult()
            if (result != null) {
                container.attemptRepository.save(result)
                _state.value = _state.value.copy(stage = Stage.DONE, savedAttemptId = result.attemptId)
            } else {
                _state.value = _state.value.copy(stage = Stage.RUNNING, showSubmit = false)
            }
        }
    }

    fun discard(onDiscarded: () -> Unit) {
        timerJob?.cancel()
        _state.value = State(stage = Stage.LOADING)
        onDiscarded()
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
