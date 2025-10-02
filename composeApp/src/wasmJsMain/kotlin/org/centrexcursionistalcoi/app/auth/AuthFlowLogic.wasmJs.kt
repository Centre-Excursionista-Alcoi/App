package org.centrexcursionistalcoi.app.auth

import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.browser.window
import org.centrexcursionistalcoi.app.BuildKonfig

actual object AuthFlowLogic {
    actual fun start() {
        val (state, codeChallenge) = generateAndStorePCKE()
        val url = URLBuilder(BuildKonfig.SERVER_URL)
            .appendPathSegments("login")
            .apply {
                parameters["redirect_uri"] = BuildKonfig.REDIRECT_URI ?: error("REDIRECT_URI is not set")
                parameters["state"] = state.toString()
                parameters["code_challenge"] = codeChallenge
            }
            .buildString()

        Napier.i { "Navigating to: $url" }
        window.location.href = url
    }
}
