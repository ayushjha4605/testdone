package com.testdone.app.data.content

import android.content.Context
import com.testdone.app.data.local.dao.ContentDao
import com.testdone.app.data.remote.firebase.FirebaseBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * v2.3.14 — OTA content sync, multi-CDN.
 *  1. Firestore `content/manifest` = { examId: version } — the source of truth
 *     for what's newest on the server.
 *  2. Newer version → download the pack once through the CDN fallback chain
 *     (jsDelivr → jsDelivr-fastly → GitHub raw → Firebase Storage) and swap
 *     it into Room.
 *  3. Emits [uiState] so the app can show the v2.3.14 content banner
 *     (downloading / needs-internet / failed-retry) while content is on the
 *     way; 18 rescue packs are bundled in assets so nothing blocks offline.
 */
class OtaSyncer(
    private val context: Context,
    private val contentDao: ContentDao,
    private val okHttpClient: OkHttpClient,
    private val manifestProvider: suspend () -> Map<String, Int>,
) {

    /** Banner-driving state for the content download UI (v2.3.14). */
    sealed interface SyncUiState {
        data object Hidden : SyncUiState
        data object Downloading : SyncUiState
        data object NeedsInternet : SyncUiState
        data class Failed(val detail: String) : SyncUiState
    }

    private val _uiState = MutableStateFlow<SyncUiState>(SyncUiState.Hidden)
    val uiState: StateFlow<SyncUiState> = _uiState

    private var inFlight = false

    /**
     * Static-host override for pack downloads (Firestore content/config →
     * pack_base_url). Defaults to the project's own GitHub content repo so the
     * app works with ZERO console config — the Firestore value, when present,
     * wins (lets the host move later without an app update).
     */
    @Volatile
    var packBaseUrl: String? = DEFAULT_PACK_BASE_URL

    companion object {
        /** v2.3.14 multi-CDN chain (fastest first). */
        const val DEFAULT_PACK_BASE_URL =
            "https://cdn.jsdelivr.net/gh/ayushjha4605/testdone-content@main/exam-content"
        val CDN_FALLBACKS = listOf(
            DEFAULT_PACK_BASE_URL,
            "https://fastly.jsdelivr.net/gh/ayushjha4605/testdone-content@main/exam-content",
            "https://raw.githubusercontent.com/ayushjha4605/testdone-content/main/exam-content",
        )
    }

    suspend fun sync(knownExamIds: Set<String>): com.testdone.app.domain.model.ContentSyncResult? =
        withContext(Dispatchers.IO) {
            if (inFlight) return@withContext null
            inFlight = true
            _uiState.value = SyncUiState.Downloading
            try {
                val manifest = runCatching { manifestProvider() }.getOrNull()
                    ?: run { _uiState.value = SyncUiState.Hidden; return@withContext null }
                if (manifest.isEmpty()) { _uiState.value = SyncUiState.Hidden; return@withContext null }

                val localVersions = contentDao.allVersions().associateBy { it.examId }
                val updated = mutableListOf<String>()
                var added = 0

                for ((examId, remoteVersion) in manifest) {
                    if (examId !in knownExamIds) continue           // unknown exam → ignore
                    val local = localVersions[examId]
                    if (local != null && remoteVersion <= local.version) continue

                    // v2.3.14: CDN chain first (jsDelivr → fastly → raw), Storage fallback.
                    val text = downloadPack(examId)
                        ?: FirebaseBackend.contentPackUrl(context, examId)?.let { url ->
                            fetch(url)
                        } ?: continue

                    val pack = ContentParser.parse(examId, text) ?: continue // never install a broken pack

                    contentDao.replaceExamContent(
                        examId = examId,
                        subjects = pack.subjects,
                        questions = pack.questions,
                        tests = pack.tests,
                        // Manifest is the source of truth: store the version we
                        // synced AGAINST, not the pack's self-declared "v" —
                        // otherwise a manifest bump without a pack "v" bump would
                        // re-download the same pack on every sync, forever.
                        version = ContentParser.versionEntity(examId, pack)
                            .let { it.copy(version = maxOf(remoteVersion, it.version)) },
                    )
                    val prevQ = local?.questionCount ?: 0
                    if (pack.questions.size > prevQ) added += pack.questions.size - prevQ
                    updated.add(examId)
                }

                _uiState.value = SyncUiState.Hidden
                if (updated.isEmpty()) null else com.testdone.app.domain.model.ContentSyncResult(updated, added)
            } catch (e: Exception) {
                // network hiccup — bundled content still works
                _uiState.value = if (isOffline(e)) SyncUiState.NeedsInternet
                else SyncUiState.Failed(e.message?.take(80) ?: "network")
                null
            } finally {
                inFlight = false
            }
        }

    /** Banner retry entry — re-runs the sync on tap. */
    fun markNeedsInternet() {
        _uiState.value = SyncUiState.NeedsInternet
    }

    fun markHidden() {
        _uiState.value = SyncUiState.Hidden
    }

    /** v2.3.14 — try each CDN in order; null when every host failed. */
    private fun downloadPack(examId: String): String? {
        val bases = buildList {
            packBaseUrl?.takeIf { it.isNotBlank() && it !in CDN_FALLBACKS }?.let { add(0, it) }
            addAll(CDN_FALLBACKS)
        }
        for (base in bases) {
            val text = fetch(base.trimEnd('/') + "/" + examId + ".json")
            if (text != null) return text
        }
        return null
    }

    private fun fetch(url: String): String? = try {
        okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    } catch (e: Exception) {
        null
    }

    private fun isOffline(e: Exception): Boolean {
        val msg = e.message ?: return false
        return msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("failed to connect", ignoreCase = true) ||
            msg.contains("ECONNREFUSED", ignoreCase = true)
    }
}
