package com.testdone.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.testdone.app.domain.model.PlanId
import com.testdone.app.domain.model.UserProfile

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

enum class ThemeMode { DARK, LIGHT }

/** User-facing settings — persisted immediately, mirrored in memory for sync reads. */
class SettingsStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val K_THEME = stringPreferencesKey("theme")
    private val K_THEME_MIGRATED_16 = booleanPreferencesKey("theme_migrated_v16")
    private val K_LANGUAGE = stringPreferencesKey("language")
    private val K_VIBRATION = booleanPreferencesKey("vibration")
    private val K_SOUNDS = booleanPreferencesKey("sounds")
    private val K_SEEDED = booleanPreferencesKey("seeded")
    private val K_ONBOARDED = booleanPreferencesKey("onboarded")
    private val K_PROFILE = stringPreferencesKey("profile_json")
    private val K_LAST_SYNC = longPreferencesKey("last_sync_check")

    // v2.3.16 — default LIGHT on app open (user request). A one-time migration
    // (below) also flips installs that inherited the old DARK default, so the
    // app opens light for everyone; an explicit toggle afterwards still sticks.
    val theme: Flow<ThemeMode> = context.settingsStore.data.map { ThemeMode.valueOf(it[K_THEME] ?: "LIGHT") }
    val language: Flow<String> = context.settingsStore.data.map { it[K_LANGUAGE] ?: "en" }
    val vibration: Flow<Boolean> = context.settingsStore.data.map { it[K_VIBRATION] ?: true }
    val sounds: Flow<Boolean> = context.settingsStore.data.map { it[K_SOUNDS] ?: true }

    private val _user = MutableStateFlow(UserProfile())
    val user: StateFlow<UserProfile> = _user
    private val _onboarded = MutableStateFlow(false)
    val onboarded: StateFlow<Boolean> = _onboarded
    private val _seeded = MutableStateFlow(false)
    val seeded: StateFlow<Boolean> = _seeded

    /** Blocking cold-start hydration. */
    fun restoreBlocking() {
        try {
            runBlocking {
                val prefs = context.settingsStore.data.firstOrNull()
                _onboarded.value = prefs?.get(K_ONBOARDED) ?: false
                _seeded.value = prefs?.get(K_SEEDED) ?: false
                prefs?.get(K_PROFILE)?.let { raw ->
                    runCatching { _user.value = json.decodeFromString<UserProfile>(raw) }
                }
                // v2.3.16 one-time migration: installs whose stored theme is the
                // OLD default (DARK, written-by-default era) move to LIGHT so the
                // app opens light. Runs once; later user toggles are respected.
                if (prefs?.get(K_THEME_MIGRATED_16) != true) {
                    val stored = prefs?.get(K_THEME)
                    if (stored == null || stored == "DARK") {
                        context.settingsStore.edit { it[K_THEME] = ThemeMode.LIGHT.name }
                    }
                    context.settingsStore.edit { it[K_THEME_MIGRATED_16] = true }
                }
            }
        } catch (e: Exception) {
            // defaults are fine
        }
    }

    fun setTheme(mode: ThemeMode) = editPrefs { it[K_THEME] = mode.name }
    fun setLanguage(code: String) = editPrefs { it[K_LANGUAGE] = code }
    fun setVibration(on: Boolean) = editPrefs { it[K_VIBRATION] = on }
    fun setSounds(on: Boolean) = editPrefs { it[K_SOUNDS] = on }

    fun setSeeded(done: Boolean = true) {
        _seeded.value = done
        editPrefs { it[K_SEEDED] = done }
    }

    fun updateUser(transform: (UserProfile) -> UserProfile) {
        val next = transform(_user.value)
        _user.value = next
        editPrefs { it[K_PROFILE] = json.encodeToString(next) }
    }

    fun setOnboarded(done: Boolean) {
        _onboarded.value = done
        editPrefs { it[K_ONBOARDED] = done }
    }

    fun markSyncCheck(now: Long) = editPrefs { it[K_LAST_SYNC] = now }

    fun resetUser() {
        _user.value = UserProfile(joinedAt = System.currentTimeMillis())
        _onboarded.value = false
        editPrefs {
            it.remove(K_PROFILE)
            it[K_ONBOARDED] = false
        }
    }

    /**
     * Every DataStore write goes through here — a failed/throwing transform must
     * NEVER crash the app (in-memory state is already updated; disk re-syncs on
     * the next write, and the profile rebuilds from the cloud on next login).
     */
    private fun editPrefs(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch {
            try {
                context.settingsStore.edit(block)
            } catch (e: Exception) {
                android.util.Log.w("SettingsStore", "prefs write failed (non-fatal)", e)
            }
        }
    }

    fun hydrateFromCloud(
        id: String, fullName: String?, email: String?, phone: String?,
        selectedExamId: String?, avatarUrl: String?, planWire: String?, planExpiryIso: String?,
        onboardedCloud: Boolean,
        joinedAtMs: Long? = null,
    ) {
        val plan = when (planWire) {
            "testdone-pass" -> PlanId.PASS
            "testdone-pass-ultra" -> PlanId.ULTRA
            else -> PlanId.FREE
        }
        val expiry = planExpiryIso?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
        val planValid = expiry == null || expiry > System.currentTimeMillis()
        updateUser { cur ->
            cur.copy(
                id = id,
                name = fullName?.takeIf { it.isNotBlank() } ?: cur.name,
                email = email ?: cur.email,
                phone = phone ?: cur.phone,
                selectedExamId = selectedExamId ?: cur.selectedExamId,
                onboarded = onboardedCloud || cur.onboarded || selectedExamId != null,
                avatarUrl = avatarUrl ?: cur.avatarUrl,
                plan = if (planValid) plan else PlanId.FREE,
                planExpiry = expiry,
                // v2.3.16 — real account creation time; never leave 0 (Jan 1970)
                joinedAt = (joinedAtMs?.takeIf { it > 0L }) ?: (cur.joinedAt.takeIf { it > 0L })
                    ?: System.currentTimeMillis(),
            )
        }
        if (_user.value.onboarded) _onboarded.value = true
    }
}
