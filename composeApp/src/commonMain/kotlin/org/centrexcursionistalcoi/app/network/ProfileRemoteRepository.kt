package org.centrexcursionistalcoi.app.network

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.response.ProfileResponse

object ProfileRemoteRepository {
    private val httpClient by lazy { getHttpClient() }

    suspend fun getProfile(): ProfileResponse? {
        val response = httpClient.get("/profile")
        if (response.status.isSuccess()) {
            val body = response.bodyAsText()
            return json.decodeFromString(ProfileResponse.serializer(), body)
        } else {
            return null
        }
    }
}
