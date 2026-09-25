package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

/** How an [AuthSessions] row was started. */
enum class AuthSessionMethod {
    /** Email and password, through `/auth/login`. */
    PASSWORD,

    /** A WebAuthn credential (a restore key), through `/auth/webauthn/verify`. */
    WEBAUTHN,
}

/** Why an [AuthSessions] row was revoked. */
enum class AuthSessionRevocationReason {
    LOGOUT,

    /** The user's password was reset: every session that existed before is revoked. */
    PASSWORD_RESET,

    /**
     * A refresh token that had already been rotated was presented again: either the legitimate client or an
     * attacker holds a stolen copy, and there's no telling which, so the whole session is revoked.
     */
    REFRESH_TOKEN_REUSE,
}

/**
 * A logged-in device: the server-side state behind a refresh token, so that it can be revoked, and expires
 * whether or not the client cooperates.
 *
 * Access tokens name their session (the `sid` claim), and are only accepted while it's active (see
 * `security/AuthTokens.kt`), so revoking a session also cuts off its access tokens immediately.
 */
object AuthSessions : UUIDTable("auth_sessions") {
    val user = reference("user", UserReferences, onDelete = ReferenceOption.CASCADE)
    val method = enumerationByName<AuthSessionMethod>("method", 32)

    val createdAt = timestamp("created_at")
    val lastUsedAt = timestamp("last_used_at")

    /** Sliding expiry: pushed forward on every refresh, but never past [absoluteExpiresAt]. */
    val expiresAt = timestamp("expires_at")

    /** Hard limit after which the user must authenticate again, however active the session is. */
    val absoluteExpiresAt = timestamp("absolute_expires_at")

    val revokedAt = timestamp("revoked_at").nullable()
    val revocationReason = enumerationByName<AuthSessionRevocationReason>("revocation_reason", 32).nullable()

    /** Where the session was started from, to show the user and investigate abuse. */
    val ipAddress = varchar("ip_address", 64).nullable()
    val userAgent = text("user_agent").nullable()

    init {
        index(false, user)
    }
}
