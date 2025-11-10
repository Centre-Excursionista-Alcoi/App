package org.centrexcursionistalcoi.app.ui.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.pointerHoverEvents(
    onMove: (Offset) -> Unit,
    onEnter: (Offset) -> Unit,
    onExit: () -> Unit
): Modifier =
    onPointerEvent(PointerEventType.Move) {
        onMove(it.changes.first().position)
    }
        .onPointerEvent(PointerEventType.Enter) {
            onEnter(it.changes.first().position)
        }
        .onPointerEvent(PointerEventType.Exit) {
            onExit()
        }
