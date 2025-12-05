package org.centrexcursionistalcoi.app.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

// TODO: Set in environment variables and load from there
val secretEncryptKey = hex("00112233445566778899aabbccddeeff")
val secretSignKey = hex("6819b57a326945c1968f45236589")

/**
 * @param sub Subject Identifier
 * @param fullName Full Name
 * @param email Email Address
 * @param groups List of groups the user belongs to
 */
@Serializable
data class UserSession(val sub: String, val fullName: String, val email: String, val groups: List<String>) {
    companion object {
        const val COOKIE_NAME = "USER_SESSION"

        @Deprecated("Do not authenticate with NIF, use email.", ReplaceWith("fromEmail"))
        context(_: JdbcTransaction)
        fun fromNif(nif: String) = UserReferenceEntity.findByNif(nif)?.let { reference ->
            val email = reference.email ?: error("User with NIF $nif has no email")

            UserSession(
                sub = reference.sub.value,
                fullName = reference.fullName,
                email = email,
                groups = reference.groups,
            )
        } ?: error("User with NIF $nif not found")

        /**
         * Creates a UserSession from an email address, checking that the user exists.
         * @param email Email Address of the user
         * @throws IllegalArgumentException if the user is not found
         */
        context(_: JdbcTransaction)
        fun fromEmail(email: String) = UserReferenceEntity.findByEmail(email)?.let { reference ->
            UserSession(
                sub = reference.sub.value,
                fullName = reference.fullName,
                email = email,
                groups = reference.groups,
            )
        } ?: error("User with email $email not found")

        fun ApplicationCall.getUserSession(): UserSession? {
            val session = sessions.get<UserSession>()
            response.header("CEA-LoggedIn", (session != null).toString())
            return session
        }

        fun RoutingContext.getUserSession(): UserSession? = call.getUserSession()

        suspend fun RoutingContext.getUserSessionOrFail(): UserSession? {
            val session = getUserSession()
            if (session == null) {
                respondError(Error.NotLoggedIn())
                return null
            } else {
                return session
            }
        }

        suspend fun RoutingContext.assertAdmin(): UserSession? {
            val session = getUserSessionOrFail() ?: return null
            if (!session.isAdmin()) {
                respondError(Error.NotAnAdmin())
                return null
            }
            return session
        }
    }

    fun isAdmin(): Boolean = groups.contains(ADMIN_GROUP_NAME)
}

fun Application.configureSessions(isTesting: Boolean, isDevelopment: Boolean) {
    install(Sessions) {
        cookie<UserSession>(UserSession.COOKIE_NAME) {
            cookie.httpOnly = true                        // Prevent JS access
            cookie.secure = !isTesting && !isDevelopment  // Use HTTPS in production
            if (!isDevelopment) cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.maxAgeInSeconds = 7 * 24 * 60 * 60 // 1 week

            // Encrypt and sign the cookie to prevent tampering
            transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretSignKey))
        }
    }
}
