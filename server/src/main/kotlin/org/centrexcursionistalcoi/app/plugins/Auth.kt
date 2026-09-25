package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.FCMRegistrationTokenEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.AuthEventType
import org.centrexcursionistalcoi.app.database.table.AuthEvents
import org.centrexcursionistalcoi.app.database.table.AuthSessionRevocationReason
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.FCMRegistrationTokens
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.database.table.Lendings
import org.centrexcursionistalcoi.app.database.table.Members
import org.centrexcursionistalcoi.app.database.table.ReceivedItems
import org.centrexcursionistalcoi.app.database.table.RecoverPasswordRequests
import org.centrexcursionistalcoi.app.database.table.UserCredentialRecords
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.Error.Companion.ERROR_INVALID_ARGUMENT
import org.centrexcursionistalcoi.app.error.Error.Companion.ERROR_MISSING_ARGUMENT
import org.centrexcursionistalcoi.app.error.Error.Companion.ERROR_PASSWORD_NOT_SAFE_ENOUGH
import org.centrexcursionistalcoi.app.error.Error.Companion.ERROR_PASSWORD_RESET_REQUEST_EXPIRED
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.notifications.EmailTemplate
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.routes.WebTemplate
import org.centrexcursionistalcoi.app.routes.WebTemplate.Companion.respondTemplate
import org.centrexcursionistalcoi.app.routes.assertContentType
import org.centrexcursionistalcoi.app.security.AuthTokens
import org.centrexcursionistalcoi.app.security.EmailValidation
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.webAuthnRoutes
import org.centrexcursionistalcoi.app.translation.locale
import org.centrexcursionistalcoi.app.utils.generateRandomString
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.upperCase
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * The duration after which a password recovery request expires.
 */
val passwordRequestExpiration = 15.minutes

private val logger = LoggerFactory.getLogger("Auth")

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

/**
 * Persists an [AuthEvents] row for [type] -- a login/registration/password-recovery request, successful or not --
 * so that a support report ("I'm not able to register") can be investigated after the fact by querying that table
 * directly (there's no API endpoint exposing it).
 * @param error The failure, or `null` if the request succeeded.
 */
internal fun RoutingContext.recordAuthEvent(type: AuthEventType, email: String?, error: Error?) {
    val remoteHost = call.request.origin.remoteHost
    val userAgent = call.request.headers[HttpHeaders.UserAgent]
    Database {
        AuthEvents.insert {
            it[this.type] = type
            it[this.email] = email?.uppercase()
            it[this.success] = error == null
            it[this.errorCode] = error?.code
            it[this.errorDescription] = error?.description
            it[this.ipAddress] = remoteHost
            it[this.userAgent] = userAgent
        }
    }
}

/** Records [error] as an [AuthEvents] row for [type], then responds it. */
internal suspend fun RoutingContext.respondAuthError(type: AuthEventType, email: String?, error: Error) {
    recordAuthEvent(type, email, error)
    respondError(error)
}

@OptIn(ExperimentalXmlUtilApi::class)
fun Route.configureAuthRoutes() {
    webAuthnRoutes()

    post("/register") {
        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val email = parameters["email"]?.trim()?.uppercase()
        val password = parameters["password"]?.trim()?.toCharArray()

        if (email == null) return@post respondAuthError(AuthEventType.REGISTER, null, Error.MissingArgument("email"))
        if (password == null) return@post respondAuthError(AuthEventType.REGISTER, email, Error.MissingArgument("password"))

        if (!EmailValidation.validate(email)) return@post respondAuthError(AuthEventType.REGISTER, email, Error.InvalidArgument("email"))

        // validate password
        if (!Passwords.isSafe(password)) return@post respondAuthError(AuthEventType.REGISTER, email, Error.PasswordNotSafeEnough())

        // check that the user doesn't exist
        val existingReference = Database { UserReferenceEntity.findByEmail(email) }
        if (existingReference != null) {
            return@post respondAuthError(AuthEventType.REGISTER, email, Error.UserAlreadyRegistered())
        }

        // check that the user is a valid an active member
        val memberReference = Database {
            MemberEntity.find { Members.email.upperCase() eq email }.limit(1).firstOrNull()
        }
        if (memberReference == null) {
            return@post respondAuthError(AuthEventType.REGISTER, email, Error.EmailNotFound())
        }
        if (memberReference.status != Member.Status.ACTIVE) {
            return@post respondAuthError(AuthEventType.REGISTER, email, Error.MemberIsNotActive())
        }

        // Update the user's password
        val hashedPassword = Passwords.hash(password)
        memberReference.insertUser(hashedPassword)

        // Success, respond accordingly
        recordAuthEvent(AuthEventType.REGISTER, email, null)
        call.respond(HttpStatusCode.OK)
    }

    post("/lost_password") {
        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val email = parameters["email"]?.trim()?.uppercase()
            ?: return@post respondAuthError(AuthEventType.LOST_PASSWORD, null, Error.MissingArgument("email"))

        val redirectTo = call.parameters["redirect_to"]?.trim()

        // check that the user exists
        val userReference = Database { UserReferenceEntity.findByEmail(email) }
        if (userReference == null) {
            return@post respondAuthError(AuthEventType.LOST_PASSWORD, email, Error.UserNotRegistered())
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
                "resetLink" to AppLinks.resetPassword(request[RecoverPasswordRequests.id].value),
            ),
        )

        recordAuthEvent(AuthEventType.LOST_PASSWORD, email, null)
        call.respond(HttpStatusCode.Accepted)
    }

    post("/reset_password") {
        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val webUi = parameters["webui"]?.toBoolean() ?: false
        val requestId = parameters["request_id"]?.trim()
        val newPassword = parameters["password"]?.trim()?.toCharArray()

        // Filled in once the request/user has been resolved below, so failures past that point are recorded
        // against the right email -- earlier failures (bad/missing request_id or password) have none to attach.
        var resolvedEmail: String? = null

        suspend fun respondError(error: Error) {
            recordAuthEvent(AuthEventType.RESET_PASSWORD, resolvedEmail, error)
            if (webUi) {
                // Keep validation in the current browser page: redirects to this host can be
                // intercepted as Android App Links and reopen the recovery flow.
                call.respondTemplate(
                    WebTemplate.LostPassword,
                    mapOf("requestId" to requestId, "error" to passwordResetErrorMessage(error.code)),
                )
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

        resolvedEmail = userReference.email

        // update the user's password
        val hashedPassword = Passwords.hash(newPassword)
        Database {
            userReference.password = hashedPassword
            // Whoever knew the old password may have logged in with it, or registered a restore key: none of that
            // survives the reset.
            AuthTokens.revokeAllSessions(userReference.sub.value, AuthSessionRevocationReason.PASSWORD_RESET)
            UserCredentialRecords.deleteWhere { UserCredentialRecords.user eq userReference.sub }
        }

        // delete the request
        Database {
            RecoverPasswordRequests.deleteWhere { RecoverPasswordRequests.id eq requestId }
        }

        // send a notification email
        Email.launch {
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
        }

        recordAuthEvent(AuthEventType.RESET_PASSWORD, userReference.email, null)

        // Success, respond accordingly
        if (webUi) {
            call.respondTemplate(WebTemplate.LostPassword, mapOf("success" to "true"))
        } else {
            call.respond(HttpStatusCode.OK)
        }
    }

    get("/reset_password") {
        val requestId = call.parameters["request_id"]?.trim()
        val errorCode = call.parameters["error"]?.trim()?.toIntOrNull()
        val success = call.parameters["success"]?.trim()?.toBoolean() ?: false

        val error = passwordResetErrorMessage(errorCode)

        call.respondTemplate(
            WebTemplate.LostPassword,
            mapOf(
                "requestId" to requestId,
                "error" to error,
                "success" to success.toString(),
            ),
        )
    }

    post("/delete_account") {
        val session = getUserSessionOrFail() ?: return@post

        val userReference = Database { UserReferenceEntity.findByEmail(session.email) } ?:
            return@post call.respondError(Error.UserReferenceNotFound())

        logger.warn("Deleting account for user ${userReference.sub} (NIF=${userReference.nif})")

        Database {
            DepartmentMemberEntity
                .find { DepartmentMembers.userSub eq userReference.sub }
                .onEach { it.delete() }
                .count()
        }.also { logger.info("Deleted $it entries from DepartmentMemberEntity") }
        Database {
            FCMRegistrationTokenEntity
                .find { FCMRegistrationTokens.user eq userReference.sub }
                .onEach { it.delete() }
                .count()
        }.also { logger.info("Deleted $it entries from FCMRegistrationTokenEntity") }
        Database {
            UserInsuranceEntity
                .find { UserInsurances.userSub eq userReference.sub }
                .onEach { it.delete() }
                .count()
        }.also { logger.info("Deleted $it entries from UserInsuranceEntity") }
        Database {
            ReceivedItemEntity
                .find { ReceivedItems.receivedBy eq userReference.sub }
                .onEach { it.delete() }
                .count()
        }.also { logger.info("Deleted $it entries from ReceivedItemEntity") }
        Database {
            LendingUserEntity
                .find { LendingUsers.userSub eq userReference.sub }
                .onEach { it.delete() }
                .count()
        }.also { logger.info("Deleted $it entries from LendingUserEntity") }
        Database {
            LendingEntity
                .find { Lendings.userSub eq userReference.sub }
                .onEach { it.delete() }
                .count()
        }.also { logger.info("Deleted $it entries from LendingEntity") }
        Database {
            UserInsuranceEntity
                .find { UserInsurances.userSub eq userReference.sub }
                .onEach { it.delete() }
                .count()
        }.also { logger.info("Deleted $it entries from UserInsuranceEntity") }

        Database {
            LendingEntity
                .find { Lendings.givenBy eq userReference.sub }
                .onEach { it.givenBy = null }
                .count()
        }.also { logger.info("Removed $it references from LendingEntity.givenBy") }

        Database {
            UserCredentialRecords.deleteWhere { UserCredentialRecords.user eq userReference.sub }
        }.also { logger.info("Deleted $it entries from UserCredentialRecords") }
        // Sessions and refresh tokens are deleted along with the user reference (ON DELETE CASCADE).
        Database { userReference.delete() }
        logger.info("Deleted user reference.")

        Database {
            MemberEntity
                .find { (Members.email eq userReference.email) or (Members.nif eq userReference.nif) }
                .onEach { it.delete() }
                .count()
        }.also { logger.info("Removed $it entries from MemberEntity") }

        call.respond(HttpStatusCode.NoContent)
    }
}

private fun passwordResetErrorMessage(errorCode: Int?): String? = when (errorCode) {
    ERROR_MISSING_ARGUMENT -> "Missing arguments."
    ERROR_PASSWORD_NOT_SAFE_ENOUGH -> "The provided password is not safe enough."
    ERROR_INVALID_ARGUMENT -> "The given request id is not valid."
    ERROR_PASSWORD_RESET_REQUEST_EXPIRED -> "The password reset request has expired."
    else -> null
}
