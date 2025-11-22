package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.routes.assertContentType
import org.centrexcursionistalcoi.app.security.NIFValidation
import org.centrexcursionistalcoi.app.security.Passwords

/**
 * Attempts to log in a user with the given NIF and password.
 * @param nif The NIF of the user.
 * @param password The password of the user.
 * @return An Error if the login failed, or null if it succeeded.
 */
fun login(nif: String, password: CharArray): Error? {
    // check that the user exists
    val existingReference = Database { UserReferenceEntity.findByNif(nif) }
    if (existingReference == null) {
        return Error.IncorrectPasswordOrNIF()
    }

    val passwordHash = existingReference.password ?: return Error.PasswordNotSet()

    // verify password
    if (!Passwords.verify(password, passwordHash)) {
        return Error.IncorrectPasswordOrNIF()
    }

    return null
}

fun Route.configureAuthRoutes() {
    post("/login") {
        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val nif = parameters["nif"]?.trim()?.uppercase()
        val password = parameters["password"]?.trim()?.toCharArray()

        if (nif == null) return@post call.respondError(Error.IncorrectPasswordOrNIF())
        if (password == null) return@post call.respondError(Error.IncorrectPasswordOrNIF())

        val error = login(nif, password)
        if (error != null) {
            return@post call.respondError(error)
        }

        // Success, set session and respond accordingly
        val session = Database { UserSession.fromNif(nif) }
        call.sessions.set(session)
        call.respond(HttpStatusCode.OK)
    }

    post("/register") {
        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val nif = parameters["nif"]?.trim()?.uppercase()
        val password = parameters["password"]?.trim()?.toCharArray()

        if (nif == null) return@post call.respondError(Error.MissingArgument("nif"))
        if (password == null) return@post call.respondError(Error.MissingArgument("password"))

        if (!NIFValidation.validate(nif)) return@post call.respondError(Error.InvalidArgument("nif"))

        // validate password
        if (!Passwords.isSafe(password)) return@post call.respondError(Error.PasswordNotSafeEnough())

        // check that the user exists
        val existingReference = Database { UserReferenceEntity.findByNif(nif) }
        if (existingReference == null) {
            return@post call.respondError(Error.NIFNotRegistered())
        }

        // Make sure the user doesn't already have a password set
        if (existingReference.password != null) {
            return@post call.respondError(Error.UserAlreadyRegistered())
        }

        // Update the user's password
        val hashedPassword = Passwords.hash(password)
        Database {
            existingReference.password = hashedPassword
        }

        // Success, respond accordingly
        call.respond(HttpStatusCode.OK)
    }
}
