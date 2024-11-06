package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
expect fun PlatformNavigationBar(
    selection: Int,
    onSelectionChanged: (Int) -> Unit,
    items: List<Pair<ImageVector, String>>
)
