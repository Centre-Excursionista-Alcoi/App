package org.centrexcursionistalcoi.app.ui.data

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
data class IconAction(
    val icon: ImageVector,
    val contentDescription: String? = null,
    val tooltip: String? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
) {
    @Composable
    @ExperimentalMaterial3Api
    fun IconButton() {
        TooltipBox(
            enableUserInput = tooltip != null,
            state = rememberTooltipState(),
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Left),
            tooltip = {
                tooltip?.let { PlainTooltip { Text(it) } }
            }
        ) {
            androidx.compose.material3.IconButton(
                enabled = enabled,
                onClick = onClick,
            ) {
                Icon(icon, contentDescription = contentDescription)
            }
        }
    }
}
