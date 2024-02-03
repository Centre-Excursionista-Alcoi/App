package ui.window

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import resources.MR
import ui.state.SharedApplicationState
import java.awt.MouseInfo
import java.awt.Point

private fun Point.toComposeOffset() = IntOffset(x, y)

@Composable
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
fun FrameWindowScope.WindowTopBar(
    windowState: WindowState,
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var startLocation: IntOffset? = null
                    var startPoint: IntOffset? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        if (windowState.placement == WindowPlacement.Floating) {
                            if (event.type == PointerEventType.Press) {
                                startLocation = window.location.toComposeOffset()
                                startPoint = MouseInfo.getPointerInfo().location.toComposeOffset()
                            } else if (event.type == PointerEventType.Release) {
                                startLocation = null
                                startPoint = null
                            } else if (startLocation != null && startPoint != null) {
                                val point = MouseInfo.getPointerInfo().location.toComposeOffset()
                                val location = startLocation + (point - startPoint)
                                window.setLocation(location.x, location.y)
                            }
                        }
                    }
                }
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onDoubleClick = {
                    windowState.placement = if (windowState.placement == WindowPlacement.Maximized)
                        WindowPlacement.Floating
                    else
                        WindowPlacement.Maximized
                },
                onLongClick = {}
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val navigator by SharedApplicationState.navigator.collectAsState(null)
        val canGoBack by SharedApplicationState.canGoBack.collectAsState(false)
        AnimatedVisibility(
            visible = canGoBack && navigator != null,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }
        ) {
            WindowActionButton(
                icon = Icons.Rounded.KeyboardArrowLeft,
                contentDescription = MR.strings.back,
                idleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { navigator?.pop() },
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .padding(vertical = 8.dp)
        )

        WindowActionButton(
            icon = Icons.Rounded.Minimize,
            contentDescription = MR.strings.window_minimize,
            idleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(4.dp)
        ) { windowState.isMinimized = true }
        WindowActionButton(
            icon = if (windowState.placement == WindowPlacement.Maximized) {
                Icons.Rounded.FilterNone
            } else {
                Icons.Rounded.CheckBoxOutlineBlank
            },
            contentDescription = MR.strings.window_maximize,
            idleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(4.dp)
        ) {
            windowState.placement = if (windowState.placement == WindowPlacement.Maximized)
                WindowPlacement.Floating
            else
                WindowPlacement.Maximized
        }
        WindowActionButton(
            icon = Icons.Rounded.Close,
            contentDescription = MR.strings.window_close,
            idleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            hoverColor = MaterialTheme.colorScheme.error,
            onClick = onClose,
            modifier = Modifier.padding(4.dp)
        )
    }
}
