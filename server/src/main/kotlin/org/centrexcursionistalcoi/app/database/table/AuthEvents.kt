package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * The kind of authentication-related request an [AuthEvents] row records.
 */
enum class AuthEventType {
    LOGIN, REGISTER, LOST_PASSWORD, RESET_PASSWORD,

    /** A restore key (WebAuthn) redeemed for tokens. */
    WEBAUTHN_LOGIN,

    /** A passkey used to log in. */
    PASSKEY_LOGIN,

    /** A registration code emailed, to prove the email is the registering user's. */
    REGISTRATION_CODE,

    /** A passkey added to an account (including a new account's first). */
    PASSKEY_ADDED,

    /** A passkey removed from an account. */
    PASSKEY_REMOVED,

    /** The password of an account set, changed or removed from its security settings. */
    PASSWORD_CHANGED,

    /** Only failed refreshes are recorded: successful ones happen every few minutes per active user. */
    TOKEN_REFRESH,

    /** An already-used refresh token was presented again, and its session revoked. */
    REFRESH_TOKEN_REUSE,
}

/**
 * Records every authentication-related request (login, registration, password recovery...), successful or not,
 * so that a support report ("I'm not able to register") can be investigated after the fact by querying this
 * table directly -- there's no API endpoint exposing it.
 */
object AuthEvents : UuidTable("auth_events") {
    val timestamp = timestamp("timestamp").defaultExpression(DatabaseNowExpression)

    /** Name of an [AuthEventType] constant. */
    val type = enumerationByName<AuthEventType>("type", 32)

    /** The email involved in the request, if known at the point of failure/success. Not normalized/validated. */
    val email = text("email").nullable()

    val success = bool("success")

    /** [org.centrexcursionistalcoi.app.error.Error.code] of the failure, `null` on success. */
    val errorCode = integer("error_code").nullable()

    /** [org.centrexcursionistalcoi.app.error.Error.description] of the failure, `null` on success. */
    val errorDescription = text("error_description").nullable()

    val ipAddress = varchar("ip_address", 64).nullable()
    val userAgent = text("user_agent").nullable()
}
