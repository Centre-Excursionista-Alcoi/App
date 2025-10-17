package org.centrexcursionistalcoi.app.ui.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
data class IconAction(
    val icon: ImageVector,
    val contentDescription: String? = null,
    val tooltip: String? = null,
    val onClick: () -> Unit,
)
