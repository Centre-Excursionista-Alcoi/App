package ui.reusable.list

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import app.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun SelectableListItem(
    selected: Boolean,
    text: String,
    enabled: Boolean = true,
    overlineContent: String? = null,
    supportingContent: String? = null,
    onClick: () -> Unit
) {
    val contentColor = LocalContentColor.current
    CompositionLocalProvider(
        LocalContentColor provides contentColor.copy(alpha = if (enabled) 1f else .5f)
    ) {
        ListItem(
            modifier = Modifier
                .clickable(
                    enabled = enabled && !selected,
                    onClickLabel = if (selected) {
                        stringResource(Res.string.selected)
                    } else if (enabled) {
                        stringResource(Res.string.select)
                    } else {
                        stringResource(Res.string.disabled)
                    },
                    role = Role.RadioButton,
                    onClick = onClick
                ),
            leadingContent = {
                Icon(
                    imageVector = if (selected) {
                        Icons.Outlined.RadioButtonChecked
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = if (selected) {
                        stringResource(Res.string.checked)
                    } else {
                        stringResource(Res.string.unchecked)
                    }
                )
            },
            headlineContent = { Text(text) },
            overlineContent = overlineContent?.let { { Text(it) } },
            supportingContent = supportingContent?.let { { Text(it) } }
        )
    }
}
