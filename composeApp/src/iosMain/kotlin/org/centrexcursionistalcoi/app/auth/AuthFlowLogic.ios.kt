package org.centrexcursionistalcoi.app.auth

import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.doMain
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.navController
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject

class AuthPresentationContextProvider :  NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor {
        return UIApplication.sharedApplication.keyWindow
            ?: UIApplication.sharedApplication.windows.first() as? UIWindow
            ?: ASPresentationAnchor()
    }
}

actual object AuthFlowLogic {
    actual fun start() {
        val (state, codeChallenge) = generateAndStorePCKE()

        val url = URLBuilder(BuildKonfig.SERVER_URL)
            .appendPathSegments("login")
            .apply {
                parameters["redirect_uri"] = "cea://redirect"
                parameters["state"] = state.toString()
                parameters["code_challenge"] = codeChallenge
            }
            .buildString()

        val session = ASWebAuthenticationSession(
            uRL = NSURL(string = url),
            callbackURLScheme = "cea",
        ) { callbackUrl: NSURL?, error: NSError? ->
            if (error != null) {
                Napier.e { "Authentication error: $error" }
                throw IllegalStateException("Authentication error: $error")
            } else if (callbackUrl != null) {
                val url = callbackUrl.toString()
                Napier.i { "Authentication complete. Url: $url" }
                CoroutineScope(defaultAsyncDispatcher).launch {
                    AuthCallbackProcessor.processCallbackUrl(
                        Url(url)
                    )

                    doMain {
                        navController.navigate(Destination.Loading) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    }
                }
            } else {
                throw IllegalArgumentException("Authentication error: no callback url or error")
            }
        }
        session.presentationContextProvider = AuthPresentationContextProvider()
        session.start()
    }
}
