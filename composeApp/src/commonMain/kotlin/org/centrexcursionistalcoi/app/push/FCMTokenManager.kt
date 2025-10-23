package org.centrexcursionistalcoi.app.push

import io.github.aakira.napier.Napier
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import org.centrexcursionistalcoi.app.network.getHttpClient

object FCMTokenManager {
    suspend fun registerNewToken(token: String): String? {
        val client = getHttpClient()
        val response = client.submitForm(
            url = "/profile/fcmToken",
            formParameters = parameters {
                append("token", token)
            }
        )
        val body = response.bodyAsText()
        return if (response.status.isSuccess()) {
            body
        } else {
            Napier.e { "Could not register new FCM token (${response.status}): $body" }
            null
        }
    }

    suspend fun revokeToken(tokenId: String): Boolean {
        val client = getHttpClient()
        val response = client.delete("/profile/fcmToken/$tokenId")
        val body = response.bodyAsText()
        return if (!response.status.isSuccess()) {
            Napier.e { "Could not remove FCM token (${response.status}): $body" }
            false
        } else {
            true
        }
    }
}
