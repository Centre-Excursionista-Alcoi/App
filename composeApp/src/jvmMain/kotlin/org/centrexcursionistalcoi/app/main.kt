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
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.icon
import com.diamondedge.logging.FixedLogLevel
import com.diamondedge.logging.KmLogging
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import org.centrexcursionistalcoi.app.di.initKoin
import org.centrexcursionistalcoi.app.log.FileLogger
import org.centrexcursionistalcoi.app.log.initializeSentry
import org.centrexcursionistalcoi.app.platform.PlatformAppUpdates.checkForUpdates
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.centrexcursionistalcoi.app.settings.applySavedLanguage
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform

private val log = logging()

object PointerEventFlow {
    private val mutableFlow = MutableStateFlow<PointerEvent?>(null)
    val flow = mutableFlow.asStateFlow()

    fun tryEmit(event: PointerEvent) = mutableFlow.tryEmit(event)
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    KmLogging.setLoggers(
        JvmLogger(FixedLogLevel(true)),
        FileLogger(Path("app.log")),
    )

    initKoin()

    initializeSentry()

    checkForUpdates()

    // Localina creates its locale state lazily. If the first access came from the background thread that restores the
    // saved language, the state would be created after the composition took its snapshot, and reading it would crash.
    // The saved language is applied now, before composing: changing it afterwards rebuilds the whole UI.
    LocaleUpdater.hashCode()
    runBlocking { KoinPlatform.getKoin().get<SettingsStore>().applySavedLanguage() }

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
