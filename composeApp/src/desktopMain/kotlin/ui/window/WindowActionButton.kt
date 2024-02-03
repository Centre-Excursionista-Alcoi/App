package ui.window

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.semantics.Role
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
@ExperimentalComposeUiApi
fun WindowActionButton(
    icon: ImageVector,
    contentDescription: StringResource,
    idleColor: Color,
    hoverColor: Color = idleColor,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isHovering by remember { mutableStateOf(false) }
    val color by animateColorAsState(targetValue = if (isHovering) hoverColor else idleColor)

    Icon(
        imageVector = icon,
        contentDescription = stringResource(contentDescription),
        tint = if (enabled) color else idleColor.copy(alpha = .5f),
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) { isHovering = true }
            .onPointerEvent(PointerEventType.Exit) { isHovering = false }
            .clickable(enabled, stringResource(contentDescription), Role.Button, onClick)
    )
}
