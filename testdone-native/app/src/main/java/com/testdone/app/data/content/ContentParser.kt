package com.testdone.app.data.content

import com.testdone.app.data.local.entity.ContentVersionEntity
import com.testdone.app.data.local.entity.QuestionEntity
import com.testdone.app.data.local.entity.SubjectEntity
import com.testdone.app.data.local.entity.TestEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Parses the OTA/bundled exam content packs — the compact tuple format shared
 * with the web app (see src/lib/data.ts parseExamContent):
 *
 * {
 *   "v": 1, "examId": "jee-main",
 *   "subjects": [{ "id": "physics", "n": "Physics", "t": ["Mechanics", ...] }],
 *   "questions": [[subjectIdx, topicIdx, text, [options], correct, solution, difficulty, marks, isPyq, year?]],
 *   "tests": [{ id, kind, title, desc, dur, total, maxMarks, neg, attempt?, year?, qs: [questionIdx] }]
 * }
 */
object ContentParser {

    private val json = Json { ignoreUnknownKeys = true }
    private val stringList = ListSerializer(String.serializer())

    private fun List<String>.toJsonList(): String = json.encodeToString(stringList, this)
    fun parseStringList(raw: String): List<String> = runCatching { json.decodeFromString(stringList, raw) }.getOrDefault(emptyList())

    data class ExamPack(
        val version: Int,
        val subjects: List<SubjectEntity>,
        val questions: List<QuestionEntity>,
        val tests: List<TestEntity>,
    )

    fun parse(examId: String, text: String): ExamPack? {
        return try {
            parseRoot(examId, json.parseToJsonElement(text).jsonObject)
        } catch (e: Exception) {
            null
        }
    }

    fun parseRoot(examId: String, root: JsonObject): ExamPack? {
        val subjectsEl = root["subjects"] as? JsonArray ?: return null
        val questionsEl = root["questions"] as? JsonArray ?: return null
        val testsEl = root["tests"] as? JsonArray ?: return null
        if (subjectsEl.isEmpty() || questionsEl.isEmpty() || testsEl.isEmpty()) return null

        val version = root["v"]?.jsonPrimitive?.int ?: 1

        data class Sub(val id: String, val name: String, val topics: List<String>)
        val subs = subjectsEl.map { el ->
            val o = el.jsonObject
            Sub(
                id = o["id"]?.jsonPrimitive?.contentOrNull ?: "",
                name = o["n"]?.jsonPrimitive?.contentOrNull ?: "",
                topics = (o["t"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
            )
        }
        val subjects = subs.map { s ->
            SubjectEntity(examId = examId, id = s.id, name = s.name, topicsJson = s.topics.toJsonList())
        }
        val topicsBySub = subs.map { it.topics }

        // questions (compact tuples)
        val questions = questionsEl.mapIndexedNotNull { i, el ->
            val t = (el as? JsonArray) ?: return@mapIndexedNotNull null
            if (t.size < 6) return@mapIndexedNotNull null
            val subIdx = t[0].jsonPrimitive.int
            val topicIdx = t[1].jsonPrimitive.int
            val sub = subs.getOrNull(subIdx) ?: return@mapIndexedNotNull null
            val options = (t[3] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: return@mapIndexedNotNull null
            QuestionEntity(
                id = "${examId}_q$i",
                examId = examId,
                subjectId = sub.id,
                subjectName = sub.name,
                topic = topicsBySub.getOrNull(subIdx)?.getOrNull(topicIdx) ?: "General",
                text = t[2].jsonPrimitive?.contentOrNull ?: "",
                optionsJson = options.toJsonList(),
                correct = t[4].jsonPrimitive.int,
                solution = t[5].jsonPrimitive?.contentOrNull ?: "",
                difficulty = t.getOrNull(6)?.jsonPrimitive?.int ?: 1,
                marks = t.getOrNull(7)?.jsonPrimitive?.double ?: 1.0,
                isPyq = t.getOrNull(8)?.jsonPrimitive?.int == 1,
                year = t.getOrNull(9)?.jsonPrimitive?.int,
            )
        }
        if (questions.isEmpty()) return null

        // tests
        val tests = testsEl.mapNotNull { el ->
            val o = el.jsonObject
            val qs = (o["qs"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.int } ?: return@mapNotNull null
            TestEntity(
                id = o["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                examId = examId,
                kind = o["kind"]?.jsonPrimitive?.contentOrNull ?: "mock",
                title = o["title"]?.jsonPrimitive?.contentOrNull ?: "",
                desc = o["desc"]?.jsonPrimitive?.contentOrNull ?: "",
                durationMin = o["dur"]?.jsonPrimitive?.int ?: 60,
                totalQuestions = o["total"]?.jsonPrimitive?.int ?: qs.size,
                maxMarks = o["maxMarks"]?.jsonPrimitive?.double ?: qs.size.toDouble(),
                neg = o["neg"]?.jsonPrimitive?.double ?: 0.0,
                attempt = o["attempt"]?.jsonPrimitive?.int,
                year = o["year"]?.jsonPrimitive?.int,
                questionIdsJson = qs.map { "${examId}_q$it" }.toJsonList(),
            )
        }
        if (tests.isEmpty()) return null

        return ExamPack(
            version = version,
            subjects = subjects,
            questions = questions,
            tests = tests,
        )
    }

    fun versionEntity(examId: String, pack: ExamPack): ContentVersionEntity = ContentVersionEntity(
        examId = examId,
        version = pack.version,
        questionCount = pack.questions.size,
        mockCount = pack.tests.count { it.kind == "mock" },
        pyqCount = pack.tests.count { it.kind == "pyq" },
    )
}
