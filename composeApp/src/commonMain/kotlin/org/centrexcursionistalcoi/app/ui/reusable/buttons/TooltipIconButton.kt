package org.centrexcursionistalcoi.app.ui.reusable.buttons

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    imageVector: ImageVector,
    tooltip: String,
    contentDescription: String? = tooltip,
    enabled: Boolean = true,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    onClick: () -> Unit,
) {
    TooltipBox(
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning),
        tooltip = { PlainTooltip { Text(tooltip) } }
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
            )
        }
    }
}
