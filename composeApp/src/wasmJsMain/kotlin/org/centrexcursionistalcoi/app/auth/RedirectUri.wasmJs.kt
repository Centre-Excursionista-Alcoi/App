package org.centrexcursionistalcoi.app.auth

import io.ktor.http.URLBuilder

var redirectOrigin: String = "http://localhost:8080"

actual val redirectUri: String
    get() = URLBuilder(redirectOrigin)
        .apply { fragment = "redirect" }
        .buildString()

actual val postLogoutRedirectUri: String
    get() = URLBuilder(redirectOrigin)
        .apply { fragment = "postLogout" }
        .buildString()
