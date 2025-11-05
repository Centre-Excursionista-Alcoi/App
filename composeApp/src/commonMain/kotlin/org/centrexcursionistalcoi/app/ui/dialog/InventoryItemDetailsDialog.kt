package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.ui.reusable.DeleteButton
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InventoryItemDetailsDialog(
    item: ReferencedInventoryItem,
    onDelete: () -> Job,
    onEdit: (variation: String) -> Job,
    onDismissRequest: () -> Unit
) {
    var deleting by remember { mutableStateOf(false) }
    if (deleting) {
        DeleteDialog(item, { it.type.displayName }, onDelete) { deleting = false }
    }

    var editing by remember { mutableStateOf(false) }
    if (editing) {
        EditInventoryItemDialog(item, onEdit) { editing = false }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(item.id.toString(), fontFamily = FontFamily.Monospace) },
        text = {
            Column {
                Text(
                    stringResource(Res.string.inventory_item_type, item.type.displayName)
                )
                Text(
                    stringResource(Res.string.inventory_item_variation, item.variation ?: stringResource(Res.string.none))
                )

                val nfcIdHex = item.nfcId?.toHexString()
                Text(
                    stringResource(Res.string.inventory_item_nfc_id, nfcIdHex ?: stringResource(Res.string.none))
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { editing = true },
            ) {
                Text(stringResource(Res.string.edit))
            }
        },
        dismissButton = {
            DeleteButton { deleting = true }
        },
    )
}
