package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.basicAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.error.ServerException
import org.centrexcursionistalcoi.app.serverJson

object Backend {
    private val client = HttpClient {
        // Set the default server URL
        defaultRequest {
            url {
                protocol = if (BuildKonfig.BACKEND_HTTPS) URLProtocol.HTTPS else URLProtocol.HTTP
                host = BuildKonfig.BACKEND_HOST
                port = BuildKonfig.BACKEND_PORT
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

    private suspend fun request(
        httpMethod: HttpMethod,
        path: String,
        basicAuth: Pair<String, String>?,
        block: HttpRequestBuilder.() -> Unit
    ): HttpResponse {
        val response = client.request(path) {
            method = httpMethod

            basicAuth?.let { (username, password) -> basicAuth(username, password) }

            block()
        }
        if (response.status.isSuccess()) {
            return response
        }

        val code = response.headers["X-Error-Code"]?.toIntOrNull() ?: (response.status.value + 1000)
        throw ServerException(code, response.bodyAsText())
    }

    /**
     * Send a GET request to the server
     * @param path The path to send the request to
     * @param block Additional configuration for the request
     * @return The response from the server
     * @throws ServerException If the server responds with an error
     */
    suspend fun get(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = request(HttpMethod.Get, path, null, block)

    /**
     * Send a GET request to the server and deserialize the response
     * @param path The path to send the request to
     * @param deserializer The deserializer to use for the response
     * @param block Additional configuration for the request
     * @return The deserialized response from the server
     * @throws ServerException If the server responds with an error
     * @throws SerializationException If the response cannot be deserialized
     */
    suspend fun <Type> get(
        path: String,
        deserializer: DeserializationStrategy<Type>,
        block: HttpRequestBuilder.() -> Unit = {}
    ): Type {
        val response = get(path, block)
        val body = response.bodyAsText()
        return serverJson.decodeFromString(deserializer, body)
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
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = request(HttpMethod.Post, path, basicAuth, block)

    /**
     * Send a POST request to the server
     * @param path The path to send the request to
     * @param block Additional configuration for the request
     * @param body The body to send with the request
     * @param bodySerializer The serializer to use for the body
     * @param basicAuth The basic authentication credentials to use
     * @return The response from the server
     * @throws ServerException If the server responds with an error
     */
    suspend fun <Type> post(
        path: String,
        body: Type,
        bodySerializer: SerializationStrategy<Type>,
        basicAuth: Pair<String, String>? = null,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = post(path, basicAuth) {
        val json = serverJson.encodeToString(bodySerializer, body)
        contentType(ContentType.Application.Json)
        setBody(json)

        block()
    }

    /**
     * Send a PATCH request to the server
     * @param path The path to send the request to
     * @param block Additional configuration for the request
     * @param body The body to send with the request
     * @param bodySerializer The serializer to use for the body
     * @param basicAuth The basic authentication credentials to use
     * @return The response from the server
     * @throws ServerException If the server responds with an error
     */
    suspend fun <Type> patch(
        path: String,
        body: Type,
        bodySerializer: SerializationStrategy<Type>,
        basicAuth: Pair<String, String>? = null,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = request(HttpMethod.Patch, path, basicAuth) {
        val json = serverJson.encodeToString(bodySerializer, body)
        contentType(ContentType.Application.Json)
        setBody(json)

        block()
    }

    /**
     * Send a DELETE request to the server
     * @param path The path to send the request to
     * @param block Additional configuration for the request
     * @return The response from the server
     * @throws ServerException If the server responds with an error
     */
    suspend fun delete(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = request(HttpMethod.Delete, path, null, block)

    /**
     * Makes a ping request to the server.
     * @return `true` if the server is reachable, `false` otherwise.
     */
    suspend fun ping(onError: (suspend (body: String?, error: Exception?) -> Unit)? = null): Boolean {
        try {
            val response = get("/ping")
            val body = response.bodyAsText()
            if (body == "pong") {
                return true
            } else {
                Napier.e { "Server answered unexpectedly. Body: $body" }
                onError?.invoke(body, null)
                return false
            }
        } catch (e: Exception) {
            Napier.e(e) { "Could not ping server." }
            onError?.invoke(null, e)
            return false
        }
    }
}
