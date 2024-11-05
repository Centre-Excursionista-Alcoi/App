package org.centrexcursionistalcoi.app.auth

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val email: String
)
