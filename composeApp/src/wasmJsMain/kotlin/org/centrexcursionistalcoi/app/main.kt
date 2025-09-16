package org.centrexcursionistalcoi.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import kotlinx.browser.document
import kotlinx.browser.window
import org.centrexcursionistalcoi.app.auth.tokenStore
import org.centrexcursionistalcoi.app.storage.WasmSettingsTokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.PlatformCodeAuthFlow
import org.publicvalue.multiplatform.oidc.appsupport.WasmCodeAuthFlowFactory

@OptIn(ExperimentalComposeUiApi::class, ExperimentalBrowserHistoryApi::class, ExperimentalOpenIdConnect::class)
fun main() {
    tokenStore = WasmSettingsTokenStore()

    val authFlowFactory = WasmCodeAuthFlowFactory()

    ComposeViewport(document.body!!) {
        val currentPath = window.location.pathname

        when {
            currentPath.isBlank() || currentPath == "/" -> {
                App(
                    authFlowFactory = authFlowFactory,
                    onNavHostReady = { it.bindToBrowserNavigation() }
                )
            }
            currentPath.startsWith("/redirect") -> {
                LaunchedEffect(Unit) {
                    PlatformCodeAuthFlow.handleRedirect()
                }
            }
        }
    }
}
