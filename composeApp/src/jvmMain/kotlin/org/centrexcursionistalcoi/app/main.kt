package org.centrexcursionistalcoi.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cea_app.composeapp.generated.resources.*
import dev.datlag.kcef.KCEF
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.auth.AuthFlowWindow
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize the logging library
    Napier.base(DebugAntilog())

    initializeSentry()

    application {
        AuthFlowWindow()

        Window(
            title = "Centre Excursionista d'Alcoi",
            icon = painterResource(Res.drawable.icon),
            state = rememberWindowState(
                size = DpSize(1000.dp, 800.dp),
            ),
            onCloseRequest = {
                KCEF.disposeBlocking()
                exitApplication()
            },
        ) {
            MainApp()
        }
    }
}
