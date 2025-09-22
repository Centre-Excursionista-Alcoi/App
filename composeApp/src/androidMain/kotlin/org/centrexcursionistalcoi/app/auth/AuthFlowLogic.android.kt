package org.centrexcursionistalcoi.app.auth

import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlin.uuid.ExperimentalUuidApi
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.MainActivity

@OptIn(ExperimentalUuidApi::class)
actual object AuthFlowLogic {
    actual fun start() {
        val activity = MainActivity.instance ?: return

        val (state, codeChallenge) = generateAndStorePCKE()

        val intent = CustomTabsIntent.Builder()
            .build()
        intent.launchUrl(
            activity,
            URLBuilder(BuildKonfig.SERVER_URL)
                .appendPathSegments("login")
                .apply {
                    parameters["redirect_uri"] = "cea://redirect"
                    parameters["state"] = state.toString()
                    parameters["code_challenge"] = codeChallenge
                }
                .buildString()
                .also { Napier.i { "Launching: $it" } }
                .toUri()
        )
    }
}
