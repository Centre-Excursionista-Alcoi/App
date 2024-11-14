package org.centrexcursionistalcoi.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ceaapp.composeapp.generated.resources.CEA
import ceaapp.composeapp.generated.resources.Res
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.painterResource

fun main() {
    Napier.base(DebugAntilog())

    application {
        Window(
            onCloseRequest = ::exitApplication,
            icon = painterResource(Res.drawable.CEA),
            title = "CEA App",
        ) {
            AppRoot()
        }
    }
}