package org.centrexcursionistalcoi.app.security

import org.centrexcursionistalcoi.app.security.crypto.RandomValues
import org.centrexcursionistalcoi.app.security.crypto.Hashing
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.centrexcursionistalcoi.app.data.TokenResponse
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Sign-ins an app (desktop) started and the user finishes with a passkey in their browser, see
 * `Api.Auth.Browser`. Held in memory only, like the tokens they carry for a few seconds: a pending sign-in lives
 * minutes, and the server runs as a single instance.
 */
object BrowserLogins {
    /** How long the user has to sign in, from the app asking. */
    val lifetime = 5.minutes

    /** A request is dropped this long after it was approved, if the app never redeemed it. */
    private val redeemWindow = 1.minutes

    // No 0/O/1/I: the code is compared by eye.
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    class Pending(
        val id: String,
        private val secretHash: ByteArray,
        /** Shown in the app and in the browser. */
        val code: String,
        val deviceName: String,
        /** Who asked, i.e. the app: the session is recorded for it, not for the browser that approves it. */
        val client: ClientInfo,
        private val createdAt: TimeMark = TimeSource.Monotonic.markNow(),
    ) {
        /** Completes when the user has signed in. */
        val approved = CompletableDeferred<Unit>()

        @Volatile
        var tokens: TokenResponse? = null
            internal set

        @Volatile
        private var approvedAt: TimeMark? = null

        fun isSecret(secret: String?): Boolean =
            secret != null && Hashing.constantTimeEquals(secretHash, Hashing.sha256(secret))

        val remaining get() = lifetime - createdAt.elapsedNow()

        internal fun isStale(): Boolean {
            val approvedAt = approvedAt
            return if (approvedAt != null) approvedAt.elapsedNow() > redeemWindow else remaining <= 0.seconds
        }

        internal fun markApproved() {
            approvedAt = TimeSource.Monotonic.markNow()
        }
    }

    private val requests = ConcurrentHashMap<String, Pending>()

    private fun randomCode(): String = RandomValues.string(6, CODE_ALPHABET).chunked(3).joinToString("-")

    /** @return the new request, and its secret: the only time it is known. */
    fun create(deviceName: String, client: ClientInfo): Pair<Pending, String> {
        purge()
        val secret = RandomValues.token()
        val pending = Pending(
            id = RandomValues.token(),
            secretHash = Hashing.sha256(secret),
            code = randomCode(),
            deviceName = deviceName,
            client = client,
        )
        requests[pending.id] = pending
        return pending to secret
    }

    /** The request with [id], if it exists and hasn't expired or been approved. */
    fun findPending(id: String): Pending? = requests[id]?.takeIf { !it.isStale() && !it.approved.isCompleted }

    /** The request with [id] if [secret] is its. */
    fun find(id: String, secret: String?): Pending? =
        requests[id]?.takeIf { !it.isStale() && it.isSecret(secret) }

    /**
     * Approves the pending request [id] with [tokens].
     * @return `false` if there is no such pending request: unknown, expired, or already approved.
     */
    fun approve(id: String, tokens: TokenResponse): Boolean {
        val pending = findPending(id) ?: return false
        synchronized(pending) {
            if (pending.approved.isCompleted) return false
            pending.tokens = tokens
            pending.markApproved()
            pending.approved.complete(Unit)
        }
        return true
    }

    /** Hands out the tokens of an approved request, once. */
    fun redeem(id: String, secret: String?): TokenResponse? {
        val pending = find(id, secret) ?: return null
        if (!pending.approved.isCompleted) return null
        if (!requests.remove(id, pending)) return null
        return pending.tokens.also { pending.tokens = null }
    }

    /** Waits until [pending] is approved, up to when it expires. */
    suspend fun awaitApproval(pending: Pending): Boolean =
        withTimeoutOrNull(pending.remaining) { pending.approved.await() } != null

    fun cancel(id: String) {
        requests.remove(id)
    }

    private fun purge() {
        requests.values.removeIf { it.isStale() }
    }
}
