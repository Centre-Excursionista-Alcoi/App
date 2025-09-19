package org.centrexcursionistalcoi.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.browser.document
import kotlinx.browser.window
import org.centrexcursionistalcoi.app.auth.redirectOrigin
import org.centrexcursionistalcoi.app.auth.tokenStore
import org.centrexcursionistalcoi.app.storage.WasmSettingsTokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.PlatformCodeAuthFlow
import org.publicvalue.multiplatform.oidc.appsupport.WasmCodeAuthFlowFactory

@OptIn(ExperimentalComposeUiApi::class, ExperimentalBrowserHistoryApi::class, ExperimentalOpenIdConnect::class)
fun main() {
    Napier.base(DebugAntilog())

    secretsBinaryLoaded = true

    redirectOrigin = window.location.origin

    tokenStore = WasmSettingsTokenStore()

    val authFlowFactory = WasmCodeAuthFlowFactory()

    ComposeViewport(document.body!!) {
        val currentPath = window.location.hash.removePrefix("#")
        Napier.d { "Current hash path: $currentPath" }

        when {
            currentPath == "redirect" -> {
                LaunchedEffect(Unit) {
                    Napier.i { "Handling redirection..." }
                    PlatformCodeAuthFlow.handleRedirect()
                }
            }
            else -> {
                MainApp(
                    authFlowFactory = authFlowFactory,
                    onNavHostReady = { it.bindToBrowserNavigation() }
                )
            }
        }
    }
}
