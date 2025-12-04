package org.centrexcursionistalcoi.app.ui.reusable.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ChevronRight
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Square
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsRow(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = title,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(
                imageVector = icon ?: MaterialSymbols.Square,
                contentDescription = contentDescription,
                tint = if (icon != null) LocalContentColor.current else Color.Transparent,
            )
        },
        supportingContent = summary?.let {
            { Text(it) }
        },
        trailingContent = {
            if (onClick != null) {
                trailingContent?.invoke() ?: run {
                    Icon(
                        imageVector = MaterialSymbols.ChevronRight,
                        contentDescription = stringResource(Res.string.settings_adjust),
                        tint = LocalContentColor.current.copy(alpha = .8f),
                    )
                }
            }
        },
        modifier = Modifier.clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
    )
}
