package com.testdone.app.data.repository

import android.content.Context
import com.testdone.app.data.local.dao.AttemptDao
import com.testdone.app.data.local.entity.AttemptEntity
import com.testdone.app.domain.model.QuestionAttempt
import com.testdone.app.domain.model.QuestionStatus
import com.testdone.app.domain.model.SectionResult
import com.testdone.app.domain.model.TestAttempt
import com.testdone.app.domain.model.TestKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Offline-first attempt history: Room is the source of truth, cloud is a mirror. */
class AttemptRepository(
    private val attemptDao: AttemptDao,
    private val profileRepository: FirestoreProfileRepository,
    private val sessionProvider: () -> com.testdone.app.data.local.prefs.SessionStore,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun observeAttempts(): Flow<List<TestAttempt>> =
        attemptDao.observeAttempts().map { rows -> rows.map { it.toDomain() } }

    suspend fun allAttempts(): List<TestAttempt> = attemptDao.allAttempts().map { it.toDomain() }

    suspend fun byId(attemptId: String): TestAttempt? = attemptDao.byId(attemptId)?.toDomain()

    suspend fun save(attempt: TestAttempt) {
        attemptDao.upsertAttempt(attempt.toEntity(synced = false))
        // fire-and-forget cloud push; stays queued locally if it fails
        pushUnsynced()
    }

    /** Push all unsynced attempts to the cloud (on submit / on login / on app start). */
    suspend fun pushUnsynced(): Int = withContext(Dispatchers.IO) {
        val session = sessionProvider().session.value ?: return@withContext 0
        val pending = attemptDao.unsynced()
        if (pending.isEmpty()) return@withContext 0
        var okCount = 0
        val okIds = mutableListOf<String>()
        for (row in pending) {
            val cloud = row.toCloud()
            val ok = runCatching { profileRepository.pushAttempt(session.userId, cloud) }.getOrDefault(false)
            if (ok) { okIds.add(row.attemptId); okCount++ }
        }
        if (okIds.isNotEmpty()) attemptDao.markSynced(okIds)
        okCount
    }

    /** Pull cloud history and merge (cloud authoritative, local-only appended). */
    suspend fun pullAndMergeCloud(): Int = withContext(Dispatchers.IO) {
        val session = sessionProvider().session.value ?: return@withContext 0
        val cloudRows = profileRepository.pullAttempts(session.userId)
        if (cloudRows.isEmpty()) return@withContext 0
        val existing = attemptDao.allAttempts().associateBy { it.attemptId }
        val merged = cloudRows.map { cloud ->
            // keep local per-question data when the row already exists locally
            val local = existing[cloud.attemptId]
            cloud.toEntity(local?.questionAttemptsJson ?: "{}")
        }
        attemptDao.mergeCloud(merged)
        merged.size
    }

    suspend fun clearLocal() = attemptDao.clearAll()
}

// ── mappers ─────────────────────────────────────────────────────────────────

@kotlinx.serialization.Serializable
private data class QAttemptJson(
    val questionId: String,
    val selectedOption: Int? = null,
    val status: String = "NOT_VISITED",
    val timeSpent: Int = 0,
    val isCorrect: Boolean? = null,
)

@kotlinx.serialization.Serializable
private data class SectionResultJson(
    val subjectId: String,
    val subjectName: String,
    val correct: Int = 0,
    val total: Int = 0,
    val timeSpent: Int = 0,
)

fun TestAttempt.toEntity(synced: Boolean): AttemptEntity = AttemptEntity(
    attemptId = attemptId,
    testId = testId,
    testTitle = testTitle,
    testKind = testKind.wire,
    examId = examId,
    startedAt = startedAt,
    completedAt = completedAt,
    durationSec = durationSec,
    totalQuestions = totalQuestions,
    attempted = attempted,
    correct = correct,
    incorrect = incorrect,
    score = score,
    maxScore = maxScore,
    accuracy = accuracy,
    percentile = percentile,
    rank = rank,
    sectionResultsJson = Json.encodeToString(sectionResults.map {
        SectionResultJson(it.subjectId, it.subjectName, it.correct, it.total, it.timeSpent)
    }),
    questionAttemptsJson = Json.encodeToString(questionAttempts.values.map {
        QAttemptJson(it.questionId, it.selectedOption, it.status.name, it.timeSpent, it.isCorrect)
    }),
    synced = synced,
)

fun AttemptEntity.toDomain(): TestAttempt {
    val sections = runCatching {
        Json.decodeFromString<List<SectionResultJson>>(sectionResultsJson)
    }.getOrDefault(emptyList())
    val qAttempts = runCatching {
        Json.decodeFromString<List<QAttemptJson>>(questionAttemptsJson)
    }.getOrDefault(emptyList())
    return TestAttempt(
        attemptId = attemptId,
        testId = testId,
        testTitle = testTitle,
        testKind = if (testKind == "pyq") TestKind.PYQ else TestKind.MOCK,
        examId = examId,
        startedAt = startedAt,
        completedAt = completedAt,
        durationSec = durationSec,
        totalQuestions = totalQuestions,
        attempted = attempted,
        correct = correct,
        incorrect = incorrect,
        score = score,
        maxScore = maxScore,
        accuracy = accuracy,
        percentile = percentile,
        rank = rank,
        sectionResults = sections.map { SectionResult(it.subjectId, it.subjectName, it.correct, it.total, it.timeSpent) },
        questionAttempts = qAttempts.associate {
            it.questionId to QuestionAttempt(
                questionId = it.questionId,
                selectedOption = it.selectedOption,
                status = runCatching { QuestionStatus.valueOf(it.status) }.getOrDefault(QuestionStatus.NOT_VISITED),
                timeSpent = it.timeSpent,
                isCorrect = it.isCorrect,
            )
        },
    )
}

fun AttemptEntity.toCloud(): com.testdone.app.domain.model.CloudAttempt =
    com.testdone.app.domain.model.CloudAttempt(
        attemptId = attemptId,
        testId = testId,
        testTitle = testTitle,
        testKind = testKind,
        examId = examId,
        startedAt = startedAt,
        completedAt = completedAt,
        durationSec = durationSec,
        totalQuestions = totalQuestions,
        attempted = attempted,
        correct = correct,
        incorrect = incorrect,
        score = score,
        maxScore = maxScore,
        accuracy = accuracy,
        percentile = percentile,
        rankEst = rank,
        sectionResults = sectionResultsJson,
    )

fun com.testdone.app.domain.model.CloudAttempt.toEntity(questionAttemptsJson: String = "{}"): AttemptEntity =
    AttemptEntity(
        attemptId = attemptId,
        testId = testId,
        testTitle = testTitle,
        testKind = testKind,
        examId = examId,
        startedAt = startedAt,
        completedAt = completedAt,
        durationSec = durationSec,
        totalQuestions = totalQuestions,
        attempted = attempted,
        correct = correct,
        incorrect = incorrect,
        score = score,
        maxScore = maxScore,
        accuracy = accuracy,
        percentile = percentile,
        rank = rankEst,
        sectionResultsJson = sectionResults ?: "[]",
        questionAttemptsJson = questionAttemptsJson,
        synced = true,
    )
