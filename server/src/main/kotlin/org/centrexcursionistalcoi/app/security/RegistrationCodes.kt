package org.centrexcursionistalcoi.app.security

import org.centrexcursionistalcoi.app.security.crypto.toBase64Url
import org.centrexcursionistalcoi.app.security.crypto.RandomValues
import org.centrexcursionistalcoi.app.security.crypto.Hashing
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.storage.RedisStoreMap

/**
 * The codes emailed to prove that the person registering owns the email they register with: a new account can only
 * be created with the last code sent to its email, while it's valid.
 *
 * Only a hash of each code is stored, and a code stops working after [MAX_ATTEMPTS] wrong guesses, so it can't be
 * brute-forced within its [validity].
 */
object RegistrationCodes {
    private val validity = 10.minutes
    private const val MAX_ATTEMPTS = 5

    @Serializable
    private class Stored(val hash: String, val attempts: Int, val expiresAt: Long)

    private fun key(email: String) = "registration_code:${email.uppercase()}"

    private fun hash(code: String): String =
        Hashing.sha256(code).toBase64Url()

    /**
     * Creates a new code for [email], replacing any previous one.
     * @return the code, to email.
     */
    suspend fun create(email: String): String {
        val code = RandomValues.digits(6)
        val stored = Stored(hash(code), 0, (Clock.System.now() + validity).toEpochMilliseconds())
        RedisStoreMap.default.put(key(email), json.encodeToString(Stored.serializer(), stored), validity.inWholeSeconds)
        return code
    }

    /**
     * Checks [code] against the one sent to [email], counting it as an attempt if it's wrong.
     * @param consume If `true`, a right code can't be used again.
     * @return whether it's the right code, and still valid.
     */
    suspend fun check(email: String, code: String, consume: Boolean = false): Boolean {
        val key = key(email)
        val stored = RedisStoreMap.default.get(key)
            ?.let { json.decodeFromString(Stored.serializer(), it) }
            ?: return false
        val remaining = stored.expiresAt - Clock.System.now().toEpochMilliseconds()
        if (remaining <= 0 || stored.attempts >= MAX_ATTEMPTS) {
            RedisStoreMap.default.remove(key)
            return false
        }

        val isRight = Hashing.constantTimeEquals(hash(code.trim()), stored.hash)
        if (!isRight) {
            val updated = Stored(stored.hash, stored.attempts + 1, stored.expiresAt)
            RedisStoreMap.default.put(key, json.encodeToString(Stored.serializer(), updated), remaining / 1000 + 1)
        } else if (consume) {
            RedisStoreMap.default.remove(key)
        }
        return isRight
    }
}
