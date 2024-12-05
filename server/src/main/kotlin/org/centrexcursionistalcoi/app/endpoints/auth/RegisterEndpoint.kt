package org.centrexcursionistalcoi.app.endpoints.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.enumeration.NotificationType
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.UsersTable
import org.centrexcursionistalcoi.app.endpoints.model.BasicAuthEndpoint
import org.centrexcursionistalcoi.app.push.FCM
import org.centrexcursionistalcoi.app.push.payload.AdminNotificationType
import org.centrexcursionistalcoi.app.push.payload.AdminUserPayload
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.server.request.RegistrationRequest
import org.centrexcursionistalcoi.app.server.response.Errors
import org.centrexcursionistalcoi.app.validation.isSafePassword
import org.centrexcursionistalcoi.app.validation.isValidEmail

object RegisterEndpoint: BasicAuthEndpoint("/register") {
    override suspend fun RoutingContext.secureBody(username: String, password: String) {
        // Validate the email
        val email = username.lowercase()
        if (!email.isValidEmail) {
            respondFailure(Errors.InvalidEmail)
            return
        }
        // Validate the password
        if (!password.isSafePassword) {
            respondFailure(Errors.UnsafePassword)
            return
        }

        // Make sure the user doesn't exist
        val userExists = ServerDatabase { User.findById(email) != null }
        if (userExists) {
            respondFailure(Errors.UserAlreadyExists)
            return
        }

        // Extract and validate the request body
        val body = call.receive<RegistrationRequest>()
        if (!body.validate()) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        // Hash password
        val passwordSalt = Passwords.generateSalt()
        val passwordHash = Passwords.hash(password, passwordSalt)

        ServerDatabase {
            User.new(email) {
                name = body.name
                familyName = body.familyName
                nif = body.nif
                phone = body.phone
                salt = passwordSalt
                hash = passwordHash
            }
        }

        // Notify all admins that a new user has registered
        FCM.notify(
            type = NotificationType.UserRegistered,
            payload = AdminUserPayload(AdminNotificationType.NewUserRegistered, email),
            criteria = { UsersTable.isAdmin eq true }
        )

        respondSuccess(HttpStatusCode.Created)
    }
}
