package com.testdone.app.data.repository

import android.app.Activity
import android.content.Context
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.common.api.ApiException
import com.testdone.app.data.local.prefs.SessionStore
import com.testdone.app.data.local.prefs.StoredSession
import com.testdone.app.data.remote.firebase.FirebaseBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Result wrapper for auth operations. */
sealed interface AuthResult {
    data class Success(val session: StoredSession, val isNewUser: Boolean = false) : AuthResult
    data class Error(val message: String, val friendly: String) : AuthResult
}

/** Live state of an in-flight phone (OTP) verification. */
sealed interface PhoneAuthEvent {
    data class OtpSent(val phone: String) : PhoneAuthEvent
    data class AutoVerified(val session: StoredSession) : PhoneAuthEvent
    data class Failed(val friendly: String) : PhoneAuthEvent
}

/** Pure phone-number helpers (unit-tested). */
object PhoneFormat {
    /** Indian-first normalisation → E.164 (+91…). Returns null when not a valid 10-digit Indian mobile. */
    fun toE164(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        val n = when {
            digits.length == 10 && !digits.startsWith("91") -> digits
            digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
            digits.length == 13 && digits.startsWith("091") -> digits.substring(3)
            else -> return null
        }
        if (n.length != 10 || n[0] !in '6'..'9') return null
        return "+91$n"
    }

    /** Pretty display: +91 98765 43210 */
    fun pretty(e164: String): String {
        val d = e164.filter { it.isDigit() }
        return if (d.length == 12) "+91 ${d.substring(2, 7)} ${d.substring(7)}" else e164
    }

    /**
     * v2.3.8 — maps an E.164 number onto the synthetic email used by the free
     * phone+password login (Firebase Email/Password auth on the Spark plan,
     * zero SMS). Example: "+919876543210" → "919876543210@phone.testdone.app".
     */
    const val PHONE_EMAIL_DOMAIN = "phone.testdone.app"

    fun toPhoneEmail(e164: String): String {
        val digits = e164.filter { it.isDigit() } // "+919876543210" → "919876543210"
        return "$digits@$PHONE_EMAIL_DOMAIN"
    }
}

/**
 * v2.3.8 — which phone-login experience the app should offer.
 *  • [PASSWORD] (default) — free Spark-plan login: number + password, zero SMS.
 *  • [SMS]         — Firebase Phone Auth OTP (requires Blaze plan).
 *
 * Flipped remotely via Firebase Remote Config key "phone_login_mode"
 * ("password" | "sms") — no app rebuild needed. Resolution is pure + tested.
 */
enum class PhoneLoginMode {
    PASSWORD, SMS;

    companion object {
        fun fromRemoteValue(value: String?): PhoneLoginMode =
            if (value?.trim()?.equals("sms", ignoreCase = true) == true) SMS else PASSWORD
    }
}

/**
 * Firebase authentication — the single backend for all three login methods:
 *  • Google (Credential Manager one-tap, native bottom sheet)
 *  • Phone number (SMS OTP with auto-verify support)
 *  • Email / password
 *
 * All network operations go through [FirebaseBackend]; when the real
 * google-services.json is not yet in place, every call fails fast with the
 * "backend setup pending" friendly message instead of crashing.
 */
class FirebaseAuthRepository(
    private val context: Context,
    private val sessionStore: SessionStore,
) {

    private val _phoneEvents = kotlinx.coroutines.flow.MutableSharedFlow<PhoneAuthEvent>(extraBufferCapacity = 8)
    val phoneEvents: kotlinx.coroutines.flow.SharedFlow<PhoneAuthEvent> = _phoneEvents

    /** Verification id of the OTP currently in flight (memory-only). */
    @Volatile
    private var pendingVerificationId: String? = null

    @Volatile
    private var pendingPhone: String? = null

    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    val isBackendReady: Boolean get() = FirebaseBackend.isConfigured(context)

    /**
     * Mirrors Firebase auth state into the encrypted SessionStore.
     * Called once from AppContainer init; the listener keeps the mirror in
     * sync for silent restores, sign-outs from other surfaces, etc.
     */
    fun start() {
        val auth = FirebaseBackend.auth(context)
        if (auth == null) {
            // Backend not configured (placeholder build) — clear stale Supabase-era sessions.
            sessionStore.clear()
            return
        }
        auth.currentUser?.let { sessionStore.save(mirror(it)) }
        auth.addAuthStateListener { fa ->
            val u: FirebaseUser? = fa.currentUser
            if (u != null) sessionStore.save(mirror(u)) else sessionStore.clear()
        }
    }

    // ── Google (Credential Manager) ─────────────────────────────────────────

    /**
     * Native one-tap Google sign-in. Must be called from the main thread
     * (the account-picker UI launches from the Activity context).
     */
    suspend fun signInWithGoogle(activity: Activity): AuthResult {
        val auth = FirebaseBackend.auth(context)
            ?: return AuthResult.Error("backend_not_configured", SETUP_PENDING)
        val serverClientId = webClientId()
            ?: return AuthResult.Error("google_not_enabled", GOOGLE_SETUP_PENDING)

        // v2.3.17 — Credential Manager kabhi-kabhi bina wajah flake karta hai
        // (NoCredentialException / unknown) jabki device pe Google accounts hain.
        // Pehle 1 auto-retry (600ms baad) — 90% flake isi se theek ho jaate hain.
        // Retry sirf GetCredentialException (non-cancel) pe hota hai; user-cancel,
        // FirebaseAuthException aur network errors pe NAHI (unki apni handling hai).
        var response: androidx.credentials.GetCredentialResponse? = null
        var credentialException: GetCredentialException? = null
        for (attempt in 1..2) {
            try {
                val credentialManager = CredentialManager.create(activity)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                response = credentialManager.getCredential(activity, request)
                break
            } catch (e: GetCredentialCancellationException) {
                return AuthResult.Error("cancelled", "") // user closed the sheet — no error UI, NO retry
            } catch (e: GetCredentialException) {
                credentialException = e
                if (attempt == 1) {
                    // transient flake ho sakta hai — ek baar chup-chaap retry
                    kotlinx.coroutines.delay(600)
                }
            }
        }
        if (response == null) {
            val e = credentialException
                ?: return AuthResult.Error("google_failed", "Google se login ka jawab hi nahi aaya — dobara try karo")
            // v2.3.17 — "no-credential" detection upgrade: pehle sirf e.type pe
            // hyphen-wala "no-credential" check tha, jo real exception type
            // ("NoCredentialException", bina hyphen) se kabhi match nahi karta tha —
            // isliye user ko raw "Credentials not found" message dikhta tha.
            // Ab class-name + type + message teeno check hote hain.
            val noAccounts = e.type?.contains("no-credential", ignoreCase = true) == true ||
                e.javaClass.simpleName.contains("NoCredential", ignoreCase = true) ||
                e.message?.contains("credentials not found", ignoreCase = true) == true ||
                e.message?.contains("no credential", ignoreCase = true) == true
            return AuthResult.Error(
                "credential_${e.type ?: "unknown"}",
                if (noAccounts) {
                    // device pe koi Google account nahi — retry ke baad bhi nahi mila
                    "Is phone pe koi Google account nahi mila — Settings → Accounts → Google se apna account add karo, ya Phone/Email se login karo (free hai)"
                } else {
                    friendlyCredentialError(e)
                },
            )
        }
        val resp = response
        return try {
            val credential = resp.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                if (googleCredential.idToken.isNullOrBlank()) {
                    // v2.3.14 — token came back empty; retry guidance, exact wording
                    return AuthResult.Error("google_empty_token", GOOGLE_TOKEN_MISSING)
                }
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                val result = auth.signInWithCredential(firebaseCredential).await()
                val user = result.user
                    ?: return AuthResult.Error("no_user", "Google login fail — dobara try karo")
                AuthResult.Success(mirror(user), isNewUser = result.additionalUserInfo?.isNewUser == true)
            } else {
                AuthResult.Error("unsupported_credential", "Ye login method support nahi hai")
            }
        } catch (e: ApiException) {
            AuthResult.Error("google_api_${e.statusCode}", friendlyCredentialError(e))
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(e.errorCode ?: "firebase", friendlyAuthError(e.errorCode, e.message))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "google_failed", friendlyCredentialError(e))
        }
    }

    // ── Phone (SMS OTP) ─────────────────────────────────────────────────────

    /**
     * Starts SMS verification for the given E.164 number. Emits
     * [PhoneAuthEvent.OtpSent] when the SMS is dispatched, or
     * [PhoneAuthEvent.AutoVerified] when Google auto-verifies (instant).
     */
    fun startPhoneVerification(activity: Activity, e164: String, resend: Boolean = false) {
        val auth = FirebaseBackend.auth(context)
        if (auth == null) {
            _phoneEvents.tryEmit(PhoneAuthEvent.Failed(SETUP_PENDING))
            return
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164)
            .setTimeout(TimeUnit.SECONDS.toMillis(60L), TimeUnit.MILLISECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // instant/auto verification — sign in directly
                    val result: com.google.firebase.auth.AuthResult? = try {
                        kotlinx.coroutines.runBlocking {
                            auth.signInWithCredential(credential).await()
                        }
                    } catch (e: Exception) {
                        _phoneEvents.tryEmit(PhoneAuthEvent.Failed(friendlyOf(e)))
                        null
                    }
                    result?.user?.let { _phoneEvents.tryEmit(PhoneAuthEvent.AutoVerified(mirror(it))) }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    pendingVerificationId = null
                    _phoneEvents.tryEmit(PhoneAuthEvent.Failed(friendlyOf(e)))
                }

                override fun onCodeSent(
                    verificationId: String,
                    forceResendingToken: PhoneAuthProvider.ForceResendingToken,
                ) {
                    pendingVerificationId = verificationId
                    pendingPhone = e164
                    resendToken = forceResendingToken
                    _phoneEvents.tryEmit(PhoneAuthEvent.OtpSent(e164))
                }
            })
        val token = resendToken
        if (resend && token != null) options.setForceResendingToken(token)
        PhoneAuthProvider.verifyPhoneNumber(options.build())
    }

    /** Verifies the 6-digit OTP typed by the user against the pending verification. */
    suspend fun verifyOtp(otp: String): AuthResult {
        val auth = FirebaseBackend.auth(context)
            ?: return AuthResult.Error("backend_not_configured", SETUP_PENDING)
        val vid = pendingVerificationId
            ?: return AuthResult.Error("no_pending_otp", "OTP session expire ho gaya — dobara bhejo")
        return try {
            val credential = PhoneAuthProvider.getCredential(vid, otp.trim())
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return AuthResult.Error("no_user", "Login fail — dobara try karo")
            pendingVerificationId = null
            AuthResult.Success(mirror(user), isNewUser = result.additionalUserInfo?.isNewUser == true)
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(e.errorCode ?: "otp_failed", friendlyAuthError(e.errorCode, e.message))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "otp_failed", friendlyOf(e))
        }
    }

    fun resendOtp(activity: Activity) {
        val phone = pendingPhone ?: return
        startPhoneVerification(activity, phone, resend = true)
    }

    fun cancelPhoneFlow() {
        pendingVerificationId = null
        pendingPhone = null
        resendToken = null
    }

    val hasPendingOtp: Boolean get() = pendingVerificationId != null

    // ── Phone + password (v2.3.8 — free, Spark-plan SMS-free login) ───────

    /**
     * Login with an Indian mobile number + password (no SMS, no Blaze).
     * The number is mapped onto a synthetic Firebase Email/Password account.
     */
    suspend fun loginWithPhonePassword(e164: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val auth = FirebaseBackend.auth(context)
            ?: return@withContext AuthResult.Error("backend_not_configured", SETUP_PENDING)
        try {
            val email = PhoneFormat.toPhoneEmail(e164)
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return@withContext AuthResult.Error("no_user", "Login fail — dobara try karo")
            AuthResult.Success(mirror(user))
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(e.errorCode ?: "login_failed", friendlyPhonePasswordError(e.errorCode, e.message))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "login_failed", friendlyOf(e))
        }
    }

    /**
     * Sign up with an Indian mobile number + password (no SMS, no Blaze).
     * Display name is set to the pretty number so profiles read nicely.
     */
    suspend fun signUpWithPhonePassword(e164: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val auth = FirebaseBackend.auth(context)
            ?: return@withContext AuthResult.Error("backend_not_configured", SETUP_PENDING)
        try {
            val email = PhoneFormat.toPhoneEmail(e164)
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return@withContext AuthResult.Error("no_user", "Account ban nahi paya — dobara try karo")
            runCatching {
                user.updateProfile(
                    com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(PhoneFormat.pretty(e164)).build()
                ).await()
            }
            AuthResult.Success(mirror(user).copy(phone = e164), isNewUser = true)
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(e.errorCode ?: "signup_failed", friendlyPhonePasswordError(e.errorCode, e.message))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "signup_failed", friendlyOf(e))
        }
    }

    // ── Email / password ────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val auth = FirebaseBackend.auth(context)
            ?: return@withContext AuthResult.Error("backend_not_configured", SETUP_PENDING)
        try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: return@withContext AuthResult.Error("no_user", "Login fail — dobara try karo")
            AuthResult.Success(mirror(user))
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(e.errorCode ?: "login_failed", friendlyAuthError(e.errorCode, e.message))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "login_failed", friendlyOf(e))
        }
    }

    suspend fun signUp(email: String, password: String, fullName: String?): AuthResult = withContext(Dispatchers.IO) {
        val auth = FirebaseBackend.auth(context)
            ?: return@withContext AuthResult.Error("backend_not_configured", SETUP_PENDING)
        try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: return@withContext AuthResult.Error("no_user", "Account ban nahi paya — dobara try karo")
            if (!fullName.isNullOrBlank()) {
                runCatching { user.updateProfile(
                    com.google.firebase.auth.UserProfileChangeRequest.Builder().setDisplayName(fullName.trim()).build()
                ).await() }
            }
            AuthResult.Success(mirror(user), isNewUser = true)
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(e.errorCode ?: "signup_failed", friendlyAuthError(e.errorCode, e.message))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "signup_failed", friendlyOf(e))
        }
    }

    suspend fun changePassword(newPassword: String, currentPassword: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val auth = FirebaseBackend.auth(context)
            ?: return@withContext Result.failure(IllegalStateException(SETUP_PENDING))
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("Pehle login karo"))
        try {
            // Firebase needs a recent login — re-authenticate first when the
            // user typed their current password (email accounts).
            if (!currentPassword.isNullOrBlank() && user.email != null) {
                try {
                    val cred = com.google.firebase.auth.EmailAuthProvider
                        .getCredential(user.email!!, currentPassword)
                    user.reauthenticate(cred).await()
                } catch (re: FirebaseAuthException) {
                    return@withContext Result.failure(
                        IllegalStateException(friendlyAuthError(re.errorCode, re.message))
                    )
                }
            }
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Result.failure(IllegalStateException("Security ke liye fresh login chahiye — logout karke dobara login karo, phir password change karo"))
        } catch (e: FirebaseAuthException) {
            Result.failure(IllegalStateException(friendlyAuthError(e.errorCode, e.message)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendRecovery(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val auth = FirebaseBackend.auth(context)
            ?: return@withContext Result.failure(IllegalStateException(SETUP_PENDING))
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Result.failure(IllegalStateException(friendlyAuthError(e.errorCode, e.message)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(activity: Activity? = null) {
        val auth = FirebaseBackend.auth(context)
        runCatching { auth?.signOut() }
        if (activity != null) {
            // wipe the Credential Manager "authorized accounts" state too, so the
            // Google sheet lists all accounts again on the next login
            runCatching { CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest()) }
        }
        sessionStore.clear()
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /**
     * Web OAuth client id required by Credential Manager's Google one-tap.
     * It appears in google-services.json (oauth_client → client_type 3) as soon
     * as Google sign-in is enabled in the Firebase console. Looked up DYNAMICALLY
     * because early/placeholder configs ship without it — a compile-time
     * R.string.default_web_client_id reference would break those builds.
     */
    private fun webClientId(): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id == 0) return null
        return runCatching { context.getString(id) }.getOrNull()
            ?.takeIf { it.isNotBlank() && !it.contains("placeholder") }
    }

    private fun mirror(user: FirebaseUser): StoredSession = StoredSession(
        accessToken = "",
        refreshToken = "",
        expiresAt = Long.MAX_VALUE,
        userId = user.uid,
        email = user.email,
        fullName = user.displayName,
        avatarUrl = user.photoUrl?.toString(),
        phone = user.phoneNumber,
    )

    private fun friendlyOf(e: Exception): String =
        if (e is FirebaseAuthException) friendlyAuthError(e.errorCode, e.message)
        else friendlyCredentialError(e)

    companion object {
        const val SETUP_PENDING = "Backend setup pending hai — Firebase google-services.json badalna hai (developer se karo)"

        const val GOOGLE_SETUP_PENDING =
            "Google login abhi enable nahi hai — Firebase console mein Google sign-in ON + SHA-1 add karna hai (developer se karo). Tab tak phone/email se login karo"

        /** v2.3.14 — empty Google ID token (exact release wording). */
        const val GOOGLE_TOKEN_MISSING =
            "Google se token nahi mila — dobara try karo (internet on hai na?)"

        /** Maps Firebase auth error codes to friendly Hinglish messages. Pure — unit-tested. */
        fun friendlyAuthError(code: String?, msg: String?): String {
            // Firebase Identity Toolkit billing gate (hit on this project, verified via docs):
            // "An internal error has occurred. [ BILLING_NOT_ENABLED ]" — since Sept 2024
            // phone-auth SMS requires the Blaze plan (billed per SMS; Spark = blocked).
            if (msg?.contains("BILLING", ignoreCase = true) == true) {
                return "Billing enable nahi hai — phone SMS ke liye Firebase ka Blaze plan zaroori hai (Sept 2024 se rule). Console → Usage & billing → Upgrade to Blaze (card lagega). Tab tak Google ya Email se login karo"
            }
            return when (code) {
            "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_EMAIL", "ERROR_WRONG_PASSWORD", "INVALID_LOGIN_CREDENTIALS",
            "ERROR_INVALID_USER_TOKEN", "ERROR_USER_DISABLED" -> "Galat email ya password"
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                // v2.3.14 — email-signup conflict, exact release wording
                "Ye email pehle se account bana hua hai — LOGIN karo usi email + password se. Password bhool gaye? Login page pe \"Forgot password\" dabao."
            "ERROR_CREDENTIAL_ALREADY_IN_USE" -> "Ye email/account pehle se registered hai — dusre login method try karo"
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "Ye email dusre method se bana hai (Google/phone) — wahi use karo"
            "ERROR_USER_NOT_FOUND" -> "Is email pe koi account nahi mila — pehle account banao"
            "ERROR_WEAK_PASSWORD" -> "Password kam se kam 6 characters ka hona chahiye"
            "ERROR_TOO_MANY_REQUESTS" -> "Bahut baar try kiya — thodi der baad koshish karo"
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network problem — internet check karo"
            "ERROR_INVALID_PHONE_NUMBER" -> "Phone number galat hai — check karo"
            "ERROR_INVALID_VERIFICATION_CODE" -> "OTP galat hai — dobara check karke daalo"
            "ERROR_SESSION_EXPIRED" -> "OTP expire ho gaya — resend karo"
            "ERROR_QUOTA_EXCEEDED", "quota-exceeded" -> "Aaj ka SMS limit khatam — kal try karo ya email/Google se login karo"
            "ERROR_REQUIRES_RECENT_LOGIN" -> "Security ke liye fresh login chahiye — dobara login karke try karo"
            "ERROR_OPERATION_NOT_ALLOWED" ->
                // Firebase regional SMS control (verified via REST on this project):
                // "SMS unable to be sent until this region enabled by the app developer"
                // → India must be added under Phone → Countries/regions in the console.
                if (msg?.contains("region", ignoreCase = true) == true) {
                    "India SMS region OFF hai — Firebase Console → Authentication → Sign-in method → Phone → Edit → Countries/regions mein India (+91) add karke Save karo. Tab tak Google/Email se login karo"
                } else {
                    "Ye login method Firebase console mein enabled nahi hai — developer se kaho"
                }
            "ERROR_API_KEY_INVALID", "ERROR_APP_NOT_AUTHORIZED" -> "Firebase setup galat hai — google-services.json / SHA-1 check karo"
            "ERROR_CAPTCHA_CHECK_FAILED" -> "Verification fail — thodi der baad try karo"
            else -> msg?.removePrefix("AuthApiError:")?.trim()?.ifEmpty { null }
                ?: "Kuch galat ho gaya — dobara try karo"
            }
        }

        /**
         * v2.3.8 — error mapping for the free phone+password login (number is
         * the identity, so "email" wording is rewritten to "number"). Pure — unit-tested.
         */
        fun friendlyPhonePasswordError(code: String?, msg: String?): String = when (code) {
            "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
                "Ye number pehle se registered hai — Login karo"
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "INVALID_LOGIN_CREDENTIALS", "ERROR_INVALID_EMAIL" ->
                "Password galat hai — dobara try karo"
            "ERROR_USER_NOT_FOUND" ->
                "Is number pe koi account nahi mila — Create account karo"
            "ERROR_WEAK_PASSWORD" ->
                "Password kam se kam 6 characters ka rakho"
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Ye login method console mein OFF hai — Firebase Console → Authentication → Sign-in method → Email/Password ON karo"
            else -> friendlyAuthError(code, msg)
        }

        /** Credential Manager / Play Services errors (Google button).
         * v2.3.14 — per-code mapping recovered exactly from the release build. */
        fun friendlyCredentialError(e: Exception): String {
            val code = (e as? ApiException)?.statusCode ?: -1
            return when (code) {
                4 -> "Google sign-in pending hai (code 4) — dobara try karo"
                7 -> "Google se connect nahi ho paya (code 7) — internet check karke dobara try karo"
                8 -> "Google ka internal error (code 8) — thodi der baad dobara try karo"
                10 -> "Google login is app ke liye enabled nahi — SHA-1 fingerprint Firebase console mein add karo"
                13 -> "Google login fail (code 13) — internet check karo"
                16 -> "Play Services purana hai — Play Store se Google Play Services update karo"
                17 -> "Google login available nahi — email/phone se login karo"
                12500 -> "Google login fail (code 12500) — Play Services update karke try karo"
                12501 -> "Google ne consent cancel kiya — dobara try karo"
                12502 -> "Google login cancel ho gaya — dobara try karo"
                else -> e.message?.trim()?.ifEmpty { null }
                    ?: "Google se login ka jawab hi nahi aaya — dobara try karo"
            }
        }
    }
}
