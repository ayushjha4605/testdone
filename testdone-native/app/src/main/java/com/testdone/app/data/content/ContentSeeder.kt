package com.testdone.app.data.content

import android.content.Context
import com.testdone.app.data.local.dao.ContentDao
import com.testdone.app.data.local.db.TestDoneDatabase
import com.testdone.app.data.local.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * First-launch database seeding: streams the 18 bundled exam packs from
 * assets into Room with visible progress (18,600+ questions, a few seconds).
 */
class ContentSeeder(
    private val context: Context,
    private val db: TestDoneDatabase,
    private val settings: SettingsStore,
) {

    sealed interface SeedState {
        data object Idle : SeedState
        data class Seeding(val done: Int, val total: Int, val currentExam: String) : SeedState
        data class Failed(val examId: String) : SeedState
        data object Done : SeedState
    }

    private val _state = MutableStateFlow<SeedState>(SeedState.Idle)
    val state: StateFlow<SeedState> = _state

    private var examIds: List<Pair<String, String>> = emptyList() // id → display name

    suspend fun ensureExamList(examList: List<Pair<String, String>>) {
        examIds = examList
    }

    suspend fun seedIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (settings.seeded.value && db.contentDao().versionCount() >= (examIds.size)) {
            _state.value = SeedState.Done
            return@withContext true
        }
        // v2.3.5+: exam packs are no longer bundled in assets (online-first) —
        // nothing to seed locally; content arrives via the OTA sync after login.
        val bundled = context.assets.list("content/exams")
        if (bundled == null || bundled.isEmpty()) {
            settings.setSeeded(true)
            _state.value = SeedState.Done
            return@withContext true
        }
        seed()
    }

    private suspend fun seed(): Boolean {
        val dao: ContentDao = db.contentDao()
        val ids = examIds.ifEmpty {
            context.assets.list("content/exams")?.map { it.removeSuffix(".json") to it } ?: emptyList()
        }
        _state.value = SeedState.Seeding(0, ids.size, "")
        for ((index, pair) in ids.withIndex()) {
            val (examId, _) = pair
            try {
                val text = context.assets.open("content/exams/$examId.json").bufferedReader().use { it.readText() }
                val pack = ContentParser.parse(examId, text) ?: run {
                    _state.value = SeedState.Failed(examId)
                    return false
                }
                dao.replaceExamContent(
                    examId = examId,
                    subjects = pack.subjects,
                    questions = pack.questions,
                    tests = pack.tests,
                    version = ContentParser.versionEntity(examId, pack),
                )
                _state.value = SeedState.Seeding(index + 1, ids.size, examId)
            } catch (e: Exception) {
                _state.value = SeedState.Failed(examId)
                return false
            }
        }
        settings.setSeeded(true)
        _state.value = SeedState.Done
        return true
    }
}
