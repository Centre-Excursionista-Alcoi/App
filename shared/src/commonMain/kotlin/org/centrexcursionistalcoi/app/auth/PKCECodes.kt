package org.centrexcursionistalcoi.app.auth

import kotlin.io.encoding.Base64
import kotlin.random.Random
import org.kotlincrypto.hash.sha2.SHA256

fun generateCodeVerifier(): String {
    // 32 random bytes -> base64url -> ~43 chars (within 43..128 required)
    val random = Random.Default
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return Base64.UrlSafe.encode(bytes)
}

fun generateCodeChallenge(verifier: String): String {
    val digest = SHA256()
    val bytes = verifier.encodeToByteArray() // Should be: US_ASCII
    digest.update(bytes)
    return Base64.UrlSafe.encode(digest.digest())
}
