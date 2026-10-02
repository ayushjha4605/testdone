package com.testdone.app.data.remote.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.testdone.app.R

/**
 * Central Firebase gate + lazy accessors.
 *
 * The repo ships with a PLACEHOLDER google-services.json so the project builds
 * before the real Firebase project exists. Every Firebase touchpoint goes
 * through this gate: when the placeholder is still in place, all backend
 * features degrade gracefully to the local/offline experience and the UI shows
 * a "backend setup pending" hint instead of crashing.
 */
object FirebaseBackend {

    private const val PLACEHOLDER_API_KEY = "REPLACE_ME_API_KEY"

    @Volatile
    private var configured: Boolean? = null

    /** True once the real google-services.json from the Firebase console is in place. */
    fun isConfigured(context: Context): Boolean {
        configured?.let { return it }
        val ok = runCatching {
            val apiKey = context.getString(R.string.google_api_key)
            apiKey.isNotBlank() && apiKey != PLACEHOLDER_API_KEY
        }.getOrDefault(false)
        configured = ok
        return ok
    }

    /** FirebaseAuth handle, or null when the backend is not configured yet. */
    fun auth(context: Context): FirebaseAuth? {
        if (!isConfigured(context)) return null
        return runCatching { FirebaseAuth.getInstance() }.getOrNull()
    }

    /** Firestore handle, or null when the backend is not configured yet. */
    fun firestore(context: Context): FirebaseFirestore? {
        if (!isConfigured(context)) return null
        return runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    /**
     * Public download URL of an exam content pack stored at
     * `exam-content/{examId}.json` in Firebase Storage (bucket must allow
     * public read — see the Firebase setup guide).
     */
    fun contentPackUrl(context: Context, examId: String): String? {
        if (!isConfigured(context)) return null
        val bucket = runCatching {
            FirebaseApp.getInstance().options.storageBucket
        }.getOrNull() ?: return null
        return "https://firebasestorage.googleapis.com/v0/b/$bucket/o/" +
            "exam-content%2F$examId.json?alt=media"
    }
}
