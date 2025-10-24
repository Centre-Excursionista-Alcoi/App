package org.centrexcursionistalcoi.app.push

import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.getHttpClient

object FCMTokenRemote {
    /**
     * Register a new FCM token with the server.
     * @param token The FCM token to register.
     * @throws ServerException if the registration fails.
     */
    suspend fun registerNewToken(token: String) {
        val client = getHttpClient()
        val response = client.submitForm(
            url = "/profile/fcmToken",
            formParameters = parameters {
                append("token", token)
            }
        )
        if (!response.status.isSuccess()) {
            throw response.bodyAsError().toThrowable()
        }
    }

    /**
     * Revoke the given FCM token from the server.
     * @param token The FCM token to revoke.
     * @throws ServerException if the revocation fails.
     */
    suspend fun revokeToken(token: String) {
        val client = getHttpClient()
        val response = client.delete("/profile/fcmToken/$token")
        if (!response.status.isSuccess()) {
            throw response.bodyAsError().toThrowable()
        }
    }
}
