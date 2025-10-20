package org.centrexcursionistalcoi.app.security

import org.centrexcursionistalcoi.app.ConfigProvider

object OIDCConfig : ConfigProvider() {
    val clientId get() = getenv("OAUTH_CLIENT_ID") ?: error("OAUTH_CLIENT_ID env var not set")
    val clientSecret get() = getenv("OAUTH_CLIENT_SECRET") ?: error("OAUTH_CLIENT_SECRET env var not set")

    val authentikBase get() = getenv("OAUTH_AUTHENTIK_BASE") ?: error("OAUTH_AUTHENTIK_BASE env var not set")
    val authentikToken get() = getenv("OAUTH_AUTHENTIK_TOKEN")

    val issuer get() = getenv("OAUTH_ISSUER") ?: error("OAUTH_ISSUER env var not set")
    val authEndpoint get() = getenv("OAUTH_AUTH_ENDPOINT") ?: error("OAUTH_AUTH_ENDPOINT env var not set")
    val tokenEndpoint get() = getenv("OAUTH_TOKEN_ENDPOINT") ?: error("OAUTH_TOKEN_ENDPOINT env var not set")
    val userInfoEndpoint get() = getenv("OAUTH_USERINFO_ENDPOINT") ?: error("OAUTH_USERINFO_ENDPOINT env var not set")
    val jwksEndpoint get() = getenv("OAUTH_JWKS_ENDPOINT") ?: error("OAUTH_JWKS_ENDPOINT env var not set")
    val redirectUri get() = getenv("OAUTH_REDIRECT_URI") ?: error("OAUTH_REDIRECT_URI env var not set")
}
