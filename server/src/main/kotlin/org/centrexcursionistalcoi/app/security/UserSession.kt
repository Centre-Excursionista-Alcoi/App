package org.centrexcursionistalcoi.app.security

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val sessionId: String,
    val expiresAt: Instant,
    val email: String,
    val ip: String,
    val fcmToken: String? = null
)
