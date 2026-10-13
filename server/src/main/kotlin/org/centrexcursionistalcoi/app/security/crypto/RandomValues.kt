package org.centrexcursionistalcoi.app.security.crypto

import java.security.SecureRandom

/**
 * Everything random that has to be unpredictable: tokens, secrets, codes. Never use [kotlin.random.Random] or
 * [java.util.Random] for those.
 */
object RandomValues {
    /** The one source of randomness, for what needs a [SecureRandom] itself (key generation, IVs, certificates). */
    val secureRandom = SecureRandom()

    fun bytes(size: Int): ByteArray = ByteArray(size).also(secureRandom::nextBytes)

    /** A random integer from 0 (inclusive) to [bound] (exclusive). */
    fun nextInt(bound: Int): Int = secureRandom.nextInt(bound)

    /** [size] random bytes, as Base64Url: a token or secret that can go in a URL. */
    fun token(size: Int = 32): String = bytes(size).toBase64Url()

    /** [length] random characters of [alphabet]. */
    fun string(length: Int, alphabet: String): String = buildString(length) {
        repeat(length) { append(alphabet[nextInt(alphabet.length)]) }
    }

    /** [length] random digits, zeros included: a numeric code. */
    fun digits(length: Int): String = string(length, "0123456789")
}
