package org.centrexcursionistalcoi.app.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import com.diamondedge.logging.logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.PointerEventFlow
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.koin.compose.koinInject

private val log = logging()

@Composable
@OptIn(ExperimentalComposeUiApi::class)
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    val dispatcherProvider = koinInject<DispatcherProvider>()
    DisposableEffect(enabled) {
        val job = if (enabled) {
            CoroutineScope(dispatcherProvider.main).launch {
                PointerEventFlow.flow.filterNotNull().filter { it.type == PointerEventType.Press }.collect { event ->
                    if (event.button?.index == 5 /* Back button */) {
                        log.d { "Back button pressed. Invoking onBack callback." }
                        onBack()
                    }
                }
            }
        } else {
            null
        }
        onDispose {
            job?.cancel()
        }
    }
}
