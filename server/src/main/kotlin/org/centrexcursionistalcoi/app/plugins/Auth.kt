package org.centrexcursionistalcoi.app.plugins

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.interfaces.Claim
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.origin
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.centrexcursionistalcoi.app.auth.TokenResponse
import org.centrexcursionistalcoi.app.json

const val AUTH_PROVIDER_NAME = "authentik-oauth"

// TODO: Remove credentials from here
const val OAUTH_CLIENT_ID = "ZvPaQu8nsU1fpaSkt3c4MPDFKue2RrpGrEdEbiTU"
const val OAUTH_CLIENT_SECRET =
    "pcG8cxsh80dN77jHTViyK6uanyEAtgOemtGgvWP8Jpva1PJqZGsLbGNp4d1tZPAzfWbgTcjnyS7BEqPeoftAgaQMO0ZDvFcYp8eMDxemVywVlLeDrbJEzWIYuGNUFjf0"

private const val ISSUER = "https://auth.cea.arnaumora.com/application/o/cea-app/"
private const val AUTH_ENDPOINT = "https://auth.cea.arnaumora.com/application/o/authorize/"
private const val TOKEN_ENDPOINT = "https://auth.cea.arnaumora.com/application/o/token/"
private const val USERINFO_ENDPOINT = "https://auth.cea.arnaumora.com/application/o/userinfo/"
private const val JWKS_ENDPOINT = "https://auth.cea.arnaumora.com/application/o/cea-app/jwks/"
private const val REDIRECT_URI = "http://localhost:8080/callback"

private val SCOPES = listOf("openid", "profile", "email", "groups").joinToString(" ")

val authHttpClient = HttpClient(Java)

// Simple in-memory store mapping state -> code_verifier (use Redis/DB for prod + TTL)
val pkceStore = ConcurrentHashMap<String, String>()

fun generateCodeVerifier(): String {
    // 32 random bytes -> base64url -> ~43 chars (within 43..128 required)
    val random = SecureRandom()
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

fun generateCodeChallenge(verifier: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(verifier.toByteArray(StandardCharsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
}

fun decodeJwtPayload(jwt: String): String {
    val parts = jwt.split(".")
    if (parts.size < 2) return "{}"
    val payload = parts[1]
    val decoded = Base64.getUrlDecoder().decode(payload)
    return String(decoded, StandardCharsets.UTF_8)
}

fun Application.configureAuth() {
    // Fetch JWKS from Authentik to verify tokens
    val jwkProvider = JwkProviderBuilder(URI(JWKS_ENDPOINT).toURL())
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
                            .withAudience(OAUTH_CLIENT_ID)
                            .withIssuer(ISSUER)
                            .build()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            validate { credential ->
                if (credential.payload.audience.contains(OAUTH_CLIENT_ID)) {
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
            .withAudience(OAUTH_CLIENT_ID)
            .withIssuer(ISSUER)
            .build()
            .verify(token)

        val sub: String? = decodedToken.getClaim("sub").asString() // Subject Identifier
        val username: String? = decodedToken.getClaim("preferred_username").asString()
        val email: String? = decodedToken.getClaim("email").asString()
        val groups = decodedToken.getClaim("groups")?.asList(String::class.java)

        if (sub != null && username != null && email != null && groups != null) {
            call.sessions.set(UserSession(sub, username, email, groups))

            val loginSession = call.sessions.get<LoginSession>()
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
            pkceStore[state] = codeVerifier
        }

        val authorizeUrl = URLBuilder(AUTH_ENDPOINT).apply {
            parameters.append("client_id", OAUTH_CLIENT_ID)
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
        val response = authHttpClient.submitForm(
            url = TOKEN_ENDPOINT,
            formParameters = Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", REDIRECT_URI)
                append("client_id", OAUTH_CLIENT_ID)
                // If your client is confidential (server-side), include secret:
                append("client_secret", OAUTH_CLIENT_SECRET)
                // PKCE verifier:
                append("code_verifier", codeVerifier)
            }
        )

        val bodyText = response.bodyAsText()
        val tokenJson = json.parseToJsonElement(bodyText).jsonObject

        val tokenResp: TokenResponse = json.decodeFromJsonElement(TokenResponse.serializer(), tokenJson)

        val idToken = tokenResp.id_token
        if (idToken == null) {
            call.respond(HttpStatusCode.InternalServerError, "No id_token returned")
            return@get
        }

        processJWT(jwkProvider, idToken)
    }
}
