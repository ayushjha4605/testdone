package com.testdone.app

import com.testdone.app.domain.logic.Versioning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.3.16 — covers the force-update gate's dotted-version comparison and the
 * payments config readiness logic (both drive user-facing gating).
 */
class VersioningAndPaymentsTest {

    // ── Versioning.parse ───────────────────────────────────────────────────

    @Test
    fun `parses three segments`() {
        assertEquals(intArrayOf(2, 3, 16).toList(), Versioning.parse("2.3.16").toList())
    }

    @Test
    fun `missing segments count as zero`() {
        assertEquals(intArrayOf(2, 4, 0).toList(), Versioning.parse("2.4").toList())
        assertEquals(intArrayOf(3, 0, 0).toList(), Versioning.parse("3").toList())
    }

    @Test
    fun `suffixes and junk never throw`() {
        assertEquals(intArrayOf(2, 3, 16).toList(), Versioning.parse("2.3.16-beta").toList())
        assertEquals(intArrayOf(0, 0, 0).toList(), Versioning.parse(null).toList())
        assertEquals(intArrayOf(0, 0, 0).toList(), Versioning.parse("").toList())
        assertEquals(intArrayOf(2, 0, 9).toList(), Versioning.parse("2.abc.9").toList()) // "abc"→0, "9"→9
    }

    // ── Versioning.isNewer (force-update trigger) ──────────────────────────

    @Test
    fun `higher minor version is newer`() {
        assertTrue(Versioning.isNewer("2.4.0", "2.3.16"))
    }

    @Test
    fun `same version is not newer - no gate`() {
        assertFalse(Versioning.isNewer("2.3.16", "2.3.16"))
    }

    @Test
    fun `lower version is not newer`() {
        assertFalse(Versioning.isNewer("2.3.15", "2.3.16"))
    }

    @Test
    fun `major bump is newer`() {
        assertTrue(Versioning.isNewer("3.0.0", "2.9.9"))
    }

    @Test
    fun `blank candidate disables the gate`() {
        assertFalse(Versioning.isNewer("", "2.3.16"))
        assertFalse(Versioning.isNewer(null, "2.3.16"))
    }

    @Test
    fun `patch-only bump is newer`() {
        assertTrue(Versioning.isNewer("2.3.17", "2.3.16"))
    }

    // ── PaymentsConfig readiness ───────────────────────────────────────────

    @Test
    fun `payments config defaults to disabled`() {
        val cfg = com.testdone.app.data.repository.PaymentsRepository.PaymentsConfig()
        assertFalse(cfg.enabled)
        assertFalse(cfg.ready)
    }

    @Test
    fun `enabled without key is not ready`() {
        val cfg = com.testdone.app.data.repository.PaymentsRepository.PaymentsConfig(
            enabled = true, razorpayKeyId = null,
        )
        assertFalse(cfg.ready)
    }

    @Test
    fun `enabled with blank key is not ready`() {
        val cfg = com.testdone.app.data.repository.PaymentsRepository.PaymentsConfig(
            enabled = true, razorpayKeyId = "   ",
        )
        assertFalse(cfg.ready)
    }

    @Test
    fun `enabled with key is ready`() {
        val cfg = com.testdone.app.data.repository.PaymentsRepository.PaymentsConfig(
            enabled = true, razorpayKeyId = "rzp_live_abc123",
        )
        assertTrue(cfg.ready)
    }

    @Test
    fun `disabled with key is still not ready`() {
        val cfg = com.testdone.app.data.repository.PaymentsRepository.PaymentsConfig(
            enabled = false, razorpayKeyId = "rzp_live_abc123",
        )
        assertFalse(cfg.ready)
    }

    // ── Doubts approval contract ───────────────────────────────────────────

    @Test
    fun `doubt defaults to pending - feed excludes it until approved`() {
        val d = com.testdone.app.data.repository.CommunityRepository.Doubt(
            id = "d1", question = "q", exam = "JEE", authorId = "u1",
            authorName = "A", likes = 0, answers = emptyList(), createdAt = 1L,
        )
        assertEquals("pending", d.status)
    }

    @Test
    fun `admin email is the single owner account`() {
        // v2.3.17 — single owner account (user request: sab jagah testdoneadmin@gmail.com).
        // the admin gate that unlocks Moderate + Config + Waitlist sections
        assertTrue(com.testdone.app.data.repository.CommunityRepository.isAdmin("testdoneadmin@gmail.com"))
        assertTrue(com.testdone.app.data.repository.CommunityRepository.isAdmin("  TestDoneAdmin@Gmail.com ")) // case+trim
        // purane owner accounts ab admin NAHI hain
        assertFalse(com.testdone.app.data.repository.CommunityRepository.isAdmin("ayushjha4605@gmail.com"))
        assertFalse(com.testdone.app.data.repository.CommunityRepository.isAdmin("9315441351@phone.testdone.app"))
        assertFalse(com.testdone.app.data.repository.CommunityRepository.isAdmin("random@gmail.com"))
        assertFalse(com.testdone.app.data.repository.CommunityRepository.isAdmin(null))
    }
}
