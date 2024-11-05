package org.centrexcursionistalcoi.app.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import org.centrexcursionistalcoi.app.error.ServerException
import org.centrexcursionistalcoi.app.serverJson

object Backend {
    private val client = HttpClient {
        // Set the default server URL
        defaultRequest {
            url {
                protocol = URLProtocol.HTTP
                host = "127.0.0.1"
                port = 8080
            }
        }
        // Install authentication plugin
        install(Auth)
        // Configure cookies storage
        install(HttpCookies)
        // Configure content negotiation
        install(ContentNegotiation) {
            json(serverJson)
        }
    }

    /**
     * Send a POST request to the server
     * @param path The path to send the request to
     * @param block Additional configuration for the request
     * @return The response from the server
     * @throws ServerException If the server responds with an error
     */
    suspend fun post(
        path: String,
        basicAuth: Pair<String, String>? = null,
        body: Any? = null,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        val response = client.post(path) {
            basicAuth?.let { (username, password) -> basicAuth(username, password) }
            body?.let {
                contentType(ContentType.Application.Json)
                setBody(it)
            }

            block()
        }
        if (response.status.isSuccess()) {
            return response
        }

        val code = response.headers["X-Error-Code"]?.toIntOrNull() ?: (response.status.value + 1000)
        throw ServerException(code, response.bodyAsText())
    }
}
