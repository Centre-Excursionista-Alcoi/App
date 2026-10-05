package org.centrexcursionistalcoi.app.ui.screen.spaces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.cancel
import cea_app.composeapp.generated.resources.space_keys_none_available
import cea_app.composeapp.generated.resources.space_keys_pickup_title
import cea_app.composeapp.generated.resources.space_keys_pickup_type
import cea_app.composeapp.generated.resources.space_keys_return_title
import cea_app.composeapp.generated.resources.space_keys_confirm
import org.centrexcursionistalcoi.app.data.SpaceKey
import org.centrexcursionistalcoi.app.data.SpaceKeyType
import org.centrexcursionistalcoi.app.data.SpaceLendingKey
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/** How a key is told from the others of its type. */
fun SpaceKey.displayName(): String = label?.takeIf { it.isNotBlank() } ?: id.toString().take(8)

/**
 * A manager chooses the exact keys they hand over: as many as the lending asks for of each type, or fewer.
 * @param keys The keys that are free to give.
 */
@Composable
fun SpaceKeysPickupDialog(
    requested: Map<Uuid, Int>,
    keyTypes: List<SpaceKeyType>,
    keys: List<SpaceKey>,
    onDismiss: () -> Unit,
    onConfirm: (List<Uuid>) -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<Uuid>()) }
    val withinRequest = requested.all { (typeId, quantity) ->
        keys.count { it.id in selected && it.type == typeId } <= quantity
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.space_keys_pickup_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for ((typeId, quantity) in requested) {
                    val name = keyTypes.find { it.id == typeId }?.name.orEmpty()
                    Text(
                        stringResource(Res.string.space_keys_pickup_type, name, quantity),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    val available = keys.filter { it.type == typeId }
                    if (available.isEmpty()) Text(stringResource(Res.string.space_keys_none_available))
                    for (key in available) {
                        KeyRow(key.displayName(), key.id in selected) { checked ->
                            selected = if (checked) selected + key.id else selected - key.id
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = withinRequest, onClick = { onConfirm(keys.filter { it.id in selected }.map { it.id }) }) {
                Text(stringResource(Res.string.space_keys_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** A manager ticks the keys that are back. */
@Composable
fun SpaceKeysReturnDialog(
    given: List<SpaceLendingKey>,
    keys: List<SpaceKey>,
    keyTypes: List<SpaceKeyType>,
    onDismiss: () -> Unit,
    onConfirm: (List<Uuid>) -> Unit,
) {
    var selected by remember { mutableStateOf(given.map { it.key }.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.space_keys_return_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (item in given) {
                    val key = keys.find { it.id == item.key }
                    val type = key?.let { k -> keyTypes.find { it.id == k.type }?.name }.orEmpty()
                    KeyRow("$type ${key?.displayName() ?: item.key.toString().take(8)}".trim(), item.key in selected) { checked ->
                        selected = if (checked) selected + item.key else selected - item.key
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selected.isNotEmpty(), onClick = { onConfirm(given.map { it.key }.filter { it in selected }) }) {
                Text(stringResource(Res.string.space_keys_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

@Composable
private fun KeyRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}
