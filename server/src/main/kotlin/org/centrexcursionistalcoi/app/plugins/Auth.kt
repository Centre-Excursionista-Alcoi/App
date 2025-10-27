package org.centrexcursionistalcoi.app.plugins

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.interfaces.Claim
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.centrexcursionistalcoi.app.auth.TokenResponse
import org.centrexcursionistalcoi.app.auth.generateCodeChallenge
import org.centrexcursionistalcoi.app.auth.generateCodeVerifier
import org.centrexcursionistalcoi.app.authentik.AuthentikUser
import org.centrexcursionistalcoi.app.authentik.errors.AuthentikError
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.routes.assertContentType
import org.centrexcursionistalcoi.app.security.OIDCConfig
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.storage.InMemoryStoreMap
import org.centrexcursionistalcoi.app.storage.RedisStoreMap

const val AUTH_PROVIDER_NAME = "oidc"

private val SCOPES = listOf("openid", "profile", "email", "groups").joinToString(" ")

private val httpClient = HttpClient(Java) {
    install(ContentNegotiation) {
        json(json)
    }
    install(Logging) {
        level = LogLevel.HEADERS
    }
}

fun getAuthHttpClient(): HttpClient = httpClient

// Simple in-memory store mapping state -> code_verifier (use Redis/DB for prod + TTL)
val pkceStore = RedisStoreMap.fromEnvOrNull() ?: InMemoryStoreMap()

private val emailRegex = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$".toRegex()

fun Application.configureAuth() {
    // Fetch JWKS from Authentik to verify tokens
    val jwkProvider = JwkProviderBuilder(URI(OIDCConfig.jwksEndpoint).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .build()

    install(Authentication) {
        jwt(AUTH_PROVIDER_NAME) {
            realm = "ktor-authentik"
            verifier { authHeader ->
                if (authHeader !is HttpAuthHeader.Single || authHeader.authScheme != "Bearer") {
                    null
                } else {
                    try {
                        // Parse token to extract "kid" from header
                        val token = authHeader.blob
                        val decodedHeader = JWT.decode(token)
                        val kid = decodedHeader.getHeaderClaim("kid").asString() ?: return@verifier null

                        // Get the JWK and build verifier
                        val jwk = jwkProvider[kid]
                        JWT.require(Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null))
                            .withAudience(OIDCConfig.clientId)
                            .withIssuer(OIDCConfig.issuer)
                            .build()
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            validate { credential ->
                if (credential.payload.audience.contains(OIDCConfig.clientId)) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }

    routing {
        configureAuthRoutes(jwkProvider)
    }
}

fun Claim.asDisplayString(): String {
    if (isNull || isMissing) return "null"
    asString()?.let { return it }
    asInt()?.let { return it.toString() }
    asBoolean()?.let { return it.toString() }
    asDouble()?.let { return it.toString() }
    asMap()?.let { return it.toString() }
    asList(Any::class.java)?.let { return "[${it.joinToString()}]" }
    return "N/A"
}

private suspend fun RoutingContext.processJWT(jwkProvider: JwkProvider, token: String) {
    try {
        val kid = JWT.decode(token).keyId
        val jwk = jwkProvider.get(kid)
        val decodedToken = JWT.require(Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null))
            .withAudience(OIDCConfig.clientId)
            .withIssuer(OIDCConfig.issuer)
            .build()
            .verify(token)

        val sub: String? = decodedToken.getClaim("sub").asString() // Subject Identifier
        val pk: Int? = decodedToken.getClaim("pk").asInt()
        val username: String? = decodedToken.getClaim("preferred_username").asString()
        val email: String? = decodedToken.getClaim("email").asString()
        val groups = decodedToken.getClaim("groups")?.asList(String::class.java)

        if (sub != null && pk != null && username != null && email != null && groups != null) {
            val session = UserSession(sub, pk, username, email, groups)
            call.sessions.set(session)

            Database { UserReferenceEntity.getOrProvide(session) }

            val loginSession = call.sessions.get<LoginSession>()
            call.sessions.clear<LoginSession>()

            val redirectUrl = loginSession?.redirectUrl
            if (redirectUrl != null)
                call.respondRedirect(redirectUrl)
            else
                call.respondText("OK")
        } else {
            call.response.header(
                "X-Debug-JWT-Claims",
                decodedToken.claims
                    .mapValues { (_, claim) -> claim.asDisplayString() }
                    .toList()
                    .joinToString(", ") { (key, claim) -> "$key=$claim" }
            )
            call.respond(HttpStatusCode.NotAcceptable, "Missing required user info in token")
        }
    } catch (e: JWTDecodeException) {
        call.respond(HttpStatusCode.BadRequest, "JWT token is not valid: ${e.message}")
    } catch (e: Exception) {
        e.printStackTrace()
        call.respond(HttpStatusCode.Unauthorized, "Invalid id_token: ${e.message}")
    }
}

fun Route.configureAuthRoutes(jwkProvider: JwkProvider) {
    // Step 1: Start login - redirect to Authentik authorize endpoint with PKCE + state
    get("/login") {
        val bearerToken = call.request.parseAuthorizationHeader()
            ?.takeIf { it.authScheme.equals("Bearer", true) }
            ?.render()
            ?.removePrefix("Bearer")
            ?.trim()
        if (bearerToken != null) {
            return@get processJWT(jwkProvider, bearerToken)
        }

        val query = call.request.queryParameters

        // Redirection address, processed by the server. Will pass a Cookie session.
        val redirectTo = query["redirect_to"]
        call.sessions.set(LoginSession(redirectTo))

        // Redirect URI to pass to Authentik (must match one of the allowed URIs in the client config)
        val redirectUri = query["redirect_uri"] ?: run {
            val origin = call.request.origin
            URLBuilder(origin.scheme + "://" + origin.serverHost + ":" + origin.serverPort)
                .appendPathSegments("callback")
                .buildString()
        }

        var state = query["state"]
        var codeChallenge = query["code_challenge"]
        if (state == null || codeChallenge == null) {
            state = UUID.randomUUID().toString()
            val codeVerifier = generateCodeVerifier()
            codeChallenge = generateCodeChallenge(codeVerifier)

            // Store verifier server-side mapped by "state"
            pkceStore.put(state, codeVerifier)
        }

        val authorizeUrl = URLBuilder(OIDCConfig.authEndpoint).apply {
            parameters.append("client_id", OIDCConfig.clientId)
            parameters.append("response_type", "code")
            parameters.append("scope", SCOPES)
            parameters.append("redirect_uri", redirectUri)
            parameters.append("state", state)
            parameters.append("code_challenge", codeChallenge)
            parameters.append("code_challenge_method", "S256")
        }.buildString()

        call.respondRedirect(authorizeUrl)
    }

    // Step 2: Callback - Authentik redirected back here with ?code&state
    get("/callback") {
        val query = call.request.queryParameters
        val code = query["code"]
        val redirectUri = query["redirect_uri"] ?: OIDCConfig.redirectUri

        if (code.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing code")
            return@get
        }

        // Retrieve and remove the stored code_verifier
        val codeVerifier = query["code_verifier"] ?: run {
            val state = query["state"]
            if (state.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing state")
                return@get
            }
            pkceStore.remove(state)
        }
        if (codeVerifier == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired state")
            return@get
        }

        // Exchange code for tokens at the token endpoint
        val response = getAuthHttpClient().submitForm(
            url = OIDCConfig.tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("client_id", OIDCConfig.clientId)
                // If your client is confidential (server-side), include secret:
                append("client_secret", OIDCConfig.clientSecret)
                // PKCE verifier:
                append("code_verifier", codeVerifier)
            }
        )

        val bodyText = response.bodyAsText()
        val tokenJson = json.parseToJsonElement(bodyText).jsonObject

        val tokenResp: TokenResponse = json.decodeFromJsonElement(TokenResponse.serializer(), tokenJson)

        val idToken = tokenResp.id_token
        if (idToken == null) {
            call.response.header("X-Debug-Token-Response", bodyText)
            call.response.header("X-Debug-RedirectUri", redirectUri)
            call.respond(HttpStatusCode.InternalServerError, "No id_token returned")
            return@get
        }

        processJWT(jwkProvider, idToken)
    }

    // Allow user registration
    post("/register") {
        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val authentikToken = OIDCConfig.authentikToken
        if (authentikToken == null) {
            respondError(Error.AuthentikNotConfigured())
            return@post
        }

        val parameters = call.receiveParameters()
        val username = parameters["username"]?.trim()
        val name = parameters["name"]?.trim()
        val email = parameters["email"]?.trim()
        val password = parameters["password"]?.trim()

        if (username == null) return@post call.respondText("Missing username", status = HttpStatusCode.BadRequest)
        if (name == null) return@post call.respondText("Missing name", status = HttpStatusCode.BadRequest)
        if (email == null) return@post call.respondText("Missing email", status = HttpStatusCode.BadRequest)
        if (password == null) return@post call.respondText("Missing password", status = HttpStatusCode.BadRequest)

        // validate email
        if (!emailRegex.matches(email)) {
            call.respondText("Invalid email format", status = HttpStatusCode.BadRequest)
            return@post
        }

        // validate username: length 3-30, only alphanumeric, underscores, hyphens
        if (username.length !in 3..30 || !username.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
            call.respondText("Invalid username format", status = HttpStatusCode.BadRequest)
            return@post
        }

        // validate name: length 1-100
        if (name.length !in 1..100) {
            call.respondText("Invalid name format", status = HttpStatusCode.BadRequest)
            return@post
        }

        // validate password
        val hasLowerCase = password.find { it.isLowerCase() } != null
        val hasUpperCase = password.find { it.isUpperCase() } != null
        val hasNumber = password.find { it.isDigit() } != null
        val hasMinLength = password.length >= 8
        if (!hasLowerCase || !hasUpperCase || !hasNumber || !hasMinLength) {
            call.respondText("Password must be at least 8 characters long and contain at least one letter and one number", status = HttpStatusCode.BadRequest)
            return@post
        }

        // Create user in Authentik via API
        val createUserUrl = URLBuilder(OIDCConfig.authentikBase)
            .appendPathSegments("/api/v3/core/users/")
            .build()
        val response = getAuthHttpClient().post(createUserUrl) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(authentikToken)
            setBody(
                JsonObject(
                    mapOf(
                        "username" to JsonPrimitive(username),
                        "name" to JsonPrimitive(name),
                        "email" to JsonPrimitive(email),
                        "is_active" to JsonPrimitive(true),
                        "path" to JsonPrimitive("users"),
                        "type" to JsonPrimitive("internal"),
                    )
                )
            )
        }
        if (!response.status.isSuccess()) {
            val error = response.bodyAsJson(AuthentikError.serializer())
            throw error.asThrowable()
        }
        val user = response.bodyAsJson(AuthentikUser.serializer())

        // Set the password for the created user
        val setPasswordUrl = URLBuilder(OIDCConfig.authentikBase)
            .appendPathSegments("/api/v3/core/users", user.pk.toString(), "set_password/")
            .build()
        val pwdResponse = getAuthHttpClient().post(setPasswordUrl) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(authentikToken)
            setBody(
                JsonObject(
                    mapOf(
                        "password" to JsonPrimitive(password)
                    )
                )
            )
        }
        if (!pwdResponse.status.isSuccess()) {
            val error = pwdResponse.bodyAsJson(AuthentikError.serializer())
            throw error.asThrowable()
        }

        // Success, respond accordingly
        call.respondText("User '$username' created successfully", status = HttpStatusCode.Created)
    }
}
