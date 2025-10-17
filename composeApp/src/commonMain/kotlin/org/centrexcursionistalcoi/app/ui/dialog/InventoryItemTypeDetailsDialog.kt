package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.jetbrains.compose.resources.stringResource

@Composable
fun InventoryItemTypeDetailsDialog(type: InventoryItemType, onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.inventory_item_type_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.inventory_item_type, type.displayName),
                    style = MaterialTheme.typography.titleMedium
                )
                val description = type.description
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (type.image != null) {
                    val imageFile by type.rememberImageFile()

                    AsyncByteImage(
                        bytes = imageFile,
                        contentDescription = type.displayName,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(bottom = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest
            ) { Text(stringResource(Res.string.close)) }
        },
    )
}
