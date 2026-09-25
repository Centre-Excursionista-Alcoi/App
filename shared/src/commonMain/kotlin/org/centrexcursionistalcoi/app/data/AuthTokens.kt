package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A token pair issued by `/auth/login`, `/auth/refresh` and `/auth/webauthn/verify`,
 * shaped after an OAuth 2.0 token response (RFC 6749 §5.1).
 *
 * The access token is sent as `Authorization: Bearer <token>` on every request, and is only valid for
 * [accessTokenExpiresIn] seconds. The refresh token is only ever sent to `/auth/refresh` (for a new pair; the one
 * sent is invalidated) and `/auth/logout`.
 */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    /** Seconds until [accessToken] expires. */
    @SerialName("expires_in") val accessTokenExpiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String,
    /** Seconds until [refreshToken] expires if it isn't used before then. */
    @SerialName("refresh_token_expires_in") val refreshTokenExpiresIn: Long,
    /** The email of the account the tokens are for, which clients name their saved account after. */
    @SerialName("account_email") val accountEmail: String,
) {
    // Keep the tokens out of logs.
    override fun toString(): String =
        "TokenResponse(tokenType=$tokenType, accessTokenExpiresIn=$accessTokenExpiresIn, refreshTokenExpiresIn=$refreshTokenExpiresIn)"
}

/** Body of `/auth/refresh` and `/auth/logout`. */
@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String,
) {
    override fun toString(): String = "RefreshTokenRequest(refreshToken=<redacted>)"
}
