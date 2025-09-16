package org.centrexcursionistalcoi.app.auth

import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsTokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.TokenRefreshHandler
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod

@OptIn(ExperimentalOpenIdConnect::class)
lateinit var tokenStore: SettingsTokenStore

@OptIn(ExperimentalOpenIdConnect::class)
val refreshHandler by lazy { TokenRefreshHandler(tokenStore = tokenStore) }

val oidcConnectClient = OpenIdConnectClient(
    discoveryUri = "https://auth.cea.arnaumora.com/application/o/cea-app/.well-known/openid-configuration"
) {
    clientId = "ZvPaQu8nsU1fpaSkt3c4MPDFKue2RrpGrEdEbiTU"
    clientSecret = "pcG8cxsh80dN77jHTViyK6uanyEAtgOemtGgvWP8Jpva1PJqZGsLbGNp4d1tZPAzfWbgTcjnyS7BEqPeoftAgaQMO0ZDvFcYp8eMDxemVywVlLeDrbJEzWIYuGNUFjf0"
    scope = "openid profile"
    codeChallengeMethod = CodeChallengeMethod.S256
    redirectUri = "cea://redirect"
    postLogoutRedirectUri = "cea://postLogout"
}
