package org.centrexcursionistalcoi.app.auth

import io.github.aakira.napier.Napier
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import org.centrexcursionistalcoi.app.network.configureLogging
import org.publicvalue.multiplatform.oidc.DefaultOpenIdConnectClient
import org.publicvalue.multiplatform.oidc.DefaultOpenIdConnectClient.Companion.DefaultHttpClient
import org.publicvalue.multiplatform.oidc.Endpoints
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.OpenIdConnectClientConfig
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsTokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.TokenRefreshHandler
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod

@OptIn(ExperimentalOpenIdConnect::class)
lateinit var tokenStore: SettingsTokenStore

@OptIn(ExperimentalOpenIdConnect::class)
val refreshHandler by lazy { TokenRefreshHandler(tokenStore = tokenStore) }

val defaultEndpoints = Endpoints(
    tokenEndpoint = "https://auth.cea.arnaumora.com/application/o/token/",
    authorizationEndpoint = "https://auth.cea.arnaumora.com/application/o/authorize/",
    userInfoEndpoint = "https://auth.cea.arnaumora.com/application/o/userinfo/",
    endSessionEndpoint = "https://auth.cea.arnaumora.com/application/o/cea-app/end-session/",
    revocationEndpoint = "https://auth.cea.arnaumora.com/application/o/revoke/",
)

fun createOidcConnectClient(includeDiscoveryUri: Boolean = true) = DefaultOpenIdConnectClient(
    httpClient = DefaultHttpClient.config {
        install(HttpCookies)
        configureLogging()
    },
    OpenIdConnectClientConfig(
        discoveryUri = "https://auth.cea.arnaumora.com/application/o/cea-app/.well-known/openid-configuration".takeIf { includeDiscoveryUri },
        endpoints = defaultEndpoints,
        clientId = "ZvPaQu8nsU1fpaSkt3c4MPDFKue2RrpGrEdEbiTU",
        clientSecret = "pcG8cxsh80dN77jHTViyK6uanyEAtgOemtGgvWP8Jpva1PJqZGsLbGNp4d1tZPAzfWbgTcjnyS7BEqPeoftAgaQMO0ZDvFcYp8eMDxemVywVlLeDrbJEzWIYuGNUFjf0",
        scope = "openid profile",
        codeChallengeMethod = CodeChallengeMethod.S256,
        redirectUri = redirectUri.also { Napier.i { "Redirect URI: $it" } },
        postLogoutRedirectUri = postLogoutRedirectUri,
    )
)

private var oidcConnectClient: OpenIdConnectClient? = null
fun getOidcConnectClient(): OpenIdConnectClient {
    return oidcConnectClient ?: createOidcConnectClient().also { oidcConnectClient = it }
}
fun setOidcConnectClient(client: OpenIdConnectClient) {
    oidcConnectClient = client
}
