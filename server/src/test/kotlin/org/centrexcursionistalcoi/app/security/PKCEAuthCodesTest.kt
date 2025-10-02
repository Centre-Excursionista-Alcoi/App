package org.centrexcursionistalcoi.app.security

import org.centrexcursionistalcoi.app.auth.toAsciiByteArray
import org.kotlincrypto.hash.sha2.SHA256
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.auth.generateCodeVerifier as kotlinGenerateCodeVerifier
import org.centrexcursionistalcoi.app.auth.generateCodeChallenge as kotlinGenerateCodeChallenge

class PKCEAuthCodesTest {
    @Test
    fun test_generateCodeVerifier() {
        val verifier = kotlinGenerateCodeVerifier()

        assertTrue(verifier.length in 43..128)
        assertTrue(verifier.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun test_generateCodeChallenge() {
        val verifier = kotlinGenerateCodeVerifier()
        val challenge = kotlinGenerateCodeChallenge(verifier)
        // SHA-256 + Base64url -> always 43 chars
        assertEquals(43, challenge.length, "Assertion failed. Length (${challenge.length}) is not correct: $challenge")
        assertTrue(challenge.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun test_kotlinCryptoSha() {
        val text = "abc123"
        val javaSha = run {
            val md = MessageDigest.getInstance("SHA-256")
            md.digest(text.toByteArray(StandardCharsets.US_ASCII))
        }
        val kotlinSha = run {
            val digest = SHA256()
            val bytes = text.toAsciiByteArray()
            digest.update(bytes)
            digest.digest()
        }
        assertContentEquals(javaSha, kotlinSha)
    }
}
