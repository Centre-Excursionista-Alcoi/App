package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlin.math.roundToInt
import org.centrexcursionistalcoi.app.ui.platform.pointerHoverEvents

@Composable
fun <ID : Any> InteractiveCanvas(
    modifier: Modifier = Modifier,
    onClick: ((ID) -> Unit)? = null,
    tooltipContent: @Composable (ID) -> String?,
    hoverBounds: Map<ID, Rect>,
    onDraw: DrawScope.() -> Unit,
) {
    // pointer position (used for tooltip position)
    var mousePos by remember { mutableStateOf<Offset?>(null) }

    // Compute hoveredId from bounds + mousePos (derived state avoids extra writes)
    val hoveredId by remember(hoverBounds, mousePos) {
        derivedStateOf {
            mousePos?.let { mousePos -> hoverBounds.entries.find { it.value.contains(mousePos) }?.key }
        }
    }

    Box {
        Canvas(
            modifier = modifier
                // click detection preserved
                .pointerInput(Unit) {
                    detectTapGestures { position ->
                        for ((id, rect) in hoverBounds) {
                            if (rect.contains(Offset(position.x, position.y))) {
                                onClick?.invoke(id)
                                break
                            }
                        }
                    }
                }
                // hover detection using pointerMoveFilter (desktop-friendly)
                .pointerHoverEvents(
                    onMove = { pos ->
                        mousePos = pos
                    },
                    onEnter = { pos ->
                        mousePos = pos
                    },
                    onExit = {
                        mousePos = null
                    }
                )
        ) {
            onDraw()
        }

        // Tooltip Popup: shown when showTooltip == true and we have a hovered id
        if (hoveredId != null) mousePos?.let { pos ->
            // small offset so the tooltip doesn't sit directly under the cursor
            val offsetX = (pos.x + 12f).roundToInt()
            val offsetY = (pos.y + 12f).roundToInt()
            val content = tooltipContent(hoveredId!!)
            if (content != null) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(offsetX, offsetY)
                ) {
                    Surface(
                        color = Color(0xFF222222),
                        tonalElevation = 4.dp,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = content,
                            color = Color.White,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
