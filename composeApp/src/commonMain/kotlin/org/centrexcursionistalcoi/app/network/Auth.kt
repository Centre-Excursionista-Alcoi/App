package org.centrexcursionistalcoi.app.network

import io.ktor.client.request.basicAuth
import org.centrexcursionistalcoi.app.error.AuthException
import org.centrexcursionistalcoi.app.error.ServerException

object Auth {
    suspend fun login(email: String, password: String) {
        try {
            Backend.post("/login") {
                basicAuth(email, password)
            }
        } catch (e: ServerException) {
            AuthException.fromCode(e.code)?.let { throw it } ?: throw e
        }
    }
}
