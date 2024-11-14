package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

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
            horizontalArrangement = Arrangement.End
        ) {
            title?.let {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
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
