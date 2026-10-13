package org.centrexcursionistalcoi.app.security.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestCryptoHelpers {
    @Test
    fun test_sha256_matchesTheKnownDigest() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Hashing.sha256("abc").toHex(),
        )
        assertContentEquals(Hashing.sha256("abc"), Hashing.sha256("abc".toByteArray()))
        assertContentEquals(Hashing.sha256("abc"), Hashing.newSha256().apply { update("abc".toByteArray()) }.digest())
    }

    @Test
    fun test_constantTimeEquals() {
        assertTrue(Hashing.constantTimeEquals("secret", "secret"))
        assertFalse(Hashing.constantTimeEquals("secret", "Secret"))
        assertFalse(Hashing.constantTimeEquals("secret", "secrets"))
    }

    @Test
    fun test_base64Url_hasNoPadding_andRoundTrips() {
        val bytes = byteArrayOf(-5, -1, -2, 0, 1)
        val encoded = bytes.toBase64Url()
        assertEquals("-__-AAE", encoded)
        assertContentEquals(bytes, encoded.fromBase64Url())
    }

    @Test
    fun test_toHex_padsEachByte() {
        assertEquals("000fff", byteArrayOf(0, 15, -1).toHex())
    }

    @Test
    fun test_randomValues() {
        assertEquals(24, RandomValues.bytes(24).size)
        assertEquals(43, RandomValues.token().length)
        assertNotEquals(RandomValues.token(), RandomValues.token())

        val digits = RandomValues.digits(6)
        assertEquals(6, digits.length)
        assertTrue(digits.all(Char::isDigit))

        assertTrue(RandomValues.string(50, "AB").all { it == 'A' || it == 'B' })
    }
}
