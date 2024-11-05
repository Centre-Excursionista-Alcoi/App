package org.centrexcursionistalcoi.app.security

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val email: String
)
