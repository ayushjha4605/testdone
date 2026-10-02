package com.testdone.app

import com.testdone.app.domain.model.PlanId
import com.testdone.app.domain.model.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the v2.0.0 FATAL crash:
 * "SerializationException: Serializer for class 'p' is not found."
 *
 * Root cause: UserProfile was persisted via the REIFIED Json.encodeToString/
 * decodeFromString extensions in SettingsStore while NOT being @Serializable.
 * Those reified calls compile fine without the annotation (the serializer is
 * looked up reflectively at runtime) and then threw inside a DataStore edit —
 * crashing the whole app right after login/startup profile hydration.
 *
 * These tests call the serializers the exact same reified way SettingsStore
 * does, so a future removal of @Serializable fails the build, not the users.
 */
class UserProfileSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `user profile roundtrips through the same reified calls SettingsStore uses`() {
        val profile = UserProfile(
            id = "uuid-1",
            name = "Aarav",
            email = "aarav@example.com",
            phone = "+91 90000 00000",
            selectedExamId = "jee-main",
            onboarded = true,
            joinedAt = 1725000000000L,
            avatarUrl = "https://example.com/a.png",
            plan = PlanId.PASS,
            planExpiry = 1893456000000L,
        )

        // encodeToString(next) — same as SettingsStore.updateUser
        val encoded: String = json.encodeToString(profile)

        // decodeFromString<UserProfile>(raw) — same as SettingsStore.restoreBlocking
        val decoded: UserProfile = json.decodeFromString(encoded)

        assertEquals(profile, decoded)
        assertEquals(PlanId.PASS, decoded.plan)
        assertEquals("jee-main", decoded.selectedExamId)
        assertTrue(decoded.onboarded)
    }

    @Test
    fun `default user profile roundtrips`() {
        val encoded = json.encodeToString(UserProfile())
        val decoded = json.decodeFromString<UserProfile>(encoded)
        assertEquals(UserProfile(), decoded)
        assertEquals(PlanId.FREE, decoded.plan)
    }

    @Test
    fun `stored json stays readable when new fields get defaults later`() {
        // A profile written by v2.0.1 (no future fields yet) must keep decoding
        // as the model evolves — ignoreUnknownKeys guards forward compatibility.
        val legacy = """{"id":"u1","name":"Diya","email":"d@x.com","phone":"","selectedExamId":"neet","onboarded":true,"joinedAt":123,"avatarUrl":null,"plan":"testdone-pass-ultra","planExpiry":null}"""
        val decoded = json.decodeFromString<UserProfile>(legacy)
        assertEquals("Diya", decoded.name)
        assertEquals(PlanId.ULTRA, decoded.plan)
        assertTrue(decoded.planActive)
    }
}
