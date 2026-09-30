package org.centrexcursionistalcoi.app.push

import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.getHttpClient
import org.centrexcursionistalcoi.app.network.requestBody
import org.centrexcursionistalcoi.app.request.RegisterFCMTokenRequest
import org.centrexcursionistalcoi.app.routes.Api

object FCMTokenRemote {
    /**
     * Register a new FCM token with the server.
     * @param token The FCM token to register.
     * @throws ServerException if the registration fails.
     */
    suspend fun registerNewToken(token: String) {
        val client = getHttpClient()
        val response = client.post(Api.Profile.FCMToken()) {
            setBody(requestBody(RegisterFCMTokenRequest(token), RegisterFCMTokenRequest.serializer()))
        }
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
        val response = client.delete(Api.Profile.FCMToken.ByToken(token))
        if (!response.status.isSuccess()) {
            throw response.bodyAsError().toThrowable()
        }
    }
}
