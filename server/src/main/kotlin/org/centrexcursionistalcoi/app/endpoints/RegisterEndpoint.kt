package org.centrexcursionistalcoi.app.endpoints

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.BasicAuthEndpoint
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.server.request.RegistrationRequest
import org.centrexcursionistalcoi.app.server.response.Errors
import org.centrexcursionistalcoi.app.validation.isSafePassword
import org.centrexcursionistalcoi.app.validation.isValidEmail

object RegisterEndpoint: BasicAuthEndpoint("/register") {
    override suspend fun RoutingContext.secureBody(username: String, password: String) {
        // Validate the email
        if (!username.isValidEmail) {
            respondFailure(Errors.InvalidEmail)
            return
        }
        // Validate the password
        if (!password.isSafePassword) {
            respondFailure(Errors.UnsafePassword)
            return
        }

        // Make sure the user doesn't exist
        val userExists = ServerDatabase { User.findById(username) != null }
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
            User.new(username) {
                name = body.name
                familyName = body.familyName
                nif = body.nif
                phone = body.phone
                salt = passwordSalt
                hash = passwordHash
            }
        }

        respondSuccess(HttpStatusCode.Created)
    }
}
