package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.ui.data.DialogContext
import org.centrexcursionistalcoi.app.ui.data.DialogContextImpl
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T> ListCard(
    list: List<T>?,
    titleResource: StringResource,
    emptyTextResource: StringResource,
    displayName: (T) -> String,
    modifier: Modifier = Modifier,
    highlight: ((T) -> Boolean)? = null,
    onCreate: (() -> Unit)? = null,
    onEditRequested: ((T) -> Unit)? = null,
    onDelete: ((T) -> Job)? = null,
    supportingContent: (@Composable (T) -> Unit)? = null,
    detailsDialogContent: (@Composable DialogContext.(T) -> Unit)? = null,
) {
    var deleting by remember { mutableStateOf<T?>(null) }
    if (onDelete != null) {
        deleting?.let { item ->
            DeleteDialog(
                item,
                { it.toString() },
                { onDelete(item) }
            ) { deleting = null }
        }
    }

    var showingDetails by remember { mutableStateOf<T?>(null) }
    if (detailsDialogContent != null) showingDetails?.let { item ->
        AlertDialog(
            onDismissRequest = { showingDetails = null },
            dismissButton = if (onEditRequested != null) {
                {
                    TextButton(onClick = { onEditRequested(item) }) {
                        Text(stringResource(Res.string.edit))
                    }
                }
            } else null,
            confirmButton = {
                TextButton(onClick = { showingDetails = null }) {
                    Text(stringResource(Res.string.close))
                }
            },
            text = {
                Column {
                    val context = DialogContextImpl(this) { showingDetails = null }
                    context.detailsDialogContent(item)
                }
            }
        )
    }

    OutlinedCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(titleResource),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            onCreate?.let { create ->
                IconButton(
                    onClick = create
                ) { Icon(Icons.Default.Add, stringResource(Res.string.create)) }
            }
        }
        if (list == null) {
            Text(stringResource(Res.string.status_loading))
        } else if (list.isEmpty()) {
            Text(stringResource(emptyTextResource))
        } else {
            for (item in list) {
                val shouldHighlight = highlight?.invoke(item) == true
                val containerColor by animateColorAsState(
                    targetValue = if (shouldHighlight) MaterialTheme.colorScheme.primary else Color.Unspecified
                )
                ListItem(
                    headlineContent = { Text(displayName(item)) },
                    supportingContent = if (supportingContent != null) {
                        { supportingContent(item) }
                    } else null,
                    trailingContent = if (onDelete != null) {
                        {
                            IconButton(
                                onClick = { deleting = item }
                            ) {
                                Icon(Icons.Default.Delete, stringResource(Res.string.delete))
                            }
                        }
                    } else null,
                    leadingContent = {
                        if (item is Department && item.image != null) {
                            val imageFile by item.rememberImageFile()
                            AsyncImage(
                                model = imageFile,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = detailsDialogContent != null) { showingDetails = item },
                    colors = ListItemDefaults.colors(containerColor)
                )
            }
        }
    }
}
