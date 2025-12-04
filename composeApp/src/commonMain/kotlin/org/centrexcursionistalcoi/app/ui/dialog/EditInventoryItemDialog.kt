package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Remove
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Undo
import org.centrexcursionistalcoi.app.ui.utils.optional
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditInventoryItemDialog(
    item: ReferencedInventoryItem,
    onSubmit: (variation: String, nfcId: ByteArray?) -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var variation by remember { mutableStateOf("") }
    var nfcId by remember { mutableStateOf(item.nfcId) }

    val isDirty = variation != item.variation || nfcId != item.nfcId

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text(stringResource(Res.string.management_inventory_item_edit)) },
        text = {
            Column {
                OutlinedTextField(
                    value = variation,
                    onValueChange = { variation = it },
                    label = { Text(stringResource(Res.string.create_inventory_item_variation).optional()) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = nfcId?.toHexString() ?: stringResource(Res.string.none),
                    onValueChange = { },
                    label = { Text(stringResource(Res.string.create_inventory_item_nfc_id).optional()) },
                    readOnly = true,
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (nfcId != null) {
                            IconButton(
                                onClick = { nfcId = null }
                            ) { Icon(MaterialSymbols.Remove, stringResource(Res.string.remove)) }
                        } else {
                            IconButton(
                                onClick = { nfcId = item.nfcId }
                            ) { Icon(MaterialSymbols.Undo, stringResource(Res.string.undo)) }
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && isDirty,
                onClick = {
                    isLoading = true
                    onSubmit(variation, nfcId).invokeOnCompletion { onDismissRequested() }
                }
            ) { Text(stringResource(Res.string.submit)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
