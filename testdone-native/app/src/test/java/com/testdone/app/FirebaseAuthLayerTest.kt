package com.testdone.app

import com.testdone.app.data.repository.FirebaseAuthRepository
import com.testdone.app.data.repository.FirebaseAuthRepository.Companion.friendlyAuthError
import com.testdone.app.data.repository.FirebaseAuthRepository.Companion.friendlyCredentialError
import com.testdone.app.data.repository.FirebaseAuthRepository.Companion.friendlyPhonePasswordError
import com.testdone.app.data.repository.PhoneFormat
import com.testdone.app.data.repository.PhoneLoginMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the pure parts of the Firebase auth layer:
 * error-code → friendly Hinglish mapping and Indian phone normalisation.
 */
class FirebaseAuthLayerTest {

    // ── friendlyAuthError ───────────────────────────────────────────────────

    @Test
    fun `wrong credentials map to generic friendly message`() {
        assertEquals("Galat email ya password", friendlyAuthError("ERROR_WRONG_PASSWORD", null))
        assertEquals("Galat email ya password", friendlyAuthError("ERROR_INVALID_CREDENTIAL", null))
        assertEquals("Galat email ya password", friendlyAuthError("INVALID_LOGIN_CREDENTIALS", "raw"))
    }

    @Test
    fun `signup conflicts map to already registered`() {
        // v2.3.14 exact-release wording (recovered from the shipped APK)
        assertEquals(
            "Ye email pehle se account bana hua hai — LOGIN karo usi email + password se. Password bhool gaye? Login page pe \"Forgot password\" dabao.",
            friendlyAuthError("ERROR_EMAIL_ALREADY_IN_USE", null),
        )
    }

    @Test
    fun `otp errors map to otp-specific messages`() {
        assertEquals("OTP galat hai — dobara check karke daalo", friendlyAuthError("ERROR_INVALID_VERIFICATION_CODE", null))
        assertEquals("OTP expire ho gaya — resend karo", friendlyAuthError("ERROR_SESSION_EXPIRED", null))
    }

    @Test
    fun `quota maps to sms limit message`() {
        assertEquals(
            "Aaj ka SMS limit khatam — kal try karo ya email/Google se login karo",
            friendlyAuthError("ERROR_QUOTA_EXCEEDED", null),
        )
    }

    @Test
    fun `billing gate maps to blaze plan guidance`() {
        val friendly = friendlyAuthError(
            "ERROR_INTERNAL_ERROR",
            "An internal error has occurred. [ BILLING_NOT_ENABLED ]",
        )
        assertTrue(friendly.contains("Blaze"))
        assertTrue(friendly.contains("Google ya Email"))
    }

    @Test
    fun `unknown code falls back to raw message`() {
        assertEquals("something odd", friendlyAuthError("ERROR_SOMETHING_ELSE", "something odd"))
    }

    @Test
    fun `unknown code with no message falls back to generic`() {
        assertEquals("Kuch galat ho gaya — dobara try karo", friendlyAuthError(null, null))
        assertEquals("Kuch galat ho gaya — dobara try karo", friendlyAuthError("ERROR_WEIRD", ""))
    }

    // ── friendlyCredentialError ─────────────────────────────────────────────

    @Test
    fun `google developer error points to sha1 setup`() {
        val e = com.google.android.gms.common.api.ApiException(
            com.google.android.gms.common.api.Status(10, "DEVELOPER_ERROR"),
        )
        assertTrue(friendlyCredentialError(e).contains("SHA-1"))
    }

    @Test
    fun `plain exception falls back to its message`() {
        assertEquals("no network", friendlyCredentialError(IllegalStateException("no network")))
    }

    // ── PhoneFormat ─────────────────────────────────────────────────────────

    @Test
    fun `plain 10 digit indian numbers get +91 prefix`() {
        assertEquals("+919876543210", PhoneFormat.toE164("9876543210"))
    }

    @Test
    fun `spaced and prefixed numbers normalise`() {
        assertEquals("+919876543210", PhoneFormat.toE164("+91 98765 43210"))
        assertEquals("+919876543210", PhoneFormat.toE164("919876543210"))
    }

    @Test
    fun `invalid numbers return null`() {
        assertNull(PhoneFormat.toE164("1234567890"))     // doesn't start 6-9
        assertNull(PhoneFormat.toE164("98765"))          // too short
        assertNull(PhoneFormat.toE164("98765432101"))    // 11 digits
        assertNull(PhoneFormat.toE164(""))
    }

    @Test
    fun `pretty formatting for display`() {
        assertEquals("+91 98765 43210", PhoneFormat.pretty("+919876543210"))
    }

    @Test
    fun `setup pending constant is user readable`() {
        assertTrue(FirebaseAuthRepository.SETUP_PENDING.contains("Firebase"))
    }

    // ── v2.3.8: phone+password (free) login ─────────────────────────────────

    @Test
    fun `phone number maps to synthetic firebase email`() {
        assertEquals("919876543210@phone.testdone.app", PhoneFormat.toPhoneEmail("+919876543210"))
        assertEquals("919876543210@phone.testdone.app", PhoneFormat.toPhoneEmail(PhoneFormat.toE164("9876543210")!!))
    }

    @Test
    fun `phone email domain is a fixed constant`() {
        assertEquals("phone.testdone.app", PhoneFormat.PHONE_EMAIL_DOMAIN)
    }

    @Test
    fun `sms value resolves to sms mode`() {
        assertEquals(PhoneLoginMode.SMS, PhoneLoginMode.fromRemoteValue("sms"))
        assertEquals(PhoneLoginMode.SMS, PhoneLoginMode.fromRemoteValue("  SMS "))
    }

    @Test
    fun `password null and garbage values resolve to password mode`() {
        assertEquals(PhoneLoginMode.PASSWORD, PhoneLoginMode.fromRemoteValue("password"))
        assertEquals(PhoneLoginMode.PASSWORD, PhoneLoginMode.fromRemoteValue(null))
        assertEquals(PhoneLoginMode.PASSWORD, PhoneLoginMode.fromRemoteValue(""))
        assertEquals(PhoneLoginMode.PASSWORD, PhoneLoginMode.fromRemoteValue("banana"))
    }

    @Test
    fun `phone password errors speak in numbers not emails`() {
        assertEquals(
            "Ye number pehle se registered hai — Login karo",
            friendlyPhonePasswordError("ERROR_EMAIL_ALREADY_IN_USE", null),
        )
        assertEquals(
            "Password galat hai — dobara try karo",
            friendlyPhonePasswordError("INVALID_LOGIN_CREDENTIALS", "raw"),
        )
        assertEquals(
            "Is number pe koi account nahi mila — Create account karo",
            friendlyPhonePasswordError("ERROR_USER_NOT_FOUND", null),
        )
        assertEquals(
            "Password kam se kam 6 characters ka rakho",
            friendlyPhonePasswordError("ERROR_WEAK_PASSWORD", null),
        )
    }

    @Test
    fun `disabled provider points to email-password console toggle`() {
        assertTrue(friendlyPhonePasswordError("ERROR_OPERATION_NOT_ALLOWED", null).contains("Email/Password"))
    }

    @Test
    fun `unknown phone password errors fall back to shared mapping`() {
        assertEquals("Kuch galat ho gaya — dobara try karo", friendlyPhonePasswordError(null, null))
        assertEquals("something odd", friendlyPhonePasswordError("ERROR_SOMETHING_ELSE", "something odd"))
    }
}
