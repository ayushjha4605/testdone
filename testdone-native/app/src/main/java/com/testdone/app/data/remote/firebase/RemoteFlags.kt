package com.testdone.app.data.remote.firebase

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.testdone.app.data.repository.PhoneLoginMode
import kotlinx.coroutines.tasks.await

/**
 * v2.3.8 — remote feature flags (Firebase Remote Config, free on Spark).
 *
 * Currently one flag:
 *  • "phone_login_mode" → "password" (default) | "sms"
 *
 * PASSWORD keeps the app 100% free (number+password login, zero SMS).
 * Switching to "sms" in the console (Remote Config → publish) turns the
 * classic Firebase Phone-Auth OTP flow back on — WITHOUT rebuilding the APK.
 * Values reach devices on the next app launch; the fetch is best-effort and
 * never blocks or crashes (fallback = PASSWORD).
 */
object RemoteFlags {

    const val KEY_PHONE_LOGIN_MODE = "phone_login_mode"
    const val DEFAULT_PHONE_LOGIN_MODE = "password"

    // ── v2.3.16 force update ─────────────────────────────────────────────
    // Console keys (Remote Config → publish; devices pick it up next launch):
    //   force_update_min_version : "2.4.0"   ("" or lower/equal = off)
    //   force_update_message     : optional custom line shown on the gate
    //   force_update_apk_url     : optional direct-APK fallback link
    const val KEY_FORCE_MIN_VERSION = "force_update_min_version"
    const val KEY_FORCE_MESSAGE = "force_update_message"
    const val KEY_FORCE_APK_URL = "force_update_apk_url"

    /** Non-null only when the installed build is BELOW the required minimum. */
    data class ForceUpdateInfo(
        val minVersion: String,
        val message: String?,
        val apkUrl: String?,
    )

    /**
     * Resolves the phone-login mode. Best-effort: when the backend isn't
     * configured, offline, or quota-limited, the last activated (cached)
     * value — or the local default — is used. NEVER throws.
     */
    suspend fun fetchPhoneLoginMode(context: Context): PhoneLoginMode {
        if (!FirebaseBackend.isConfigured(context)) return PhoneLoginMode.PASSWORD
        return runCatching {
            val rc = FirebaseRemoteConfig.getInstance()
            // best-effort refresh; failure keeps the previously activated value
            runCatching {
                rc.setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(0) // small fleet — flip fast
                        .build()
                ).await()
                rc.setDefaultsAsync(mapOf(KEY_PHONE_LOGIN_MODE to DEFAULT_PHONE_LOGIN_MODE)).await()
                rc.fetchAndActivate().await()
            }
            PhoneLoginMode.fromRemoteValue(rc.getString(KEY_PHONE_LOGIN_MODE))
        }.getOrDefault(PhoneLoginMode.PASSWORD)
    }

    /**
     * v2.3.16 — resolves the force-update gate. Returns null when Remote
     * Config has no minimum set, or the installed version already satisfies
     * it. Best-effort like everything here: NEVER throws, offline falls back
     * to the cached/default (off) state.
     */
    suspend fun fetchForceUpdate(context: Context, currentVersion: String): ForceUpdateInfo? {
        if (!FirebaseBackend.isConfigured(context)) return null
        return runCatching {
            val rc = FirebaseRemoteConfig.getInstance()
            runCatching {
                rc.setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(0)
                        .build()
                ).await()
                rc.setDefaultsAsync(
                    mapOf(
                        KEY_FORCE_MIN_VERSION to "",
                        KEY_FORCE_MESSAGE to "",
                        KEY_FORCE_APK_URL to "",
                    )
                ).await()
                rc.fetchAndActivate().await()
            }
            val min = rc.getString(KEY_FORCE_MIN_VERSION).trim()
            if (min.isEmpty()) return@runCatching null
            if (!com.testdone.app.domain.logic.Versioning.isNewer(min, currentVersion)) return@runCatching null
            ForceUpdateInfo(
                minVersion = min,
                message = rc.getString(KEY_FORCE_MESSAGE).trim().takeIf { it.isNotEmpty() },
                apkUrl = rc.getString(KEY_FORCE_APK_URL).trim().takeIf { it.isNotEmpty() },
            )
        }.getOrNull()
    }
}
