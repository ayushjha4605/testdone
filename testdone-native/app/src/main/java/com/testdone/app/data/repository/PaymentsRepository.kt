package com.testdone.app.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.testdone.app.data.local.prefs.StoredSession
import com.testdone.app.data.remote.firebase.FirebaseBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * v2.3.16 — Razorpay payments, ADMIN-GATED.
 *
 * Firestore layout:
 *   app_config/payments  — { enabled: Boolean, razorpayKeyId: String?, updatedAt: Long }
 *   payments/{paymentId} — { userId, email, planId, amount, currency,
 *                            razorpayPaymentId, createdAt, source: "razorpay" }
 *
 * • OFF by default: a missing doc or `enabled != true` keeps the Plan screen
 *   in the free "launch tak sab free" state — exactly what the owner asked for
 *   ("abhi enable nahi, mei enable kar pau kabhi bhi").
 * • The owner enables payments from the IN-APP admin console (Moderate →
 *   Config): flipping the toggle writes this doc — no rebuild, no console visit.
 * • The Razorpay KEY ID is public by design (client-side checkout); the
 *   secret key never leaves the Razorpay dashboard.
 * • Security rules for this doc (see FIXES-v2.3.16.md):
 *       allow read: if true;
 *       allow write: if request.auth.token.email in <owner emails>;
 */
class PaymentsRepository(
    private val context: Context,
    private val sessionStore: com.testdone.app.data.local.prefs.SessionStore,
) {

    data class PaymentsConfig(
        val enabled: Boolean = false,
        val razorpayKeyId: String? = null,
        val updatedAt: Long = 0L,
    ) {
        /** Payments can actually charge only when enabled AND a key is set. */
        val ready: Boolean get() = enabled && !razorpayKeyId.isNullOrBlank()
    }

    private fun db(): FirebaseFirestore? = FirebaseBackend.firestore(context)

    private fun session(): StoredSession? = sessionStore.session.value

    /** Current config. Missing doc / offline / rules-missing → disabled. */
    suspend fun config(): Result<PaymentsConfig> = withContext(Dispatchers.IO) {
        val db = db() ?: return@withContext Result.success(PaymentsConfig())
        try {
            val snap = db.collection("app_config").document("payments").get().await()
            if (!snap.exists()) return@withContext Result.success(PaymentsConfig())
            val enabled = snap.getBoolean("enabled") ?: false
            val key = snap.getString("razorpayKeyId")?.takeIf { it.isNotBlank() }
            val updatedAt = (snap.get("updatedAt") as? Number)?.toLong() ?: 0L
            Result.success(PaymentsConfig(enabled = enabled, razorpayKeyId = key, updatedAt = updatedAt))
        } catch (e: Exception) {
            // offline / PERMISSION_DENIED before rules are added → treat as off
            Result.success(PaymentsConfig())
        }
    }

    /** Admin: enable/disable payments + set the Razorpay Key ID. */
    suspend fun saveConfig(enabled: Boolean, razorpayKeyId: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            val s = session() ?: return@withContext Result.failure(IllegalStateException("Pehle login karo"))
            if (!CommunityRepository.isAdmin(s.email)) {
                return@withContext Result.failure(IllegalStateException("Sirf project admin ke liye"))
            }
            val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
            try {
                db.collection("app_config").document("payments").set(
                    mapOf(
                        "enabled" to enabled,
                        "razorpayKeyId" to razorpayKeyId?.trim()?.takeIf { it.isNotBlank() },
                        "updatedAt" to System.currentTimeMillis(),
                        "updatedBy" to s.email,
                    )
                ).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Record a successful payment (fire-and-forget audit trail; the plan itself
     * is activated via FirestoreProfileRepository.activatePlan).
     */
    suspend fun recordPayment(
        planId: String,
        amountInr: Double,
        razorpayPaymentId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val s = session() ?: return@withContext Result.failure(IllegalStateException("Pehle login karo"))
        val db = db() ?: return@withContext Result.failure(IllegalStateException(BACKEND_PENDING))
        try {
            db.collection("payments").document(razorpayPaymentId).set(
                mapOf(
                    "userId" to s.userId,
                    "email" to s.email,
                    "name" to (s.fullName ?: ""),
                    "planId" to planId,
                    "amount" to amountInr,
                    "currency" to "INR",
                    "razorpayPaymentId" to razorpayPaymentId,
                    "createdAt" to System.currentTimeMillis(),
                    "source" to "razorpay",
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private const val BACKEND_PENDING =
    "Backend setup pending hai — Firebase google-services.json badalna hai (developer se karo)"
