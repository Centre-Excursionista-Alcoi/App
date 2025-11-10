package org.centrexcursionistalcoi.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cea_app.composeapp.generated.resources.*
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.sentry.kotlin.multiplatform.Sentry
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize the logging library
    Napier.base(
        object : Antilog() {
            override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
                val out = if (priority == LogLevel.ERROR) System.err else System.out
                out.println("[$priority] ${tag.orEmpty()}: ${message.orEmpty()}")
                if (message != null) {
                    Sentry.captureMessage(message)
                }
                if (throwable != null) {
                    throwable.printStackTrace()
                    Sentry.captureException(throwable)
                }
            }
        }
    )

    initializeSentry()

    application {
        Window(
            title = "Centre Excursionista d'Alcoi",
            icon = painterResource(Res.drawable.icon),
            state = rememberWindowState(
                size = DpSize(1000.dp, 800.dp),
            ),
            onCloseRequest = ::exitApplication,
        ) {
            MainApp()
        }
    }
}
