package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
actual fun PlatformNavigationBar(
    selection: Int,
    onSelectionChanged: (Int) -> Unit,
    items: List<Pair<ImageVector, String>>
) {
    NavigationBar {
        for ((index, item) in items.withIndex()) {
            val (icon, label) = item
            NavigationBarItem(
                selected = index == selection,
                icon = { Icon(icon, label) },
                label = { Text(label) },
                onClick = { onSelectionChanged(index) }
            )
        }
    }
}
