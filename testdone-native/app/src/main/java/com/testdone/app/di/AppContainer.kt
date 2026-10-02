package com.testdone.app.di

import android.content.Context
import com.testdone.app.core.ConnectivityObserver
import com.testdone.app.core.I18n
import com.testdone.app.data.content.ContentSeeder
import com.testdone.app.data.content.OtaSyncer
import com.testdone.app.data.local.db.TestDoneDatabase
import com.testdone.app.data.local.prefs.SessionStore
import com.testdone.app.data.local.prefs.SettingsStore
import com.testdone.app.data.repository.AttemptRepository
import com.testdone.app.data.repository.FirebaseAuthRepository
import com.testdone.app.data.repository.ContentRepository
import com.testdone.app.data.repository.FirestoreProfileRepository
import com.testdone.app.data.repository.CommunityRepository
import com.testdone.app.data.repository.PaymentsRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual dependency container — deliberately simple (no annotation processors):
 * one place to see every dependency, testable, and fast to build.
 *
 * Backend = Firebase (Auth: Google / phone OTP / email · Firestore: profiles,
 * attempts, content manifest · Storage: exam content packs).
 */
class AppContainer(context: Context) {

    // v2.3.8: exposed for Remote Config flag fetches (phone login mode).
    val appContext: Context = context.applicationContext

    // ── serialization ────────────────────────────────────────────────────────
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    // ── local ────────────────────────────────────────────────────────────────
    val database: TestDoneDatabase = TestDoneDatabase.build(appContext)
    val settings = SettingsStore(appContext).apply { restoreBlocking() }
    val sessionStore = SessionStore(appContext).apply { restoreBlocking() }

    // ── network (content pack downloads only — auth/data use Firebase SDKs) ──
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // content packs can be a few MB
            .build()
    }

    // ── Firebase-backed repositories ─────────────────────────────────────────
    // authRepository is EAGER (not lazy): its auth-state listener must mirror the
    // Firebase session into SessionStore before the splash gate reads isLoggedIn.
    val authRepository: FirebaseAuthRepository = FirebaseAuthRepository(appContext, sessionStore).apply { start() }
    val profileRepository: FirestoreProfileRepository by lazy { FirestoreProfileRepository(appContext) }

    // v2.3.14 — community: doubts feed, question submissions, admin moderation
    val communityRepository: CommunityRepository by lazy { CommunityRepository(appContext, sessionStore) }

    // v2.3.16 — Razorpay payments (admin-gated via app_config/payments)
    val paymentsRepository: PaymentsRepository by lazy { PaymentsRepository(appContext, sessionStore) }

    /**
     * v2.3.16 — Razorpay checkout results. The SDK delivers them to the
     * Activity's PaymentResultListener callbacks (MainActivity), which re-post
     * them here; PlanScreen collects while a checkout is in flight.
     */
    val paymentResults: MutableSharedFlow<PaymentOutcome> = MutableSharedFlow(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val contentRepository: ContentRepository by lazy { ContentRepository(appContext, database.contentDao()) }
    val attemptRepository: AttemptRepository by lazy {
        AttemptRepository(database.attemptDao(), profileRepository) { sessionStore }
    }

    // ── content pipeline ─────────────────────────────────────────────────────
    val contentSeeder: ContentSeeder by lazy { ContentSeeder(appContext, database, settings) }
    val otaSyncer: OtaSyncer by lazy { OtaSyncer(appContext, database.contentDao(), okHttpClient) { profileRepository.contentManifest() } }

    // ── core services ────────────────────────────────────────────────────────
    val i18n = I18n(appContext)
    val connectivity = ConnectivityObserver(appContext)

    // ── session-level sync coordinator ───────────────────────────────────────
    val sessionCoordinator: SessionCoordinator by lazy {
        SessionCoordinator(this)
    }
}

/** Razorpay checkout result relayed from MainActivity to the UI layer. */
sealed class PaymentOutcome {
    /** [razorpayPaymentId] on success — the UI activates the plan with it. */
    data class Success(val razorpayPaymentId: String) : PaymentOutcome()

    /** [code] + friendly [message] on failure/cancellation. */
    data class Error(val code: Int, val message: String) : PaymentOutcome()
}
