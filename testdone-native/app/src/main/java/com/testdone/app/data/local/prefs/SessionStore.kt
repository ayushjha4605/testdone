package com.testdone.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

/**
 * Encrypted mirror of the Firebase session (uid / email / name / avatar / phone).
 * Firebase itself persists the real auth state; this store only mirrors the
 * identity fields so the UI can read them synchronously.
 */
@Serializable
data class StoredSession(
    // token fields are legacy-shaped (Supabase era) — kept so old blobs still
    // deserialize; Firebase manages real tokens internally.
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = Long.MAX_VALUE,
    val userId: String,
    val email: String? = null,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null,
)

/**
 * Persisted, encrypted session mirror + an in-memory copy for fast access.
 * Written exclusively by [FirebaseAuthRepository]'s auth-state listener.
 */
class SessionStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val keyBlob = stringPreferencesKey("session_blob")

    private val _session = MutableStateFlow<StoredSession?>(null)
    val session: StateFlow<StoredSession?> = _session

    val isLoggedIn: Boolean get() = _session.value != null

    /** Restore from disk on cold start (blocking, called once from Application). */
    fun restoreBlocking() {
        try {
            val blob = runBlocking {
                context.sessionStore.data.firstOrNull()?.get(keyBlob)
            }
            if (blob != null) {
                SessionCrypto.decrypt(blob)?.let { plain ->
                    _session.value = json.decodeFromString<StoredSession>(plain)
                }
            }
        } catch (e: Exception) {
            // corrupted / keystore rotated → fresh login
            _session.value = null
        }
    }

    fun save(session: StoredSession) {
        _session.value = session
        scope.launch {
            try {
                context.sessionStore.edit { prefs ->
                    prefs[keyBlob] = SessionCrypto.encrypt(json.encodeToString(session))
                }
            } catch (e: Exception) {
                // Keystore/IO failure must never crash the app — the in-memory
                // session still works; worst case the user re-logins after restart.
                android.util.Log.w("SessionStore", "session persist failed (non-fatal)", e)
            }
        }
    }

    fun clear() {
        _session.value = null
        scope.launch {
            try {
                context.sessionStore.edit { it.clear() }
            } catch (e: Exception) {
                android.util.Log.w("SessionStore", "session clear failed (non-fatal)", e)
            }
        }
    }
}
