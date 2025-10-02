package org.centrexcursionistalcoi.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import kotlinx.browser.document
import kotlinx.browser.window
import org.centrexcursionistalcoi.app.auth.redirectOrigin
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.centrexcursionistalcoi.app.viewmodel.AuthCallbackModel

@OptIn(ExperimentalComposeUiApi::class, ExperimentalBrowserHistoryApi::class)
fun main() {
    Napier.base(DebugAntilog())

    secretsBinaryLoaded = true

    redirectOrigin = window.location.origin

    ComposeViewport(document.body!!) {
        val currentPath = window.location.hash.removePrefix("#")
        Napier.d { "Current hash path: $currentPath" }

        when {
            currentPath == "redirect" -> {
                val model = viewModel { AuthCallbackModel() }
                val error by model.error.collectAsState()

                LaunchedEffect(Unit) {
                    Napier.i { "Handling redirection..." }
                    val url = window.location.href.let { Url(it) }
                    model.processCallbackUrl(url) {
                        // Redirect back to root
                        window.location.href = "/"
                    }
                }

                AppTheme {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            else -> {
                MainApp(
                    onNavHostReady = { it.bindToBrowserNavigation() }
                )
            }
        }
    }
}
