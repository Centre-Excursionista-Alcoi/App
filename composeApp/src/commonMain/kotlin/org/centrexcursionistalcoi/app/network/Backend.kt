package org.centrexcursionistalcoi.app.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import org.centrexcursionistalcoi.app.error.ServerException

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
    }

    /**
     * Send a POST request to the server
     * @param path The path to send the request to
     * @param block Additional configuration for the request
     * @return The response from the server
     * @throws ServerException If the server responds with an error
     */
    suspend fun post(path: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        val response = client.post(path, block)
        if (response.status.isSuccess()) {
            return response
        }

        val code = response.headers["X-Error-Code"]?.toIntOrNull() ?: (response.status.value + 1000)
        throw ServerException(code, response.bodyAsText())
    }
}
