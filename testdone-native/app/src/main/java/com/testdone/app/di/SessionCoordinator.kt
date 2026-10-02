package com.testdone.app.di

import com.testdone.app.domain.model.AppConfig
import com.testdone.app.domain.model.ContentSyncResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App-start coordinator — native port of the web app's initAuth() + syncContent():
 *  1. session restore (already done in AppContainer constructors)
 *  2. ensure cloud profile + hydrate user
 *  3. pull cloud attempt history + merge
 *  4. push offline-finished attempts
 *  5. fetch app config (feature flags)
 *  6. OTA content sync (silent, once per launch)
 */
class SessionCoordinator(private val container: AppContainer) {

    data class Toast(
        val message: String,
        val detail: String? = null,
        val kind: Kind = Kind.INFO,
    ) {
        enum class Kind { INFO, SUCCESS, ERROR }
    }

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig

    private val _toasts = MutableSharedFlow<Toast>(extraBufferCapacity = 8)
    val toasts: SharedFlow<Toast> = _toasts

    private var startupRan = false

    suspend fun runStartupSync() {
        if (startupRan) return
        startupRan = true
        val session = container.sessionStore.session.value

        if (session != null && session.userId.isNotBlank()) {
            try {
                val profile = container.profileRepository.ensureProfile(session.userId, session.email, session.phone)
                if (profile != null) {
                    container.settings.hydrateFromCloud(
                        id = profile.id,
                        fullName = profile.fullName,
                        email = profile.email,
                        phone = profile.phone,
                        selectedExamId = profile.selectedExamId,
                        avatarUrl = profile.avatarUrl,
                        planWire = profile.plan,
                        planExpiryIso = profile.planExpiresAt,
                        onboardedCloud = profile.onboarded,
                        joinedAtMs = profile.joinedAt,
                    )
                }
                container.attemptRepository.pullAndMergeCloud()
                container.attemptRepository.pushUnsynced()
            } catch (e: Exception) {
                // offline start — profile pulls again next launch
            }
        }

        // feature flags + OTA content sync
        syncConfigAndContent()
        _ready.value = true
    }

    /**
     * Fetch feature flags (incl. the OTA pack host override) and run the OTA
     * content sync. Safe to call repeatedly — pack downloads are version-gated
     * and the syncer guards against concurrent runs.
     */
    private suspend fun syncConfigAndContent() {
        // feature flags (+ OTA pack host override — set BEFORE otaSyncer.sync below).
        // config.packBaseUrl is null until content/config exists → fall back to the
        // built-in GitHub host (v2.3.5: assets gone, GitHub is the default source).
        runCatching {
            val config = container.profileRepository.fetchAppConfig()
            _appConfig.value = config
            container.otaSyncer.packBaseUrl =
                config.packBaseUrl ?: com.testdone.app.data.content.OtaSyncer.DEFAULT_PACK_BASE_URL
        }

        // OTA content sync — silent; toast when new content lands
        if (container.connectivity.isOnline()) {
            container.contentRepository.ensureIndexLoaded()
            val result: ContentSyncResult? = runCatching {
                container.otaSyncer.sync(container.contentRepository.exams.value.map { it.id }.toSet())
            }.getOrNull()
            if (result != null && result.updatedExams.isNotEmpty()) {
                container.contentRepository.reloadLiveCounts()
                val names = result.updatedExams.mapNotNull { container.contentRepository.examById(it)?.name }
                _toasts.tryEmit(
                    Toast(
                        message = "New content added",
                        detail = buildString {
                            append(names.joinToString(", "))
                            if (result.addedQuestions > 0) append(" · +${result.addedQuestions} questions")
                            append(" · works offline now")
                        },
                        kind = Toast.Kind.SUCCESS,
                    )
                )
            }
        }
    }

    /** After a successful login (any method): profile + history pull. */
    suspend fun onLogin(session: com.testdone.app.data.local.prefs.StoredSession) {
        try {
            val profile = container.profileRepository.ensureProfile(session.userId, session.email, session.phone)
            if (profile != null) {
                container.settings.hydrateFromCloud(
                    id = profile.id,
                    fullName = profile.fullName ?: session.fullName,
                    email = profile.email,
                    phone = profile.phone ?: session.phone,
                    selectedExamId = profile.selectedExamId,
                    avatarUrl = profile.avatarUrl,
                    planWire = profile.plan,
                    planExpiryIso = profile.planExpiresAt,
                    onboardedCloud = profile.onboarded,
                    joinedAtMs = profile.joinedAt,
                )
            }
            container.attemptRepository.pullAndMergeCloud()
            container.attemptRepository.pushUnsynced()
        } catch (e: Exception) {
            // offline login — fine
        }
        // Fresh login can read Firestore now (rules need auth) — sync content too.
        syncConfigAndContent()
        _ready.value = true
    }

    suspend fun manualContentCheck(): ContentSyncResult? {
        // Re-fetch config first — user may have JUST set pack_base_url in console.
        runCatching {
            val config = container.profileRepository.fetchAppConfig()
            _appConfig.value = config
            container.otaSyncer.packBaseUrl =
                config.packBaseUrl ?: com.testdone.app.data.content.OtaSyncer.DEFAULT_PACK_BASE_URL
        }
        container.contentRepository.ensureIndexLoaded()
        val result = runCatching {
            container.otaSyncer.sync(container.contentRepository.exams.value.map { it.id }.toSet())
        }.getOrNull()
        if (result != null) container.contentRepository.reloadLiveCounts()
        return result
    }

    fun toast(message: String, detail: String? = null, kind: Toast.Kind = Toast.Kind.INFO) {
        _toasts.tryEmit(Toast(message, detail, kind))
    }
}
