package com.testdone.app.data.repository

import android.content.Context
import com.testdone.app.data.content.ContentParser
import com.testdone.app.data.local.dao.ContentDao
import com.testdone.app.data.local.entity.SubjectEntity
import com.testdone.app.data.local.entity.TestEntity
import com.testdone.app.domain.model.Difficulty
import com.testdone.app.domain.model.Exam
import com.testdone.app.domain.model.ExamCategory
import com.testdone.app.domain.model.Plan
import com.testdone.app.domain.model.Question
import com.testdone.app.domain.model.SubjectMeta
import com.testdone.app.domain.model.TestKind
import com.testdone.app.domain.model.TestMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Content access layer: exam metadata (bundled index asset) + live question/
 * test data from Room (seeded from assets, OTA-updatable).
 */
class ContentRepository(
    private val context: Context,
    private val contentDao: ContentDao,
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams

    private val _categories = MutableStateFlow<List<ExamCategory>>(emptyList())
    val categories: StateFlow<List<ExamCategory>> = _categories

    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans

    private val _contentReady = MutableStateFlow(false)

    /** True once at least one exam pack exists in Room (arrived via OTA download). */
    val contentReady: StateFlow<Boolean> = _contentReady

    var totalQuestions: Int = 0; private set
    var totalMockTests: Int = 0; private set
    var totalPyqSets: Int = 0; private set

    private var indexLoaded = false

    /** Parse the bundled metadata index (fast, one-time per process). */
    suspend fun ensureIndexLoaded() = mutex.withLock {
        if (indexLoaded) return@withLock
        withContext(Dispatchers.IO) {
            val text = context.assets.open("content/exams_index.json").bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(text).jsonObject

            val versions = contentDao.allVersions().associateBy { it.examId }

            _exams.value = (root["exams"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { el ->
                val o = el.jsonObject
                val id = o["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val v = versions[id]
                Exam(
                    id = id,
                    name = o["name"]?.jsonPrimitive?.contentOrNull ?: id,
                    shortName = o["shortName"]?.jsonPrimitive?.contentOrNull ?: id,
                    category = o["category"]?.jsonPrimitive?.contentOrNull ?: "",
                    icon = o["icon"]?.jsonPrimitive?.contentOrNull ?: "📚",
                    description = o["description"]?.jsonPrimitive?.contentOrNull ?: "",
                    colorHex = o["color"]?.jsonPrimitive?.contentOrNull ?: "#6366F1",
                    gradientCss = o["gradient"]?.jsonPrimitive?.contentOrNull ?: "",
                    difficulty = o["difficulty"]?.jsonPrimitive?.contentOrNull ?: "Moderate",
                    candidateBase = o["candidateBase"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 100_000L,
                    subjects = (o["subjects"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { sEl ->
                        val s = sEl.jsonObject
                        val sid = s["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        SubjectMeta(
                            id = sid,
                            name = s["name"]?.jsonPrimitive?.contentOrNull ?: sid,
                            icon = s["icon"]?.jsonPrimitive?.contentOrNull ?: "📘",
                            questionCount = s["questionCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                            topics = (s["topics"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                        )
                    } ?: emptyList(),
                    questionCount = v?.questionCount ?: o["questionCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                    mockCount = v?.mockCount ?: o["mockCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                    pyqCount = v?.pyqCount ?: o["pyqCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                    pyqYears = (o["pyqYears"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull?.toIntOrNull() } ?: emptyList(),
                    durationMin = o["durationMin"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 180,
                )
            } ?: emptyList()

            _categories.value = (root["categories"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { el ->
                val o = el.jsonObject
                val id = o["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                ExamCategory(
                    id = id,
                    label = o["label"]?.jsonPrimitive?.contentOrNull ?: id,
                    icon = o["icon"]?.jsonPrimitive?.contentOrNull ?: "📚",
                )
            } ?: emptyList()

            _plans.value = (root["plans"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { el ->
                val o = el.jsonObject
                val id = o["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                Plan(
                    id = id,
                    name = o["name"]?.jsonPrimitive?.contentOrNull ?: id,
                    shortName = o["shortName"]?.jsonPrimitive?.contentOrNull ?: id,
                    price = o["price"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
                    period = o["period"]?.jsonPrimitive?.contentOrNull ?: "month",
                    colorHex = o["color"]?.jsonPrimitive?.contentOrNull ?: "#6366F1",
                    gradientCss = o["gradient"]?.jsonPrimitive?.contentOrNull ?: "",
                    tagline = o["tagline"]?.jsonPrimitive?.contentOrNull ?: "",
                    features = (o["features"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                    popular = o["popular"]?.jsonPrimitive?.contentOrNull == "true",
                )
            } ?: emptyList()

            totalQuestions = root["totalQuestions"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
            totalMockTests = root["totalMockTests"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
            totalPyqSets = root["totalPyqSets"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
            indexLoaded = true
            // v2.3.5+: packs are online-only — did the OTA sync already deliver any?
            _contentReady.value = contentDao.allVersions().isNotEmpty()
        }
    }

    /** Refresh live counts after an OTA sync (questionCount etc. changed). */
    suspend fun reloadLiveCounts() {
        mutex.withLock {
            if (!indexLoaded) return@withLock
            val versions = contentDao.allVersions().associateBy { it.examId }
            _contentReady.value = versions.isNotEmpty()
            _exams.value = _exams.value.map { e ->
                val v = versions[e.id] ?: return@map e
                e.copy(questionCount = v.questionCount, mockCount = v.mockCount, pyqCount = v.pyqCount)
            }
        }
    }

    fun examById(id: String): Exam? = _exams.value.firstOrNull { it.id == id }

    fun searchExams(query: String): List<Exam> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return _exams.value
        return _exams.value.filter {
            it.name.lowercase().contains(q) || it.shortName.lowercase().contains(q) ||
                it.category.lowercase().contains(q) || it.subjects.any { s -> s.name.lowercase().contains(q) }
        }
    }

    // ── Room-backed data ─────────────────────────────────────────────────────

    suspend fun testsForExam(examId: String, kind: TestKind? = null): List<TestMeta> =
        withContext(Dispatchers.IO) {
            val rows = if (kind != null) contentDao.testsForExam(examId, kind.wire) else contentDao.allTestsForExam(examId)
            rows.map { it.toDomain() }
        }

    suspend fun testById(testId: String): TestMeta? = withContext(Dispatchers.IO) {
        contentDao.testById(testId)?.toDomain()
    }

    suspend fun questionsByIds(ids: List<String>): List<Question> = withContext(Dispatchers.IO) {
        val byId = contentDao.questionsByIds(ids).associateBy { it.id }
        ids.mapNotNull { qid ->
            byId[qid]?.let { it.toDomain() }
        }
    }

    data class QBankPage(val questions: List<Question>, val total: Int, val topics: List<QBankTopic>)

    data class QBankTopic(val name: String, val count: Int)

    suspend fun qbankQuestions(
        examId: String,
        subjectId: String?,
        topic: String?,
        limit: Int,
        offset: Int,
    ): QBankPage = withContext(Dispatchers.IO) {
        // subjectId == null → the WHOLE exam's question bank (all questions
        // default); a subject narrows it; topic narrows further.
        val rows = when {
            subjectId != null && topic != null -> contentDao.questionsForTopic(examId, subjectId, topic, limit, offset)
            subjectId != null -> contentDao.questionsForSubject(examId, subjectId, limit, offset)
            else -> contentDao.questionsForExam(examId, limit, offset)
        }
        val total = when {
            subjectId != null && topic != null -> contentDao.countForTopic(examId, subjectId, topic)
            subjectId != null -> contentDao.countForSubject(examId, subjectId)
            else -> contentDao.countForExam(examId)
        }
        val subjectRow: SubjectEntity? = null
        QBankPage(
            questions = rows.map { it.toDomain() },
            total = total,
            topics = emptyList(), // topics loaded separately via subjectTopics()
        )
    }

    suspend fun subjectTopics(examId: String, subjectId: String): List<QBankTopic> =
        withContext(Dispatchers.IO) {
            val subs = contentDao.subjectsForExam(examId)
            val sub = subs.firstOrNull { it.id == subjectId } ?: return@withContext emptyList()
            val names = ContentParser.parseStringList(sub.topicsJson)
            names.map { t -> QBankTopic(t, contentDao.countForTopic(examId, subjectId, t)) }
        }

    suspend fun subjectsForExam(examId: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        contentDao.subjectsForExam(examId).map { it.id to it.name }
    }

    suspend fun attemptedTestIds(testIds: List<String>, isAttempted: suspend (String) -> Boolean): Set<String> {
        return testIds.filter { isAttempted(it) }.toSet()
    }
}

// ── entity → domain mappers ─────────────────────────────────────────────────

fun com.testdone.app.data.local.entity.TestEntity.toDomain(): TestMeta = TestMeta(
    id = id,
    examId = examId,
    kind = if (kind == "pyq") TestKind.PYQ else TestKind.MOCK,
    title = title,
    desc = desc,
    durationMin = durationMin,
    totalQuestions = totalQuestions,
    maxMarks = maxMarks,
    neg = neg,
    attempt = attempt,
    year = year,
    questionIds = ContentParser.parseStringList(questionIdsJson),
)

fun com.testdone.app.data.local.entity.QuestionEntity.toDomain(): Question = Question(
    id = id,
    examId = examId,
    subjectId = subjectId,
    subjectName = subjectName,
    topic = topic,
    text = text,
    options = ContentParser.parseStringList(optionsJson),
    correct = correct,
    solution = solution,
    difficulty = Difficulty.entries.getOrElse(difficulty) { Difficulty.MODERATE },
    marks = marks,
    isPyq = isPyq,
    year = year,
)
