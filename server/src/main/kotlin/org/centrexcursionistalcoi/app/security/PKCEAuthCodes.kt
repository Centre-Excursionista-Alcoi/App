package org.centrexcursionistalcoi.app.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

fun generateCodeVerifier(): String {
    // 32 random bytes -> base64url -> ~43 chars (within 43..128 required)
    val random = SecureRandom()
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

fun generateCodeChallenge(verifier: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(verifier.toByteArray(StandardCharsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
}

fun decodeJwtPayload(jwt: String): String {
    val parts = jwt.split(".")
    if (parts.size < 2) return "{}"
    val payload = parts[1]
    val decoded = Base64.getUrlDecoder().decode(payload)
    return String(decoded, StandardCharsets.UTF_8)
}
