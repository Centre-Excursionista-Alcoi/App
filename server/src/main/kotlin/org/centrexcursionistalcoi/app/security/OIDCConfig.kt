package org.centrexcursionistalcoi.app.security

/*const val OAUTH_CLIENT_ID = "ZvPaQu8nsU1fpaSkt3c4MPDFKue2RrpGrEdEbiTU"
const val OAUTH_CLIENT_SECRET =
    "pcG8cxsh80dN77jHTViyK6uanyEAtgOemtGgvWP8Jpva1PJqZGsLbGNp4d1tZPAzfWbgTcjnyS7BEqPeoftAgaQMO0ZDvFcYp8eMDxemVywVlLeDrbJEzWIYuGNUFjf0"

private const val ISSUER = "https://auth.cea.arnaumora.com/application/o/cea-app/"
private const val AUTH_ENDPOINT = "https://auth.cea.arnaumora.com/application/o/authorize/"
private const val TOKEN_ENDPOINT = "https://auth.cea.arnaumora.com/application/o/token/"
private const val USERINFO_ENDPOINT = "https://auth.cea.arnaumora.com/application/o/userinfo/"
private const val JWKS_ENDPOINT = "https://auth.cea.arnaumora.com/application/o/cea-app/jwks/"
private const val REDIRECT_URI = "http://localhost:8080/callback"*/

object OIDCConfig {
    val clientId get() = System.getenv("OAUTH_CLIENT_ID") ?: error("OAUTH_CLIENT_ID env var not set")
    val clientSecret get() = System.getenv("OAUTH_CLIENT_SECRET") ?: error("OAUTH_CLIENT_SECRET env var not set")

    val issuer get() = System.getenv("OAUTH_ISSUER") ?: error("OAUTH_ISSUER env var not set")
    val authEndpoint get() = System.getenv("OAUTH_AUTH_ENDPOINT") ?: error("OAUTH_AUTH_ENDPOINT env var not set")
    val tokenEndpoint get() = System.getenv("OAUTH_TOKEN_ENDPOINT") ?: error("OAUTH_TOKEN_ENDPOINT env var not set")
    val userInfoEndpoint get() = System.getenv("OAUTH_USERINFO_ENDPOINT") ?: error("OAUTH_USERINFO_ENDPOINT env var not set")
    val jwksEndpoint get() = System.getenv("OAUTH_JWKS_ENDPOINT") ?: error("OAUTH_JWKS_ENDPOINT env var not set")
    val redirectUri get() = System.getenv("OAUTH_REDIRECT_URI") ?: error("OAUTH_REDIRECT_URI env var not set")
}
