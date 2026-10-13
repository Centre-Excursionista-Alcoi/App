package org.centrexcursionistalcoi.app.security.crypto

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Message digests and comparing them. Passwords have their own, slow hash: see [Passwords].
 */
object Hashing {
    private const val SHA_256 = "SHA-256"

    fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance(SHA_256).digest(bytes)

    fun sha256(text: String): ByteArray = sha256(text.toByteArray())

    /** A SHA-256 digest to feed with [MessageDigest.update], for what can't be held in memory at once. */
    fun newSha256(): MessageDigest = MessageDigest.getInstance(SHA_256)

    /** The HMAC-SHA512 of [data] with [key]. */
    fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA512").apply { init(SecretKeySpec(key, "HmacSHA512")) }.doFinal(data)

    /**
     * Whether [a] and [b] are equal, in a time that doesn't depend on where they differ: use it, rather than `==`,
     * to compare secrets and their hashes.
     */
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean = MessageDigest.isEqual(a, b)

    fun constantTimeEquals(a: String, b: String): Boolean = constantTimeEquals(a.toByteArray(), b.toByteArray())
}
