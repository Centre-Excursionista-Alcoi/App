package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.response.header
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.SessionTransportTransformerEncrypt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.util.hex
import kotlinx.serialization.Serializable

val secretEncryptKey = hex("00112233445566778899aabbccddeeff")
val secretSignKey = hex("6819b57a326945c1968f45236589")

/**
 * @param sub Subject Identifier
 * @param username Preferred Username
 * @param email Email Address
 */
@Serializable
data class UserSession(val sub: String, val username: String, val email: String, val groups: List<String>) {
    companion object {
        const val ADMIN_GROUP_NAME = "CEA Admin"
        const val COOKIE_NAME = "USER_SESSION"

        fun RoutingContext.getUserSession(): UserSession? {
            val session = call.sessions.get<UserSession>()
            call.response.header("CEA-LoggedIn", (session != null).toString())
            return session
        }

        suspend fun RoutingContext.getUserSessionOrFail(): UserSession? {
            val session = getUserSession()
            if (session == null) {
                call.respondText("Not logged in", status = HttpStatusCode.Unauthorized)
                return null
            } else {
                return session
            }
        }
    }

    fun isAdmin(): Boolean = groups.contains(ADMIN_GROUP_NAME)
}

@Serializable
data class LoginSession(val redirectUrl: String?) {
    companion object {
        const val COOKIE_NAME = "LOGIN_SESSION"
    }
}

fun Application.configureSessions(isTesting: Boolean, isDevelopment: Boolean) {
    install(Sessions) {
        cookie<UserSession>(UserSession.COOKIE_NAME) {
            cookie.httpOnly = true                        // Prevent JS access
            cookie.secure = !isTesting && !isDevelopment  // Use HTTPS in production
            cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.maxAgeInSeconds = 7 * 24 * 60 * 60 // 1 week

            // Encrypt and sign the cookie to prevent tampering
            transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretSignKey))
        }
        cookie<LoginSession>(LoginSession.COOKIE_NAME) {
            cookie.httpOnly = true      // Prevent JS access
            cookie.secure = !isTesting  // Use HTTPS in production
            cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.maxAgeInSeconds = 5 * 60 // 5 minutes
        }
    }
}
