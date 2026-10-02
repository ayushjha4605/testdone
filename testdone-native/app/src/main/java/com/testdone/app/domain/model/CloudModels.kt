package com.testdone.app.domain.model

/**
 * Cloud (Firestore) data contracts. Firestore stores plain camelCase fields —
 * no wire-format aliases needed since this is our own database now.
 */

data class CloudProfile(
    val id: String,
    val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val selectedExamId: String? = null,
    val avatarUrl: String? = null,
    val plan: String = "free",
    val planExpiresAt: String? = null,
    val onboarded: Boolean = false,
    /** v2.3.16 — account creation time (users/{uid}.createdAt) for "Member since". */
    val joinedAt: Long? = null,
)

data class ProfilePatch(
    val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val selectedExamId: String? = null,
    val onboarded: Boolean? = null,
    val avatarUrl: String? = null,
)

data class CloudAttempt(
    val attemptId: String,
    val testId: String,
    val testTitle: String = "",
    val testKind: String = "mock",
    val examId: String,
    val startedAt: Long = 0L,
    val completedAt: Long? = null,
    val durationSec: Int = 0,
    val totalQuestions: Int = 0,
    val attempted: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
    val score: Double = 0.0,
    val maxScore: Double = 0.0,
    val accuracy: Double = 0.0,
    val percentile: Double? = null,
    val rankEst: Long? = null,
    val sectionResults: String? = null, // JSON array text (same schema as local rows)
)
