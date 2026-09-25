package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * Every refresh token issued for an [AuthSessions] row, current or already rotated.
 *
 * Only a SHA-256 hash of each token is stored, so a leak of this table can't be replayed. Rotated tokens are
 * kept (with [usedAt] set) until their session is deleted, so that presenting one again is recognized as reuse
 * of a stolen token instead of just an unknown one.
 */
object AuthRefreshTokens : IdTable<String>("auth_refresh_tokens") {
    /** Hex-encoded SHA-256 of the token. */
    override val id: Column<EntityID<String>> = varchar("token_hash", 64).entityId()

    val session = reference("session", AuthSessions, onDelete = ReferenceOption.CASCADE)

    val createdAt = timestamp("created_at")

    /** When this token was exchanged for its successor, or `null` while it's the session's current one. */
    val usedAt = timestamp("used_at").nullable()

    /** The hash of the token issued in exchange for this one, once [usedAt] is set. */
    val replacedBy = varchar("replaced_by", 64).nullable()

    init {
        index(false, session)
    }
}
