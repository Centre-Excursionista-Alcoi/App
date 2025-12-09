package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIcon(
    imageVector: ImageVector,
    tooltip: String,
    contentDescription: String? = tooltip,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
) {
    TooltipBox(
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning),
        tooltip = { PlainTooltip { Text(tooltip) } }
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}
