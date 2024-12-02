package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class Action(
    val icon: ImageVector,
    val label: String,
    /**
     * Whether the action is a primary action (`true`) or secondary (`false`).
     * Only used in desktop.
     */
    val isPrimary: Boolean = true,
    /**
     * If not null, when clicking the icon, a popup window will appear with this content.
     * Then, if set, [onClick] will be ignored.
     * Only used in desktop.
     */
    val popupContent: (@Composable ColumnScope.() -> Unit)? = null,
    val onClick: () -> Unit
)
