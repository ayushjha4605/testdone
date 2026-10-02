package com.testdone.app.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.testdone.app.data.remote.firebase.FirebaseBackend
import com.testdone.app.domain.model.AppConfig
import com.testdone.app.domain.model.CloudAttempt
import com.testdone.app.domain.model.CloudProfile
import com.testdone.app.domain.model.ProfilePatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Firestore-backed profile, attempt-history, app-config and content-manifest
 * repository (replaces the Supabase PostgREST version).
 *
 * Layout:
 *   users/{uid}                      — profile document
 *   users/{uid}/attempts/{attemptId} — test history
 *   content/manifest                 — { examId: version } map (OTA content)
 *   content/config                   — feature flags
 *
 * Every method degrades to null / empty / false when the backend placeholder
 * is still in place, so the app works fully offline-first.
 */
class FirestoreProfileRepository(
    private val context: Context,
) {

    private fun db(): FirebaseFirestore? = FirebaseBackend.firestore(context)

    // ── profile ──────────────────────────────────────────────────────────────

    /** Fetch profile, creating the doc on first login. */
    suspend fun ensureProfile(userId: String, email: String?, phone: String?): CloudProfile? =
        withContext(Dispatchers.IO) {
            val db = db() ?: return@withContext null
            val ref = db.collection("users").document(userId)
            try {
                val snap = ref.get().await()
                if (snap.exists()) {
                    snap.toObject(FirestoreProfile::class.java)?.toDomain(userId)
                } else {
                    val profile = FirestoreProfile(
                        email = email, phone = phone,
                        fullName = email?.substringBefore("@")?.takeIf { it.isNotBlank() },
                        onboarded = false,
                        createdAt = System.currentTimeMillis(),
                    )
                    ref.set(profile).await()
                    profile.toDomain(userId)
                }
            } catch (e: Exception) {
                null
            }
        }

    suspend fun updateProfile(userId: String, patch: ProfilePatch): Boolean = withContext(Dispatchers.IO) {
        val db = db() ?: return@withContext false
        val updates = buildMap {
            patch.fullName?.let { put("fullName", it) }
            patch.phone?.let { put("phone", it) }
            patch.email?.let { put("email", it) }
            patch.selectedExamId?.let { put("selectedExamId", it) }
            patch.onboarded?.let { put("onboarded", it) }
            patch.avatarUrl?.let { put("avatarUrl", it) }
            put("updatedAt", System.currentTimeMillis())
        }
        try {
            db.collection("users").document(userId).update(updates).await()
            true
        } catch (e: Exception) {
            // update() fails on a missing doc → create it (offline-first sign-in edge)
            runCatching {
                db.collection("users").document(userId).set(
                    FirestoreProfile(
                        email = patch.email, phone = patch.phone, fullName = patch.fullName,
                        selectedExamId = patch.selectedExamId, onboarded = patch.onboarded ?: false,
                        createdAt = System.currentTimeMillis(),
                    )
                ).await()
                true
            }.getOrDefault(false)
        }
    }

    /**
     * v2.3.16 — activate a paid plan after a successful Razorpay payment.
     * Writes users/{uid} = { plan, planExpiresAt (ISO), updatedAt } so the plan
     * follows the account across devices and reinstalls.
     */
    suspend fun activatePlan(userId: String, planWire: String, expiresAtMs: Long): Boolean =
        withContext(Dispatchers.IO) {
            val db = db() ?: return@withContext false
            try {
                db.collection("users").document(userId).update(
                    mapOf(
                        "plan" to planWire,
                        "planExpiresAt" to java.time.Instant.ofEpochMilli(expiresAtMs).toString(),
                        "updatedAt" to System.currentTimeMillis(),
                    )
                ).await()
                true
            } catch (e: Exception) {
                false
            }
        }

    // ── attempt history ──────────────────────────────────────────────────────

    suspend fun pushAttempt(userId: String, attempt: CloudAttempt): Boolean = withContext(Dispatchers.IO) {
        val db = db() ?: return@withContext false
        try {
            db.collection("users").document(userId)
                .collection("attempts").document(attempt.attemptId)
                .set(attempt.toMap()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun pullAttempts(userId: String): List<CloudAttempt> = withContext(Dispatchers.IO) {
        val db = db() ?: return@withContext emptyList()
        try {
            db.collection("users").document(userId)
                .collection("attempts")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(200)
                .get().await()
                .documents.mapNotNull { doc ->
                    doc.data?.let { row ->
                        CloudAttempt(
                            attemptId = doc.id,
                            testId = row["testId"] as? String ?: "",
                            testTitle = row["testTitle"] as? String ?: "",
                            testKind = row["testKind"] as? String ?: "mock",
                            examId = row["examId"] as? String ?: "",
                            startedAt = (row["startedAt"] as? Number)?.toLong() ?: 0L,
                            completedAt = (row["completedAt"] as? Number)?.toLong(),
                            durationSec = (row["durationSec"] as? Number)?.toInt() ?: 0,
                            totalQuestions = (row["totalQuestions"] as? Number)?.toInt() ?: 0,
                            attempted = (row["attempted"] as? Number)?.toInt() ?: 0,
                            correct = (row["correct"] as? Number)?.toInt() ?: 0,
                            incorrect = (row["incorrect"] as? Number)?.toInt() ?: 0,
                            score = (row["score"] as? Number)?.toDouble() ?: 0.0,
                            maxScore = (row["maxScore"] as? Number)?.toDouble() ?: 0.0,
                            accuracy = (row["accuracy"] as? Number)?.toDouble() ?: 0.0,
                            percentile = (row["percentile"] as? Number)?.toDouble(),
                            rankEst = (row["rankEst"] as? Number)?.toLong(),
                            sectionResults = row["sectionResults"] as? String,
                        )
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── app config (feature flags) ───────────────────────────────────────────

    suspend fun fetchAppConfig(): AppConfig = withContext(Dispatchers.IO) {
        val db = db() ?: return@withContext AppConfig()
        try {
            val snap = db.collection("content").document("config").get().await()
            AppConfig(
                gateContent = snap.getBoolean("gate_content") ?: false,
                paymentsLive = snap.getBoolean("payments_live") ?: false,
                packBaseUrl = snap.getString("pack_base_url")?.takeIf { it.isNotBlank() },
            )
        } catch (e: Exception) {
            AppConfig()
        }
    }

    // ── OTA content manifest (examId → version) ──────────────────────────────

    suspend fun contentManifest(): Map<String, Int> = withContext(Dispatchers.IO) {
        val db = db() ?: return@withContext emptyMap()
        try {
            val snap = db.collection("content").document("manifest").get().await()
            snap.data?.mapNotNull { (k, v) ->
                (v as? Number)?.toInt()?.let { k to it }
            }?.toMap() ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ── Firestore document shape ─────────────────────────────────────────────

    @com.google.firebase.firestore.IgnoreExtraProperties
    data class FirestoreProfile(
        val email: String? = null,
        val phone: String? = null,
        val fullName: String? = null,
        val selectedExamId: String? = null,
        val avatarUrl: String? = null,
        val plan: String = "free",
        val planExpiresAt: String? = null,
        val onboarded: Boolean = false,
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L,
    ) {
        fun toDomain(id: String) = CloudProfile(
            id = id, fullName = fullName, phone = phone, email = email,
            selectedExamId = selectedExamId, avatarUrl = avatarUrl,
            plan = plan, planExpiresAt = planExpiresAt, onboarded = onboarded,
            // v2.3.16 — createdAt → joinedAt so "Member since" shows the real date
            joinedAt = createdAt.takeIf { it > 0L },
        )
    }
}

private fun CloudAttempt.toMap(): Map<String, Any?> = mapOf(
    "testId" to testId,
    "testTitle" to testTitle,
    "testKind" to testKind,
    "examId" to examId,
    "startedAt" to startedAt,
    "completedAt" to completedAt,
    "durationSec" to durationSec,
    "totalQuestions" to totalQuestions,
    "attempted" to attempted,
    "correct" to correct,
    "incorrect" to incorrect,
    "score" to score,
    "maxScore" to maxScore,
    "accuracy" to accuracy,
    "percentile" to percentile,
    "rankEst" to rankEst,
    "sectionResults" to sectionResults,
    "updatedAt" to System.currentTimeMillis(),
)
