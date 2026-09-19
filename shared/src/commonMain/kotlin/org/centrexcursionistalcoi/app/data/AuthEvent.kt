package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * The kind of authentication-related request an [AuthEvent] records.
 */
@Serializable
enum class AuthEventType {
    LOGIN, REGISTER, LOST_PASSWORD, RESET_PASSWORD
}

/**
 * A record of an authentication-related request (login, registration, password recovery...), kept so that
 * support requests ("I can't register") can be investigated after the fact instead of relying on live logs.
 */
@Serializable
data class AuthEvent(
    val id: Uuid,
    @Serializable(InstantSerializer::class) val timestamp: Instant,
    val type: AuthEventType,
    val email: String?,
    val success: Boolean,
    val errorCode: Int?,
    val errorDescription: String?,
    val ipAddress: String?,
    val userAgent: String?,
)
