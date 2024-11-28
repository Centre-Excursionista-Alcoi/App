package org.centrexcursionistalcoi.app

import androidx.compose.ui.window.ComposeUIViewController
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

fun initialize() {
    Napier.base(DebugAntilog())
}

fun MainViewController() = ComposeUIViewController {
    AppRoot()
}
