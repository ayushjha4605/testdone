package com.testdone.app

import com.testdone.app.data.content.ContentParser
import com.testdone.app.domain.logic.computeStreak
import com.testdone.app.domain.logic.estimateAir
import com.testdone.app.domain.logic.estimatePercentile
import com.testdone.app.domain.logic.formatDuration
import com.testdone.app.domain.model.Difficulty
import com.testdone.app.domain.model.QuestionStatus
import com.testdone.app.domain.model.TestKind
import com.testdone.app.data.repository.toDomain
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Data-pipeline tests against the REAL bundled exam packs — the exact same
 * files that ship inside the APK (copied into test resources).
 */
class ContentParserTest {

    private fun pack(name: String): String =
        File("src/test/resources/$name").readText()

    @Test
    fun `parses real jee-main pack with correct counts`() {
        val text = pack("jee-main.json")
        val parsed = ContentParser.parse("jee-main", text)
        assertNotNull(parsed)
        val p = parsed!!

        assertEquals(3, p.subjects.size)
        assertEquals(933, p.questions.size)
        assertEquals(15, p.tests.size)
        assertEquals(1, p.version)

        // first question tuple → entity mapping
        val q0 = p.questions[0]
        assertEquals("jee-main_q0", q0.id)
        assertEquals("physics", q0.subjectId)
        assertEquals("Physics", q0.subjectName)
        assertEquals("SHM", q0.topic)
        assertEquals(4, ContentParser.parseStringList(q0.optionsJson).size)
        assertEquals(0, q0.correct)
        assertEquals(Difficulty.MODERATE, q0.toDomain().difficulty)
        assertEquals(4.0, q0.marks, 0.001)
        assertTrue(q0.isPyq)

        // first test
        val t0 = p.tests[0]
        assertEquals("jee_mock_01", t0.id)
        assertEquals(90, ContentParser.parseStringList(t0.questionIdsJson).size)
        assertEquals(180, t0.durationMin)
        assertEquals(300.0, t0.maxMarks, 0.001)
        assertEquals(1.0, t0.neg, 0.001)
        assertEquals(75, t0.attempt)

        // version entity counts
        val v = ContentParser.versionEntity("jee-main", p)
        assertEquals(933, v.questionCount)
        val mocks = p.tests.count { it.kind == TestKind.MOCK.wire }
        assertEquals(15 - p.tests.count { it.kind == "pyq" }, mocks)
    }

    @Test
    fun `parses neet pack`() {
        val parsed = ContentParser.parse("neet", pack("neet.json"))
        assertNotNull(parsed)
        assertTrue(parsed!!.questions.isNotEmpty())
        assertTrue(parsed.subjects.size >= 2)
        assertTrue(parsed.tests.isNotEmpty())
    }

    @Test
    fun `rejects malformed packs`() {
        assertNull(ContentParser.parse("x", "not json"))
        assertNull(ContentParser.parse("x", """{"subjects":[],"questions":[],"tests":[]}"""))
        assertNull(ContentParser.parse("x", """{"subjects":[{"id":"a","n":"A","t":[]}],"questions":[],"tests":[]}"""))
    }

    @Test
    fun `question status transitions mirror the web app`() {
        // answered → marked keeps the answer
        var s = QuestionStatus.ANSWERED
        assertTrue(s.isAnswered && !s.isMarked)
        s = QuestionStatus.MARKED_ANSWERED
        assertTrue(s.isAnswered && s.isMarked)
    }

    @Test
    fun `scoring math matches web implementation`() {
        assertEquals(0.0, estimatePercentile(Double.NaN), 0.001)
        assertEquals(87.5, estimatePercentile(87.5), 0.001)
        assertEquals(99.9, estimatePercentile(150.0), 0.001) // clamped like the web app
        assertEquals(1_000_000L, estimateAir(2_000_000, 50.0))
        assertEquals(1L, estimateAir(100, 100.0))
        assertEquals("00:59", formatDuration(59))
        assertEquals("01:00", formatDuration(60))
        assertEquals("1:01:00", formatDuration(3660))
    }

    @Test
    fun `streak logic matches web implementation`() {
        val now = System.currentTimeMillis()
        // today + yesterday + 2 days ago = 3 day streak
        assertEquals(3, computeStreak(listOf(now, now - 86_400_000L, now - 2 * 86_400_000L)))
        // nothing recent → 0
        assertEquals(0, computeStreak(listOf(now - 10 * 86_400_000L)))
        assertEquals(0, computeStreak(emptyList()))
    }
}
