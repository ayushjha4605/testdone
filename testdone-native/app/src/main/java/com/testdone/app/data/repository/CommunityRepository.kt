package com.testdone.app.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.testdone.app.data.local.prefs.StoredSession
import com.testdone.app.data.remote.firebase.FirebaseBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * v2.3.14 — community repository (doubts feed + question submissions + admin
 * moderation), recovered byte-faithfully from the v2.3.14 release APK.
 *
 * Firestore layout:
 *   community_doubts/{docId}      — { question, exam, authorId, authorName,
 *                                    createdAt, likes, answers[] }
 *   community_submissions/{docId} — { question, options[], correct, explanation,
 *                                    examId, authorId, authorName, createdAt,
 *                                    status: pending|live|rejected,
 *                                    reviewedAt, reviewedBy }
 *   users/{uid}                   — { ..., suspended: true, lastSuspendedTime }
 *
 * Admin gate: the signed-in account's email (lower-cased) must be one of
 * [ADMIN_EMAILS] — the two owner accounts (Google login + phone-password
 * login). Everything else sees "Sirf project admin ke liye".
 */
class CommunityRepository(
    private val context: Context,
    private val sessionStore: com.testdone.app.data.local.prefs.SessionStore,
) {

    /** Owner accounts — the ONLY logins allowed into the admin console. */
    companion object {
        private const val TAG = "CommunityRepo"

        // v2.3.17 — single owner account (user request: sab jagah testdoneadmin@gmail.com).
        // Purane accounts (ayushjha4605@gmail.com / 9315441351@phone.testdone.app) hata diye.
        // NOTE: Firestore rules mein bhi yahi email hona chahiye (FIXES-v2.3.17.md).
        val ADMIN_EMAILS = setOf(
            "testdoneadmin@gmail.com",
        )

        /** True when [email] belongs to an owner account (case-insensitive). */
        fun isAdmin(email: String?): Boolean {
            if (email == null) return false
            val normalized = email.trim().lowercase()
            return normalized in ADMIN_EMAILS
        }

        /**
         * Maps Firestore/community failures to friendly Hinglish messages.
         * Code + wording recovered exactly from the v2.3.14 APK.
         */
        fun friendly(t: Throwable?): CommunityError {
            if (t == null) return CommunityError("Kuch galat ho gaya — dobara try karo", "unknown")
            val e = t as? Exception
            Log.w(TAG, "firestore community op failed: ${t.message}", t)
            val code = (t as? FirebaseFirestoreException)?.code?.name
            val friendly = when (code) {
                "UNAVAILABLE" -> "Firebase abhi reachable nahi hai — internet check karke dobara try karo"
                "FAILED_PRECONDITION" -> "Firestore index missing hai — Console ke link se index banao (agar toast mein link hai)"
                "DEADLINE_EXCEEDED", "CANCELLED" -> "Request timeout ho gayi — dobara try karo"
                "UNAUTHENTICATED" -> "Pehle login karo — community posting sirf logged-in users ke liye hai"
                "RESOURCE_EXHAUSTED" -> "Aaj ka quota khatam ho gaya — thodi der baad try karo"
                "PERMISSION_DENIED" ->
                    "Firebase security rules is collection ko block kar rahe hain — FIXES-v2.3.10.md mein " +
                        "\"Firestore rules (2 min setup)\" wale section ke rules Firebase Console → Firestore → " +
                        "Rules mein paste karke Publish karo. Tab tak profile name/email se post karo."
                else -> t.message?.take(120)?.takeIf { it.isNotBlank() }
                    ?: "Kuch galat ho gaya — dobara try karo"
            }
            return CommunityError(friendly, code?.let { "other: ${t.message?.take(120)}" } ?: "unknown")
        }
    }

    data class CommunityError(val message: String, val code: String)

    // ── models (field names = Firestore contract, keep exact) ────────────────

    data class Doubt(
        val id: String,
        val question: String,
        val exam: String,
        val authorId: String?,
        val authorName: String,
        val likes: Int,
        val answers: List<String>,
        val createdAt: Long,
        /** v2.3.16 — pending (awaiting admin approval) | live (visible to all) | rejected. */
        val status: String = "pending",
    )

    enum class SubmissionStatus { PENDING, LIVE, REJECTED }

    data class Submission(
        val id: String,
        val authorId: String?,
        val authorName: String,
        val examId: String,
        val question: String,
        val options: List<String>,
        val correct: String,
        val explanation: String,
        val status: String, // "pending" | "live" | "rejected"
        val createdAt: Long,
        val reviewedBy: String? = null,
        val reviewedAt: Long? = null,
    )

    private fun db(): FirebaseFirestore? = FirebaseBackend.firestore(context)

    private fun session(): StoredSession? = sessionStore.session.value

    // ── doubts feed ──────────────────────────────────────────────────────────

    /**
     * v2.3.16 — the public feed shows ONLY admin-approved doubts
     * (status == "live"), newest first.
     *
     * Query is deliberately index-free: a single equality filter fetches the
     * matching docs and we sort by createdAt locally — no composite index
     * needed in the console.
     */
    suspend fun feedDoubts(limit: Long = 200): Result<List<Doubt>> = withContext(Dispatchers.IO) {
        val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
        try {
            val snaps = db.collection("community_doubts")
                .whereEqualTo("status", "live")
                .limit(limit)
                .get().await()
            val doubts = snaps.documents.mapNotNull { doc ->
                val question = doc.getString("question")?.trim()
                if (question.isNullOrEmpty()) return@mapNotNull null
                Doubt(
                    id = doc.id,
                    question = question,
                    exam = doc.getString("exam") ?: "",
                    authorId = doc.getString("authorId"),
                    authorName = doc.getString("authorName") ?: "Aspirant",
                    likes = (doc.get("likes") as? Number)?.toInt() ?: 0,
                    answers = (doc.get("answers") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L,
                    status = doc.getString("status") ?: "pending",
                )
            }.sortedByDescending { it.createdAt }
            Result.success(doubts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Post a doubt to [community_doubts]. Requires a logged-in session.
     * v2.3.16 — posts land as `status = "pending"`; they appear in the public
     * feed only after an admin approves them.
     */
    suspend fun postDoubt(question: String, exam: String): Result<Unit> = withContext(Dispatchers.IO) {
        val s = session() ?: return@withContext Result.failure(IllegalStateException(NOT_LOGGED_IN))
        val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
        try {
            val payload = mapOf(
                "question" to question.trim(),
                "exam" to exam,
                "authorId" to s.userId,
                "authorName" to (s.fullName?.takeIf { it.isNotBlank() } ?: "Aspirant"),
                "createdAt" to System.currentTimeMillis(),
                "likes" to 0,
                "answers" to emptyList<String>(),
                "status" to "pending",
            )
            db.collection("community_doubts").add(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── doubts moderation (admin) ────────────────────────────────────────────

    /** Admin: list doubts by status (pending / live / rejected), newest first. */
    suspend fun listDoubts(status: String, limit: Long = 200): Result<List<Doubt>> =
        withContext(Dispatchers.IO) {
            val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
            try {
                val snaps = db.collection("community_doubts")
                    .whereEqualTo("status", status)
                    .limit(limit)
                    .get().await()
                val doubts = snaps.documents.mapNotNull { doc ->
                    val question = doc.getString("question")?.trim()
                    if (question.isNullOrEmpty()) return@mapNotNull null
                    Doubt(
                        id = doc.id,
                        question = question,
                        exam = doc.getString("exam") ?: "",
                        authorId = doc.getString("authorId"),
                        authorName = doc.getString("authorName") ?: "Aspirant",
                        likes = (doc.get("likes") as? Number)?.toInt() ?: 0,
                        answers = (doc.get("answers") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L,
                        status = doc.getString("status") ?: "pending",
                    )
                }.sortedByDescending { it.createdAt }
                Result.success(doubts)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Admin: approve (status=live → everyone sees it) or reject a doubt.
     */
    suspend fun reviewDoubt(id: String, approve: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val s = session() ?: return@withContext Result.failure(IllegalStateException(NOT_LOGGED_IN))
        val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
        try {
            val reviewer = s.fullName?.takeIf { it.isNotBlank() } ?: "admin"
            db.collection("community_doubts").document(id).update(
                mapOf(
                    "status" to if (approve) "live" else "rejected",
                    "reviewedAt" to System.currentTimeMillis(),
                    "reviewedBy" to reviewer,
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── question submissions ─────────────────────────────────────────────────

    /** Submit a community question for review (status = pending). */
    suspend fun submitQuestion(
        question: String,
        options: List<String>,
        correct: String,
        explanation: String,
        examId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val s = session() ?: return@withContext Result.failure(IllegalStateException(NOT_LOGGED_IN))
        val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
        try {
            val payload = hashMapOf(
                "question" to question.trim(),
                "options" to options.map { it.trim() },
                "correct" to correct,
                "explanation" to explanation.trim(),
                "examId" to examId,
                "authorId" to s.userId,
                "authorName" to (s.fullName?.takeIf { it.isNotBlank() } ?: "Aspirant"),
                "createdAt" to System.currentTimeMillis(),
                "status" to "pending",
            )
            db.collection("community_submissions").add(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** This user's own submissions (for the Submit screen status area). */
    suspend fun mySubmissions(): Result<List<Submission>> = withContext(Dispatchers.IO) {
        val s = session() ?: return@withContext Result.success(emptyList())
        val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
        try {
            // v2.3.16 — index-free: equality filter + local sort (no composite
            // index required in the console).
            val snaps = db.collection("community_submissions")
                .whereEqualTo("authorId", s.userId)
                .limit(20)
                .get().await()
            Result.success(snaps.documents.mapNotNull { toSubmission(it.id, it) }.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin: list submissions by status (pending / live / rejected). */
    suspend fun listSubmissions(status: String, limit: Long = 50): Result<List<Submission>> =
        withContext(Dispatchers.IO) {
            val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
            try {
                // v2.3.16 — index-free: equality filter + local sort (no composite
                // index required in the console).
                val snaps = db.collection("community_submissions")
                    .whereEqualTo("status", status)
                    .limit(limit)
                    .get().await()
                Result.success(snaps.documents.mapNotNull { toSubmission(it.id, it) }.sortedByDescending { it.createdAt })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Admin: approve (status=live) or reject (status=rejected) a submission.
     * Author sees 'Not approved' status; live questions show in Q-Bank.
     */
    suspend fun reviewSubmission(id: String, approve: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val s = session() ?: return@withContext Result.failure(IllegalStateException(NOT_LOGGED_IN))
        val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
        try {
            val reviewer = s.fullName?.takeIf { it.isNotBlank() } ?: "admin"
            db.collection("community_submissions").document(id).update(
                mapOf(
                    "status" to if (approve) "live" else "rejected",
                    "reviewedAt" to System.currentTimeMillis(),
                    "reviewedBy" to reviewer,
                ),
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── moderation: suspend / ban a user ─────────────────────────────────────

    /**
     * Admin: suspend (or un-suspend) the author of a post/submission by writing
     * users/{uid} = { suspended, lastSuspendedTime }.
     */
    suspend fun setUserSuspended(userId: String?, suspend: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (userId.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("Account ban nahi paya — dobara try karo"))
            }
            val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
            try {
                val updates = if (suspend) {
                    mapOf("suspended" to true, "lastSuspendedTime" to System.currentTimeMillis())
                } else {
                    mapOf("suspended" to false)
                }
                db.collection("users").document(userId).update(updates).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun toSubmission(id: String, doc: com.google.firebase.firestore.DocumentSnapshot): Submission? {
        val question = doc.getString("question")?.trim()
        if (question.isNullOrEmpty()) return null
        return Submission(
            id = id,
            authorId = doc.getString("authorId"),
            authorName = doc.getString("authorName") ?: "Aspirant",
            examId = doc.getString("examId") ?: "",
            question = question,
            options = (doc.get("options") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            correct = doc.getString("correct") ?: "",
            explanation = doc.getString("explanation") ?: "",
            status = doc.getString("status") ?: "pending",
            createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L,
            reviewedBy = doc.getString("reviewedBy"),
            reviewedAt = (doc.get("reviewedAt") as? Number)?.toLong(),
        )
    }

    // ── Ultra early-access waitlist (v2.3.17) ─────────────────────────────────
    //
    // Ultra tab ka "Get early access" email ab Firestore mein jaata hai —
    // collection: ultra_waitlist/{uid}. Owner Moderate → Waitlist tab mein
    // HAR user ka email + naam + time dekh sakta hai. Doc id = uid, isliye
    // ek user kai baar submit kare toh overwrite hota hai (no duplicates).

    data class UltraWaitlistEntry(
        val id: String,
        val email: String,
        val userId: String?,
        val userName: String,
        val createdAt: Long,
    )

    /** User joins the Ultra waitlist. Doc id = uid → upsert, no duplicates. */
    suspend fun joinUltraWaitlist(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val db = db()
        val s = session()
        if (db == null || s == null) return@withContext Result.failure(IllegalStateException(NOT_LOGGED_IN))
        try {
            val payload = hashMapOf<String, Any?>(
                "email" to email.trim(),
                "userId" to s.userId,
                "userName" to (s.fullName?.takeIf { it.isNotBlank() } ?: "Aspirant"),
                "createdAt" to System.currentTimeMillis(),
            )
            db.collection("ultra_waitlist").document(s.userId).set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin: list waitlist entries, newest first. */
    suspend fun listUltraWaitlist(limit: Long = 500): Result<List<UltraWaitlistEntry>> =
        withContext(Dispatchers.IO) {
            val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
            try {
                val snaps = db.collection("ultra_waitlist").limit(limit).get().await()
                val entries = snaps.documents.mapNotNull { doc ->
                    val email = doc.getString("email")?.trim()
                    if (email.isNullOrEmpty()) return@mapNotNull null
                    UltraWaitlistEntry(
                        id = doc.id,
                        email = email,
                        userId = doc.getString("userId"),
                        userName = doc.getString("userName") ?: "Aspirant",
                        createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L,
                    )
                }.sortedByDescending { it.createdAt }
                Result.success(entries)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

private const val BACKEND_PENDING =
    "Backend setup pending hai — Firebase google-services.json badalna hai (developer se karo)"
private const val NOT_LOGGED_IN =
    "Pehle login karo — community posting sirf logged-in users ke liye hai"
