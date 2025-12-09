package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.Members
import org.centrexcursionistalcoi.app.database.table.RecoverPasswordRequests
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.Error.Companion.ERROR_INVALID_ARGUMENT
import org.centrexcursionistalcoi.app.error.Error.Companion.ERROR_MISSING_ARGUMENT
import org.centrexcursionistalcoi.app.error.Error.Companion.ERROR_PASSWORD_NOT_SAFE_ENOUGH
import org.centrexcursionistalcoi.app.error.Error.Companion.ERROR_PASSWORD_RESET_REQUEST_EXPIRED
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.mailersend.MailerSendEmail
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.notifications.EmailTemplate
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.routes.WebTemplate
import org.centrexcursionistalcoi.app.routes.WebTemplate.Companion.respondTemplate
import org.centrexcursionistalcoi.app.routes.assertContentType
import org.centrexcursionistalcoi.app.security.EmailValidation
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.translation.locale
import org.centrexcursionistalcoi.app.utils.generateRandomString
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.upperCase
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * The duration after which a password recovery request expires.
 */
val passwordRequestExpiration = 15.minutes

/**
 * Attempts to log in a user with the given email and password.
 * @param email The email of the user.
 * @param password The password of the user.
 * @return An Error if the login failed, or null if it succeeded.
 */
fun login(email: String, password: CharArray): Error? {
    // check that the user exists
    val existingReference = Database { UserReferenceEntity.findByEmail(email) }
    if (existingReference == null) {
        return Error.IncorrectPasswordOrEmail()
    }

    if (existingReference.isDisabled) {
        return Error.UserIsDisabled()
    }

    // verify password
    if (!Passwords.verify(password, existingReference.password)) {
        return Error.IncorrectPasswordOrEmail()
    }

    return null
}

@OptIn(ExperimentalXmlUtilApi::class)
fun Route.configureAuthRoutes() {
    post("/login") {
        getUserSession()?.let {
            return@post call.respond(HttpStatusCode.OK)
        }

        val contentType = call.request.contentType()
        val (email, password) = when {
            contentType.match(ContentType.Application.FormUrlEncoded) -> {
                val parameters = call.receiveParameters()
                val email = parameters["email"]?.trim()?.uppercase()
                val password = parameters["password"]?.trim()?.toCharArray()
                email to password
            }
            else -> {
                val credentials = call.request.basicAuthenticationCredentials()
                if (credentials == null) {
                    call.response.header(HttpHeaders.WWWAuthenticate, "Basic realm=\"Introduce your credentials\"")
                    return@post call.respondError(Error.IncorrectPasswordOrEmail())
                }
                val email = credentials.name.trim().uppercase()
                val password = credentials.password.trim().toCharArray()
                email to password
            }
        }

        if (email == null) return@post call.respondError(Error.IncorrectPasswordOrEmail())
        if (password == null) return@post call.respondError(Error.IncorrectPasswordOrEmail())

        val error = login(email, password)
        if (error != null) {
            return@post call.respondError(error)
        }

        // Success, set session and respond accordingly
        val session = Database { UserSession.fromEmail(email) }
        call.sessions.set(session)
        call.respond(HttpStatusCode.OK)
    }

    post("/register") {
        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val email = parameters["email"]?.trim()?.uppercase()
        val password = parameters["password"]?.trim()?.toCharArray()

        if (email == null) return@post call.respondError(Error.MissingArgument("email"))
        if (password == null) return@post call.respondError(Error.MissingArgument("password"))

        if (!EmailValidation.validate(email)) return@post call.respondError(Error.InvalidArgument("email"))

        // validate password
        if (!Passwords.isSafe(password)) return@post call.respondError(Error.PasswordNotSafeEnough())

        // check that the user doesn't exist
        val existingReference = Database { UserReferenceEntity.findByEmail(email) }
        if (existingReference != null) {
            return@post call.respondError(Error.UserAlreadyRegistered())
        }

        // check that the user is a valid an active member
        val memberReference = Database {
            MemberEntity.find { Members.email.upperCase() eq email }.limit(1).firstOrNull()
        }
        if (memberReference == null) {
            return@post call.respondError(Error.EmailNotFound())
        }
        if (memberReference.status != Members.Status.ACTIVE) {
            return@post call.respondError(Error.MemberIsNotActive())
        }

        // Update the user's password
        val hashedPassword = Passwords.hash(password)
        memberReference.insertUser(hashedPassword)

        // Success, respond accordingly
        call.respond(HttpStatusCode.OK)
    }

    post("/lost_password") {
        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val email = parameters["email"]?.trim()?.uppercase() ?: return@post call.respondError(Error.MissingArgument("email"))

        val redirectTo = call.parameters["redirect_to"]?.trim()

        // check that the user exists
        val userReference = Database { UserReferenceEntity.findByEmail(email) }
        if (userReference == null) {
            return@post call.respondError(Error.UserNotRegistered())
        }

        // Create a new request
        val request = Database {
            RecoverPasswordRequests.insert {
                it[this.id] = generateRandomString(128)
                it[this.user] = userReference.id
                it[this.redirectTo] = redirectTo
            }
        }

        val locale = call.request.locale()

        Email.sendTemplate(
            to = listOf(
                MailerSendEmail(email, userReference.fullName)
            ),
            template = EmailTemplate.LostPassword,
            locale = locale,
            args = mapOf(
                "userName" to userReference.fullName,
                "resetLink" to "${call.request.origin.scheme}://${call.request.host()}:${call.request.port()}/reset_password?request_id=${request[RecoverPasswordRequests.id]}",
            ),
        )

        call.respond(HttpStatusCode.Accepted)
    }

    post("/reset_password") {
        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val webUi = call.receiveParameters()["webui"]?.toBoolean() ?: false
        val requestId = parameters["request_id"]?.trim()
        val newPassword = parameters["password"]?.trim()?.toCharArray()

        suspend fun respondError(error: Error) {
            if (webUi) {
                call.respondRedirect("/reset_password_form?request_id=$requestId&error=${error.code}")
            } else {
                call.respondError(error)
            }
        }

        if (requestId == null) return@post respondError(Error.MissingArgument("request_id"))
        if (newPassword == null) return@post respondError(Error.MissingArgument("password"))

        // validate password
        if (!Passwords.isSafe(newPassword)) return@post respondError(Error.PasswordNotSafeEnough())

        // find the request
        val request = Database {
            RecoverPasswordRequests.selectAll().where { RecoverPasswordRequests.id eq requestId }.limit(1).firstOrNull()
        } ?: return@post respondError(Error.InvalidArgument("request_id"))

        // check expiration
        val timestamp = request[RecoverPasswordRequests.timestamp]
        if (timestamp.plus(passwordRequestExpiration.toJavaDuration()) < now()) {
            return@post respondError(Error.PasswordResetRequestExpired())
        }

        val userId = Database { request[RecoverPasswordRequests.user] }

        // find the user
        val userReference = Database {
            UserReferenceEntity.findById(userId)
        } ?: return@post respondError(Error.InvalidArgument("request_id"))

        // update the user's password
        val hashedPassword = Passwords.hash(newPassword)
        Database {
            userReference.password = hashedPassword
        }

        // delete the request
        Database {
            RecoverPasswordRequests.deleteWhere { RecoverPasswordRequests.id eq requestId }
        }

        // send a notification email
        val locale = call.request.locale()
        Email.sendTemplate(
            to = listOf(
                MailerSendEmail(userReference.email, userReference.fullName)
            ),
            template = EmailTemplate.PasswordChangedNotification,
            locale = locale,
            args = mapOf(
                "userName" to userReference.fullName,
            ),
        )

        // Success, respond accordingly
        call.respond(HttpStatusCode.OK)
    }

    get("/reset_password") {
        val requestId = call.parameters["request_id"]?.trim()
        val errorCode = call.parameters["error"]?.trim()?.toIntOrNull()
        val success = call.parameters["success"]?.trim()?.toBoolean() ?: false

        val error = when (errorCode) {
            ERROR_MISSING_ARGUMENT -> "Missing arguments."
            ERROR_PASSWORD_NOT_SAFE_ENOUGH -> "The provided password is not safe enough."
            ERROR_INVALID_ARGUMENT -> "The given request id is not valid."
            ERROR_PASSWORD_RESET_REQUEST_EXPIRED -> "The password reset request has expired."
            else -> null
        }

        call.respondTemplate(
            WebTemplate.LostPassword,
            mapOf(
                "requestId" to requestId,
                "error" to error,
                "success" to success.toString(),
            ),
        )
    }
}
