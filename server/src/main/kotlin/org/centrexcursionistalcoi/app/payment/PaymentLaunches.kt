package org.centrexcursionistalcoi.app.payment

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import org.centrexcursionistalcoi.app.security.crypto.RandomValues

/**
 * Payments that were started and whose user hasn't been sent to pay yet. The app can't submit the form of a provider
 * (Redsys needs a `POST`), so it opens `/pay?id=...` in a browser, which finds the session here. In memory, like the
 * browser sign-ins: it lives minutes, and the server runs as a single instance.
 */
object PaymentLaunches {
    /** How long a payment can be started from the app, once created. */
    val lifetime = 30.minutes

    class Launch(val id: String, val session: PaymentSession, private val createdAt: TimeMark = TimeSource.Monotonic.markNow()) {
        val remaining get() = lifetime - createdAt.elapsedNow()
    }

    private val launches = ConcurrentHashMap<String, Launch>()

    /** Makes [session] available to open, under a new unguessable id. */
    fun create(session: PaymentSession): Launch {
        purge()
        return Launch(RandomValues.token(), session).also { launches[it.id] = it }
    }

    /** The launch [id], if it exists and hasn't expired. */
    operator fun get(id: String): Launch? = launches[id]?.takeIf { it.remaining.isPositive() }

    private fun purge() {
        launches.values.removeIf { !it.remaining.isPositive() }
    }
}
