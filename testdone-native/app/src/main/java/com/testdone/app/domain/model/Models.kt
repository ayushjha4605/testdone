package com.testdone.app.domain.model

import kotlinx.serialization.Serializable

/** Core domain models — mirror of the web app's data contracts. */

enum class Difficulty(val label: String) { EASY("Easy"), MODERATE("Moderate"), HARD("Hard"), VERY_HARD("Very Hard") }
enum class TestKind(val wire: String) { MOCK("mock"), PYQ("pyq") }

/** Serialized inside [UserProfile] (profile_json in DataStore) — must stay @Serializable.
 *  @SerialName pins the stored JSON to the stable Supabase wire values so renaming
 *  the Kotlin constants never breaks stored profiles. */
@Serializable
enum class PlanId(val wire: String) {
    @kotlinx.serialization.SerialName("free") FREE("free"),
    @kotlinx.serialization.SerialName("testdone-pass") PASS("testdone-pass"),
    @kotlinx.serialization.SerialName("testdone-pass-ultra") ULTRA("testdone-pass-ultra"),
}

enum class QuestionStatus {
    NOT_VISITED, NOT_ANSWERED, ANSWERED, MARKED, MARKED_ANSWERED;

    val isAnswered: Boolean get() = this == ANSWERED || this == MARKED_ANSWERED
    val isMarked: Boolean get() = this == MARKED || this == MARKED_ANSWERED
}

data class SubjectMeta(
    val id: String,
    val name: String,
    val icon: String,
    val questionCount: Int,
    val topics: List<String>,
)

data class Exam(
    val id: String,
    val name: String,
    val shortName: String,
    val category: String,
    val icon: String,
    val description: String,
    val colorHex: String,
    val gradientCss: String,
    val difficulty: String,
    val candidateBase: Long,
    val subjects: List<SubjectMeta>,
    val questionCount: Int,
    val mockCount: Int,
    val pyqCount: Int,
    val pyqYears: List<Int>,
    val durationMin: Int,
)

data class ExamCategory(val id: String, val label: String, val icon: String)

data class Question(
    val id: String,
    val examId: String,
    val subjectId: String,
    val subjectName: String,
    val topic: String,
    val text: String,
    val options: List<String>,
    val correct: Int,
    val solution: String,
    val difficulty: Difficulty,
    val marks: Double,
    val isPyq: Boolean,
    val year: Int?,
)

data class TestMeta(
    val id: String,
    val examId: String,
    val kind: TestKind,
    val title: String,
    val desc: String,
    val durationMin: Int,
    val totalQuestions: Int,
    val maxMarks: Double,
    val neg: Double,
    val attempt: Int?,
    val year: Int?,
    val questionIds: List<String>,
)

data class QuestionAttempt(
    val questionId: String,
    val selectedOption: Int?,
    val status: QuestionStatus,
    val timeSpent: Int = 0,
    val isCorrect: Boolean? = null,
)

data class SectionResult(
    val subjectId: String,
    val subjectName: String,
    val correct: Int,
    val total: Int,
    val timeSpent: Int,
)

data class TestAttempt(
    val attemptId: String,
    val testId: String,
    val testTitle: String,
    val testKind: TestKind,
    val examId: String,
    val startedAt: Long,
    val completedAt: Long?,
    val durationSec: Int,
    val totalQuestions: Int,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val score: Double,
    val maxScore: Double,
    val accuracy: Double,
    val percentile: Double?,
    val rank: Long?,
    val sectionResults: List<SectionResult>,
    val questionAttempts: Map<String, QuestionAttempt> = emptyMap(),
) {
    val scorePct: Int get() = if (maxScore > 0) ((score / maxScore) * 100).toInt() else 0
}

/**
 * Persisted as JSON in DataStore (SettingsStore K_PROFILE) and restored on cold start.
 * MUST stay @Serializable — the reified encodeToString()/decodeFromString() calls
 * compile without it but crash at runtime with "Serializer for class not found".
 */
@Serializable
data class UserProfile(
    val id: String? = null,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val selectedExamId: String? = null,
    val onboarded: Boolean = false,
    val joinedAt: Long = 0L,
    val avatarUrl: String? = null,
    val plan: PlanId = PlanId.FREE,
    val planExpiry: Long? = null,
) {
    val planActive: Boolean get() = plan != PlanId.FREE && (planExpiry == null || planExpiry > System.currentTimeMillis())
}

data class Plan(
    val id: String,
    val name: String,
    val shortName: String,
    val price: Double,
    val period: String,
    val colorHex: String,
    val gradientCss: String,
    val tagline: String,
    val features: List<String>,
    val popular: Boolean = false,
)

data class AppConfig(
    val gateContent: Boolean = false,
    val paymentsLive: Boolean = false,
    /**
     * Base URL for OTA content packs (set via Firestore content/config →
     * pack_base_url). Lets packs be served from ANY free static host
     * (GitHub raw, jsDelivr, …) instead of Firebase Storage, which now
     * requires the Blaze (billing) plan. URL + "/{examId}.json" must 200.
     */
    val packBaseUrl: String? = null,
)

data class ContentSyncResult(
    val updatedExams: List<String> = emptyList(),
    val addedQuestions: Int = 0,
)
