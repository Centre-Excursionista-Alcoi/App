package org.centrexcursionistalcoi.app.auth

import kotlin.io.encoding.Base64
import org.kotlincrypto.hash.sha2.SHA256
import org.kotlincrypto.random.CryptoRand

private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

fun generateCodeVerifier(): String {
    // 32 random bytes -> base64url -> ~43 chars (within 43..128 required)
    val random = CryptoRand.Default
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return base64.encode(bytes)
}

fun String.toAsciiByteArray(): ByteArray {
    return ByteArray(length) { i ->
        val c = this[i]
        if (c.code > 127) error("Non-ASCII character: $c (${c.code})") else c.code.toByte()
    }
}

fun generateCodeChallenge(verifier: String): String {
    val digest = SHA256()
    val bytes = verifier.toAsciiByteArray()
    digest.update(bytes)
    return base64.encode(digest.digest())
}
