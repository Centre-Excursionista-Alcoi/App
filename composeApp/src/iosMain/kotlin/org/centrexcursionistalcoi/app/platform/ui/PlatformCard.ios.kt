package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoSurface
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformCard(
    title: String?,
    modifier: Modifier,
    action: Triple<ImageVector, String, () -> Unit>?,
    content: @Composable ColumnScope.() -> Unit
) {
    CupertinoSurface(
        modifier = modifier,
        color = CupertinoTheme.colorScheme.secondarySystemBackground
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (title != null || action != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    title?.let {
                        CupertinoText(
                            text = it,
                            modifier = Modifier.weight(1f),
                            style = CupertinoTheme.typography.title2
                        )
                    }
                    action?.let { (icon, description, onClick) ->
                        CupertinoIconButton(
                            onClick = onClick
                        ) {
                            CupertinoIcon(icon, description)
                        }
                    }
                }
            }
        }
    }
}
