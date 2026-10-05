package org.centrexcursionistalcoi.app.routes.sync

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Instant
import org.centrexcursionistalcoi.app.security.UserSession

/**
 * A part of what `GET /sync` answers: the same data its own route answers, for the same user.
 *
 * Every route that lists something the app keeps a copy of registers one, so the sync can't drift from the routes:
 * both use the same [snapshot].
 */
class SyncSection(
    /** The name of the section in the response, and of the query parameter with the last time it was synced. */
    val key: String,
    /**
     * Anything that must be done before the snapshot is taken, outside its transaction (it can be slow, or need a
     * suspending call).
     */
    val prepare: suspend (UserSession) -> Unit = {},
    /**
     * When the section last changed for this user, or `null` if that isn't known and it is always sent.
     */
    val lastUpdate: suspend (UserSession) -> Instant? = { null },
    /**
     * The JSON of the section for this user. Runs inside the transaction shared by all the sections, so they are
     * consistent with each other.
     */
    val snapshot: (UserSession) -> String,
)

object SyncSections {
    private val sections = ConcurrentHashMap<String, SyncSection>()

    /** Registers [section], replacing the one with its key, if any. */
    fun register(section: SyncSection) {
        sections[section.key] = section
    }

    fun all(): List<SyncSection> = sections.values.sortedBy { it.key }
}
