package org.centrexcursionistalcoi.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

fun main() {
    Napier.base(DebugAntilog())

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "CEA App",
        ) {
            AppRoot()
        }
    }
}