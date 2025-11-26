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
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.log.initializeSentry
import org.centrexcursionistalcoi.app.platform.PlatformAppUpdates.checkForUpdates
import org.jetbrains.compose.resources.painterResource

private val log = logging()

object PointerEventFlow {
    private val mutableFlow = MutableStateFlow<PointerEvent?>(null)
    val flow = mutableFlow.asStateFlow()

    fun tryEmit(event: PointerEvent) = mutableFlow.tryEmit(event)
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
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
                        log.d { "Listening for pointer events..." }
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
