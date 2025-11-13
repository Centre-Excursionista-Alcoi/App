package org.centrexcursionistalcoi.app.ui.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

expect fun Modifier.pointerHoverEvents(
    onMove: (Offset) -> Unit,
    onEnter: (Offset) -> Unit,
    onExit: () -> Unit
): Modifier
