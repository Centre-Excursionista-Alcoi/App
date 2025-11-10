package org.centrexcursionistalcoi.app.ui.reusable.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.centrexcursionistalcoi.app.ui.reusable.buttons.CloseButton

@Composable
fun <T: Any> SettingsOptionsRow(
    title: String,
    options: List<T>,
    selection: T? = null,
    summary: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = title,
    toString: (T) -> String = { it.toString() },
    optionLeadingContent: (@Composable (T) -> Unit)? = null,
    onOptionSelected: (T) -> Unit,
) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        AlertDialog(
            onDismissRequest = { showingDialog = false },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(items = options, key = { it }) { option ->
                        ListItem(
                            headlineContent = { Text(toString(option)) },
                            leadingContent = optionLeadingContent?.let {
                                { it(option) }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (option == selection) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                headlineColor = if (option == selection) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified,
                            ),
                            modifier = Modifier.fillMaxWidth().clickable {
                                onOptionSelected(option)
                                showingDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                CloseButton { showingDialog = false }
            },
        )
    }

    SettingsRow(
        title = title,
        summary = summary,
        icon = icon,
        contentDescription = contentDescription,
        onClick = { showingDialog = true }
    )
}
