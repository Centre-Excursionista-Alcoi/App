package org.centrexcursionistalcoi.app.network

import org.centrexcursionistalcoi.app.error.AuthException
import org.centrexcursionistalcoi.app.error.ServerException
import org.centrexcursionistalcoi.app.server.request.RegistrationRequest

object AuthBackend {
    suspend fun login(email: String, password: String) {
        try {
            Backend.post("/login", basicAuth = email to password).also {
                val cookies = it.cookies()
                println("Cookies: $cookies")
            }
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
}
