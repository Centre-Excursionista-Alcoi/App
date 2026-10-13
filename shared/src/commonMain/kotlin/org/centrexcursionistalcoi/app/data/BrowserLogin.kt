package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of `POST /auth/browser`. */
@Serializable
data class BrowserLoginStartRequest(
    /** Names the app asking to sign in, shown to the user in the browser. */
    @SerialName("device_name") val deviceName: String,
)

/**
 * A sign-in the app started and the user finishes in their browser, see `Api.Auth.Browser`.
 */
@Serializable
data class BrowserLoginRequest(
    val id: String,
    /** Proves the holder started the request: needed to listen for its result and redeem it. Never in the URL. */
    val secret: String,
    /** Short code shown both in the app and in the browser, for the user to check they are the same request. */
    val code: String,
    /** The page to open in the browser. */
    val url: String,
    @SerialName("expires_in") val expiresIn: Long,
) {
    override fun toString(): String = "BrowserLoginRequest(id=$id, code=$code, url=$url, secret=<redacted>)"
}

/** Name of the header carrying [BrowserLoginRequest.secret]. */
const val BROWSER_LOGIN_SECRET_HEADER = "X-Login-Secret"

/** Event the server sends through SSE once the browser login was approved. */
const val BROWSER_LOGIN_APPROVED_EVENT = "approved"

/** Event the server sends through SSE if the browser login expired before being approved. */
const val BROWSER_LOGIN_EXPIRED_EVENT = "expired"
