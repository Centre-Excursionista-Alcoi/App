package org.centrexcursionistalcoi.app

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.platform.PlatformAppUpdates.checkForUpdates
import org.jetbrains.compose.resources.painterResource

object PointerEventFlow {
    private val mutableFlow = MutableStateFlow<PointerEvent?>(null)
    val flow = mutableFlow.asStateFlow()

    fun tryEmit(event: PointerEvent) = mutableFlow.tryEmit(event)
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize the logging library
    Napier.base(
        object : Antilog() {
            override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
                val out = if (priority == LogLevel.ERROR) System.err else System.out
                out.println("[$priority] ${tag.orEmpty()}: ${message.orEmpty()}")
                if (throwable != null) {
                    throwable.printStackTrace()
                    Sentry.captureException(throwable)
                }
            }
        }
    )

    initializeSentry()

    checkForUpdates()

    application {
        Window(
            title = "Centre Excursionista d'Alcoi",
            icon = painterResource(Res.drawable.icon),
            state = rememberWindowState(
                size = DpSize(1000.dp, 800.dp),
            ),
            onCloseRequest = ::exitApplication,
        ) {
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        Napier.d { "Listening for pointer events..." }
                        while (true) {
                            val event = awaitPointerEvent()
                            PointerEventFlow.tryEmit(event)
                        }
                    }
                }
            ) {
                MainApp()
            }
        }
    }
}
