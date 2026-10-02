package com.testdone.app.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testdone.app.data.content.ContentSeeder
import com.testdone.app.data.remote.firebase.RemoteFlags
import com.testdone.app.data.repository.AuthResult
import com.testdone.app.data.repository.PhoneAuthEvent
import com.testdone.app.data.repository.PhoneLoginMode
import com.testdone.app.di.AppContainer
import com.testdone.app.di.SessionCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

/**
 * App-level state machine:
 * splash → seeding (first launch) → auth/onboarding gate → main app.
 * Also owns toasts, theme/language application and the login flows
 * (Google one-tap, phone OTP, email).
 */
class AppViewModel(val container: AppContainer) : ViewModel() {

    enum class Phase { SPLASH, SEEDING, AUTH, MAIN }

    /** Phone (OTP) login flow state. */
    data class PhoneAuthState(
        val step: Step = Step.ENTRY,
        val phone: String = "",          // E.164
        val error: String? = null,
        val busy: Boolean = false,
    ) {
        enum class Step { ENTRY, SENDING, OTP, VERIFYING }
    }

    data class GoogleBusy(val launching: Boolean = false)

    /** v2.3.8 — which phone-login mode is live (PASSWORD default, SMS via Remote Config). */
    private val _phoneLoginMode = MutableStateFlow(PhoneLoginMode.PASSWORD)
    val phoneLoginMode: StateFlow<PhoneLoginMode> = _phoneLoginMode

    private val _phase = MutableStateFlow(Phase.SPLASH)
    val phase: StateFlow<Phase> = _phase

    private val _toast = MutableStateFlow<SessionCoordinator.Toast?>(null)
    val toast: StateFlow<SessionCoordinator.Toast?> = _toast

    private val _googleBusy = MutableStateFlow(GoogleBusy())
    val googleBusy: StateFlow<GoogleBusy> = _googleBusy

    private val _phoneState = MutableStateFlow(PhoneAuthState())
    val phoneState: StateFlow<PhoneAuthState> = _phoneState

    /**
     * v2.3.15 — set the moment a login succeeds for an account whose onboarding
     * hasn't finished. AuthFlow watches this to flip to the exam picker
     * INSTANTLY (the old path waited for the cloud profile hydrate, which
     * rides a slow network chain and made login look dead).
     */
    private val _authNeedsOnboarding = MutableStateFlow(false)
    val authNeedsOnboarding: StateFlow<Boolean> = _authNeedsOnboarding

    val theme = container.settings.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.testdone.app.data.local.prefs.ThemeMode.LIGHT)

    /**
     * v2.3.16 — force-update gate. Non-null ONLY when Remote Config's
     * force_update_min_version is above the installed build; TestDoneAppRoot
     * then shows ForceUpdateScreen instead of the app.
     */
    private val _forceUpdate = MutableStateFlow<RemoteFlags.ForceUpdateInfo?>(null)
    val forceUpdate: StateFlow<RemoteFlags.ForceUpdateInfo?> = _forceUpdate
    val language = container.settings.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")
    val user = container.settings.user
    val onboarded = container.settings.onboarded

    private var splashSkipped = false
    private var startupStarted = false

    init {
        // v2.3.8 — resolve the phone-login mode from Remote Config (best-effort,
        // defaults to the free PASSWORD mode; never blocks the splash gate).
        viewModelScope.launch {
            _phoneLoginMode.value = RemoteFlags.fetchPhoneLoginMode(container.appContext)
        }
        // v2.3.16 — force-update check (Remote Config, best-effort; null = off)
        viewModelScope.launch {
            _forceUpdate.value = RemoteFlags.fetchForceUpdate(
                container.appContext,
                com.testdone.app.BuildConfig.VERSION_NAME,
            )
        }
        // Global toast collector
        viewModelScope.launch {
            container.sessionCoordinator.toasts.collect { _toast.value = it }
        }
        // Phone auth events from the repository (callbacks arrive off-thread)
        viewModelScope.launch {
            container.authRepository.phoneEvents.collect { ev ->
                when (ev) {
                    is PhoneAuthEvent.OtpSent -> _phoneState.value = _phoneState.value.copy(
                        step = PhoneAuthState.Step.OTP, busy = false, error = null,
                        phone = ev.phone,
                    )
                    is PhoneAuthEvent.AutoVerified -> handleAuthSuccess(ev.session, isNew = true, "Login successful!")
                    is PhoneAuthEvent.Failed -> _phoneState.value = _phoneState.value.copy(
                        step = if (_phoneState.value.step == PhoneAuthState.Step.VERIFYING) PhoneAuthState.Step.OTP else PhoneAuthState.Step.ENTRY,
                        busy = false, error = ev.friendly,
                    )
                }
            }
        }
    }

    /** Called once the animated splash finishes. */
    fun onSplashFinished() {
        if (splashSkipped) return
        splashSkipped = true
        viewModelScope.launch {
            container.i18n.load()
            container.contentRepository.ensureIndexLoaded()
            container.i18n.setLanguage(container.settings.language.firstOrNull() ?: "en")

            // v2.3.5+: no bundled exam packs — seedIfNeeded is an instant no-op
            // (assets removed, content arrives via the OTA sync after login).
            container.contentSeeder.ensureExamList(
                container.contentRepository.exams.value.map { it.id to it.name }
            )
            container.contentSeeder.seedIfNeeded()

            // startup sync (profile, attempts, flags, OTA) in parallel with gate decision
            startStartupSyncOnce()

            _phase.value = if (container.settings.onboarded.value) Phase.MAIN else Phase.AUTH
        }
    }

    fun retrySeed() {
        viewModelScope.launch {
            container.contentSeeder.ensureExamList(
                container.contentRepository.exams.value.map { it.id to it.name }
            )
            if (container.contentSeeder.seedIfNeeded()) {
                startStartupSyncOnce()
                _phase.value = if (container.settings.onboarded.value || container.sessionStore.isLoggedIn) Phase.MAIN else Phase.AUTH
            }
        }
    }

    fun seedState(): StateFlow<ContentSeeder.SeedState> = container.contentSeeder.state

    private fun startStartupSyncOnce() {
        if (startupStarted) return
        startupStarted = true
        viewModelScope.launch { container.sessionCoordinator.runStartupSync() }
    }

    // ── auth gate helpers ────────────────────────────────────────────────────

    fun onAuthSuccess() {
        viewModelScope.launch {
            _authNeedsOnboarding.value = false
            _phase.value = Phase.MAIN
        }
    }

    /** Logged out → back to auth gate. */
    fun onLoggedOut() {
        container.settings.resetUser()
        _phase.value = Phase.AUTH
        // re-resolve the phone-login flag so console flips show up at the gate
        viewModelScope.launch {
            _phoneLoginMode.value = RemoteFlags.fetchPhoneLoginMode(container.appContext)
        }
    }

    fun logout(activity: Activity? = null, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            container.authRepository.logout(activity)
            container.attemptRepository.clearLocal()
            container.settings.resetUser()
            _phase.value = Phase.AUTH
            onDone()
        }
    }

    fun dismissToast() { _toast.value = null }

    /**
     * v2.3.14 — banner retry: re-run the OTA content sync immediately
     * (the banner's tap handler; safe to call repeatedly).
     */
    fun retryContentSync() {
        viewModelScope.launch {
            val ids = container.contentRepository.exams.value.map { it.id }.toSet()
            container.otaSyncer.sync(ids)
        }
    }

    // ── Google one-tap sign-in ───────────────────────────────────────────────

    fun signInGoogle(activity: Activity) {
        viewModelScope.launch {
            _googleBusy.value = GoogleBusy(launching = true)
            val result = container.authRepository.signInWithGoogle(activity)
            _googleBusy.value = GoogleBusy()
            when (result) {
                is AuthResult.Success -> handleAuthSuccess(result.session, result.isNewUser, "Welcome${result.session.fullName?.let { ", $it" } ?: ""}!")
                is AuthResult.Error -> if (result.friendly.isNotBlank()) {
                    container.sessionCoordinator.toast("Google login fail", result.friendly, SessionCoordinator.Toast.Kind.ERROR)
                }
            }
        }
    }

    // ── Phone (OTP) sign-in ──────────────────────────────────────────────────

    /** Starts the SMS flow; [phone] must already be E.164 (+91…). */
    fun startPhoneLogin(activity: Activity, phone: String) {
        _phoneState.value = PhoneAuthState(step = PhoneAuthState.Step.SENDING, phone = phone)
        container.authRepository.startPhoneVerification(activity, phone)
    }

    fun submitOtp(otp: String) {
        if (otp.length != 6) {
            _phoneState.value = _phoneState.value.copy(error = "6 digit ka OTP daalo")
            return
        }
        _phoneState.value = _phoneState.value.copy(step = PhoneAuthState.Step.VERIFYING, busy = true, error = null)
        viewModelScope.launch {
            val result = container.authRepository.verifyOtp(otp)
            when (result) {
                is AuthResult.Success -> handleAuthSuccess(result.session, result.isNewUser, "Welcome${result.session.fullName?.let { ", $it" } ?: ""}!")
                is AuthResult.Error -> _phoneState.value = _phoneState.value.copy(
                    step = PhoneAuthState.Step.OTP, busy = false, error = result.friendly,
                )
            }
        }
    }

    fun resendOtp(activity: Activity) {
        _phoneState.value = _phoneState.value.copy(step = PhoneAuthState.Step.SENDING, error = null)
        container.authRepository.resendOtp(activity)
    }

    fun resetPhoneFlow() {
        container.authRepository.cancelPhoneFlow()
        _phoneState.value = PhoneAuthState()
    }

    // ── shared login-success path ────────────────────────────────────────────

    /**
     * v2.3.15 — THE login fix. Previously every auth path awaited
     * sessionCoordinator.onLogin() inline before navigating: that call chains
     * profile-ensure + attempt pull/push + config fetch + a FULL OTA pack sync
     * over the network, so on a slow connection the app sat on the login screen
     * with zero feedback — "login kuch nahi karta". Now:
     *   1. toast + phase/onboarding decision happen IMMEDIATELY, and
     *   2. the cloud sync runs in the background and silently hydrates the
     *      profile/attempts when they land.
     */
    fun onLoginSuccess(
        session: com.testdone.app.data.local.prefs.StoredSession,
        isNew: Boolean,
        message: String,
    ) = handleAuthSuccess(session, isNew, message)

    private fun handleAuthSuccess(session: com.testdone.app.data.local.prefs.StoredSession, isNew: Boolean, message: String) {
        container.sessionCoordinator.toast(message, kind = SessionCoordinator.Toast.Kind.SUCCESS)
        _phoneState.value = PhoneAuthState()

        // Instant identity: the session already carries name/email/phone, so
        // Home's greeting and the drawer look right the moment MAIN appears —
        // the cloud hydrate below only refines it.
        if (!session.email.isNullOrBlank() || !session.phone.isNullOrBlank() || !session.fullName.isNullOrBlank()) {
            container.settings.updateUser { u ->
                u.copy(
                    name = session.fullName?.takeIf { it.isNotBlank() } ?: u.name,
                    email = session.email?.takeIf { it.isNotBlank() } ?: u.email,
                    phone = session.phone?.takeIf { it.isNotBlank() } ?: u.phone,
                    // v2.3.16 — never show "Member since Jan 1970": seed the join
                    // date at login; the cloud hydrate replaces it with the real
                    // users/{uid}.createdAt when it lands.
                    joinedAt = u.joinedAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                )
            }
        }

        val returningUser = container.settings.onboarded.value
        if (returningUser) {
            _authNeedsOnboarding.value = false
            _phase.value = Phase.MAIN // instant entry — no network wait
        } else {
            _authNeedsOnboarding.value = true // AuthFlow flips to the exam picker now
        }

        // Background hydrate: profile, attempts, flags + OTA content. Never
        // blocks the UI; failures are swallowed and retried next launch.
        viewModelScope.launch {
            container.sessionCoordinator.onLogin(session)
            // User onboarded on ANOTHER device (cloud says onboarded, local
            // prefs didn't know) → straight into the app.
            if (_authNeedsOnboarding.value && container.settings.onboarded.value) {
                _authNeedsOnboarding.value = false
                _phase.value = Phase.MAIN
            }
        }
    }

    // ── settings shortcuts ───────────────────────────────────────────────────

    fun setLanguage(code: String) {
        container.settings.setLanguage(code)
        container.i18n.setLanguage(code)
    }

    /** Persist profile changes to the cloud (no-op when logged out). */
    fun updateCloudProfile(patch: com.testdone.app.domain.model.ProfilePatch) {
        val session = container.sessionStore.session.value ?: return
        viewModelScope.launch {
            container.profileRepository.updateProfile(session.userId, patch)
        }
    }

    fun toggleTheme() {
        val current = container.settings.theme
        // settings.theme is a Flow — read latest synchronously via stateIn mirror
        viewModelScope.launch {
            val latest = container.settings.theme.firstOrNull()
                ?: com.testdone.app.data.local.prefs.ThemeMode.LIGHT
            container.settings.setTheme(
                if (latest == com.testdone.app.data.local.prefs.ThemeMode.DARK)
                    com.testdone.app.data.local.prefs.ThemeMode.LIGHT
                else
                    com.testdone.app.data.local.prefs.ThemeMode.DARK
            )
        }
    }
}
