package org.centrexcursionistalcoi.app.auth

import kotlinx.serialization.Serializable

// Token response data class (partial)
@Serializable
data class TokenResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val expires_in: Int? = null,
    val refresh_token: String? = null,
    val id_token: String? = null
)
