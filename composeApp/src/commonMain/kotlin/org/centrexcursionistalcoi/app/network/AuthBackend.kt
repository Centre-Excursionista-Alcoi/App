package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import io.ktor.http.isSuccess
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.error.AuthException
import org.centrexcursionistalcoi.app.error.ServerException
import org.centrexcursionistalcoi.app.server.request.RegistrationRequest

object AuthBackend {
    /**
     * Logs in using the given credentials.
     * @param email The user's email
     * @param password The user's password
     * @return The token returned by the server and its expiration timestamp in millis if any.
     * Ktor already stores it automatically in cookies. They are HttpOnly, so client cannot set them.
     * @throws AuthException If there's an error during the authentication
     * @throws ServerException If there's another kind of error during login
     * @throws NullPointerException If the server didn't return a token
     */
    suspend fun login(email: String, password: String): Pair<String, Long?> {
        try {
            val httpResponse = Backend.post("/login", basicAuth = email to password)
            val token = httpResponse.cookies().find { it.name == "user_session" }!!
            val expiration = token.expires?.timestamp
            return token.value to expiration
        } catch (e: ServerException) {
            AuthException.fromCode(e.code)?.let { throw it } ?: throw e
        }
    }

    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        familyName: String,
        nif: String,
        phone: String
    ) {
        try {
            Backend.post(
                "/register",
                basicAuth = email to password,
                bodySerializer = RegistrationRequest.serializer(),
                body = RegistrationRequest(
                    name = firstName,
                    familyName = familyName,
                    nif = nif,
                    phone = phone
                )
            )
        } catch (e: ServerException) {
            AuthException.fromCode(e.code)?.let { throw it } ?: throw e
        }
    }

    /**
     * Notifies the server of a new FCM token for the current session.
     * @param token The FCM token
     * @return True if the token was successfully sent to the server
     */
    suspend fun notifyToken(token: String): Boolean {
        try {
            val account = AccountManager.get()
            if (account == null) {
                Napier.i { "Won't send FCM token to server since not logged in." }
                return false
            }
            val response = Backend.post("/fcm_token", body = token, bodySerializer = String.serializer())
            return response.status.isSuccess()
        } catch (e: ServerException) {
            Napier.e(e) { "Could not notify about new FCM token." }
            return false
        }
    }
}
