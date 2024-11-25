package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoNavigationBar
import io.github.alexzhirkevich.cupertino.CupertinoNavigationBarItem
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformNavigationBar(
    selection: Int,
    onSelectionChanged: (Int) -> Unit,
    items: List<Pair<ImageVector, String>>
) {
    CupertinoNavigationBar {
        for ((index, item) in items.withIndex()) {
            val (icon, title) = item
            CupertinoNavigationBarItem(
                selected = index == selection,
                onClick = { onSelectionChanged(index) },
                icon = { CupertinoIcon(icon, title) },
                label = { CupertinoText(title) }
            )
        }
    }
}
