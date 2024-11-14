package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
actual fun PlatformCard(
    title: String?,
    modifier: Modifier,
    action: Triple<ImageVector, String, () -> Unit>?,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            title?.let {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f).padding(8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            action?.let { (icon, title, action) ->
                IconButton(
                    onClick = action,
                ) {
                    Icon(icon, title)
                }
            }
        }
        content()
    }
}
