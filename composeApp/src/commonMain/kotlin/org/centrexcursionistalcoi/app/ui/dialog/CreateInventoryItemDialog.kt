package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.ui.reusable.DropdownField
import org.centrexcursionistalcoi.app.ui.utils.optional
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateInventoryItemDialog(
    type: ReferencedInventoryItemType? = null,
    onCreate: (variation: String, type: ReferencedInventoryItemType, amount: Int) -> Job,
    onDismissRequested: () -> Unit
) {
    CreateInventoryItemDialog(
        type = type,
        types = null,
        onCreate = onCreate,
        onDismissRequested = onDismissRequested,
    )
}

@Composable
fun CreateInventoryItemDialog(
    types: List<ReferencedInventoryItemType>? = null,
    onCreate: (variation: String, type: ReferencedInventoryItemType, amount: Int) -> Job,
    onDismissRequested: () -> Unit
) {
    CreateInventoryItemDialog(
        type = null,
        types = types,
        onCreate = onCreate,
        onDismissRequested = onDismissRequested,
    )
}

@Composable
private fun CreateInventoryItemDialog(
    type: ReferencedInventoryItemType? = null,
    types: List<ReferencedInventoryItemType>? = null,
    onCreate: (variation: String, type: ReferencedInventoryItemType, amount: Int) -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var variation by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(type) }
    var amount by remember { mutableStateOf("") }
    val isValid = type != null && (amount.isBlank() || amount.toUIntOrNull() != null)

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text("Create item") },
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
                if (types != null) {
                    DropdownField(
                        value = type,
                        onValueChange = { type = it },
                        options = types,
                        label = stringResource(Res.string.create_inventory_item_type),
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        itemToString = { it?.displayName ?: "" }
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(Res.string.create_inventory_item_amount)) },
                    placeholder = { Text("1") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && isValid,
                onClick = {
                    isLoading = true
                    onCreate(variation, type!!, amount.ifBlank { "1" }.toInt()).invokeOnCompletion { onDismissRequested() }
                }
            ) { Text(stringResource(Res.string.create)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
