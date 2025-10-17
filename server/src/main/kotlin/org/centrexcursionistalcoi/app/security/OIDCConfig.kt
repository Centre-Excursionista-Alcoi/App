package org.centrexcursionistalcoi.app.security

import org.jetbrains.annotations.VisibleForTesting

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
    private val override = mutableMapOf<String, String>()

    @VisibleForTesting
    fun override(name: String, value: String?) {
        if (value == null) {
            override.remove(name)
        } else {
            override[name] = value
        }
    }

    private fun getenv(name: String): String? = override[name] ?: System.getenv(name)?.ifBlank { null }

    val clientId get() = getenv("OAUTH_CLIENT_ID") ?: error("OAUTH_CLIENT_ID env var not set")
    val clientSecret get() = getenv("OAUTH_CLIENT_SECRET") ?: error("OAUTH_CLIENT_SECRET env var not set")

    val issuer get() = getenv("OAUTH_ISSUER") ?: error("OAUTH_ISSUER env var not set")
    val authEndpoint get() = getenv("OAUTH_AUTH_ENDPOINT") ?: error("OAUTH_AUTH_ENDPOINT env var not set")
    val tokenEndpoint get() = getenv("OAUTH_TOKEN_ENDPOINT") ?: error("OAUTH_TOKEN_ENDPOINT env var not set")
    val userInfoEndpoint get() = getenv("OAUTH_USERINFO_ENDPOINT") ?: error("OAUTH_USERINFO_ENDPOINT env var not set")
    val jwksEndpoint get() = getenv("OAUTH_JWKS_ENDPOINT") ?: error("OAUTH_JWKS_ENDPOINT env var not set")
    val redirectUri get() = getenv("OAUTH_REDIRECT_URI") ?: error("OAUTH_REDIRECT_URI env var not set")
}
