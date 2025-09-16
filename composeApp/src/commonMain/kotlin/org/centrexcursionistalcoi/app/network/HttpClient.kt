package org.centrexcursionistalcoi.app.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.cookies.HttpCookies
import org.centrexcursionistalcoi.app.auth.oidcConnectClient
import org.centrexcursionistalcoi.app.auth.refreshHandler
import org.centrexcursionistalcoi.app.auth.tokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.ktor.oidcBearer

@OptIn(ExperimentalOpenIdConnect::class)
val httpClient: HttpClient by lazy {
    HttpClient {
        install(Auth) {
            oidcBearer(
                tokenStore = tokenStore,
                refreshHandler = refreshHandler,
                client = oidcConnectClient,
            )
        }
        install(HttpCookies)
    }
}
