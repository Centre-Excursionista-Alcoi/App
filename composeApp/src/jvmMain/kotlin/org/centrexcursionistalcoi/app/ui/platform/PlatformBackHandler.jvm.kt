package org.centrexcursionistalcoi.app.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.PointerEventFlow

@Composable
@OptIn(ExperimentalComposeUiApi::class)
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    DisposableEffect(enabled) {
        val job = if (enabled) {
            CoroutineScope(Dispatchers.Main).launch {
                PointerEventFlow.flow.filterNotNull().filter { it.type == PointerEventType.Press }.collect { event ->
                    if (event.button?.index == 5 /* Back button */) {
                        Napier.d { "Back button pressed. Invoking onBack callback." }
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
