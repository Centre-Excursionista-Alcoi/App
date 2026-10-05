package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.cancel
import cea_app.composeapp.generated.resources.delete
import cea_app.composeapp.generated.resources.edit
import cea_app.composeapp.generated.resources.management_spaces_cancel
import cea_app.composeapp.generated.resources.management_spaces_key_max
import cea_app.composeapp.generated.resources.management_spaces_key_name
import cea_app.composeapp.generated.resources.management_spaces_save
import cea_app.composeapp.generated.resources.none
import cea_app.composeapp.generated.resources.space_key_add
import cea_app.composeapp.generated.resources.space_key_description
import cea_app.composeapp.generated.resources.space_key_label
import cea_app.composeapp.generated.resources.space_key_line
import cea_app.composeapp.generated.resources.space_key_nfc
import cea_app.composeapp.generated.resources.space_key_nfc_invalid
import cea_app.composeapp.generated.resources.space_key_type_add
import cea_app.composeapp.generated.resources.space_key_type_spaces
import cea_app.composeapp.generated.resources.space_keys_inventory
import cea_app.composeapp.generated.resources.space_keys_inventory_empty
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.SpaceKey
import org.centrexcursionistalcoi.app.data.SpaceKeyType
import org.centrexcursionistalcoi.app.data.SpaceKeyTypeSpace
import org.centrexcursionistalcoi.app.viewmodel.management.SpacesManagementViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * The keys the club has: its types of keys (each for one or several spaces) and the individual keys of each, which can
 * have a label and an NFC tag.
 */
@OptIn(ExperimentalStdlibApi::class)
@Composable
fun SpaceKeysInventory(model: SpacesManagementViewModel, spaces: List<Space>) {
    val types by model.keyTypes.collectAsState()
    val keys by model.keys.collectAsState()

    var typeDialog by remember { mutableStateOf<SpaceKeyType?>(null) }
    var creatingType by remember { mutableStateOf(false) }
    var keyDialog by remember { mutableStateOf<Pair<SpaceKeyType, SpaceKey?>?>(null) }
    var deletingType by remember { mutableStateOf<SpaceKeyType?>(null) }

    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(stringResource(Res.string.space_keys_inventory), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { creatingType = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(stringResource(Res.string.space_key_type_add))
        }
        if (types.orEmpty().isEmpty()) Text(stringResource(Res.string.space_keys_inventory_empty))
        for (type in types.orEmpty()) {
            val typeKeys = keys.orEmpty().filter { it.type == type.id }
            OutlinedCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(type.name, style = MaterialTheme.typography.titleSmall)
                    type.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    val spaceNames = type.spaces.mapNotNull { link ->
                        spaces.find { it.id == link.space }?.let { "${it.name} (${link.maxPerLending})" }
                    }
                    Text(
                        stringResource(Res.string.space_key_type_spaces, spaceNames.joinToString().ifEmpty { stringResource(Res.string.none) }),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    for (key in typeKeys) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(
                                    Res.string.space_key_line,
                                    key.label?.takeIf { it.isNotBlank() } ?: key.id.toString().take(8),
                                    key.nfcId?.toHexString() ?: stringResource(Res.string.none),
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { keyDialog = type to key }) { Text(stringResource(Res.string.edit)) }
                            TextButton(onClick = { model.deleteKey(key.id) }) { Text(stringResource(Res.string.delete)) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { keyDialog = type to null }) { Text(stringResource(Res.string.space_key_add)) }
                        TextButton(onClick = { typeDialog = type }) { Text(stringResource(Res.string.edit)) }
                        TextButton(onClick = { deletingType = type }) { Text(stringResource(Res.string.delete)) }
                    }
                }
            }
        }
    }

    if (creatingType) {
        SpaceKeyTypeDialog(
            type = null,
            spaces = spaces,
            onDismiss = { creatingType = false },
            onSave = { name, description, links ->
                creatingType = false
                model.createKeyType(name, description.takeIf { it.isNotBlank() }, links)
            },
        )
    }
    typeDialog?.let { type ->
        SpaceKeyTypeDialog(
            type = type,
            spaces = spaces,
            onDismiss = { typeDialog = null },
            onSave = { name, description, links ->
                typeDialog = null
                model.updateKeyType(type.id, name, description, links)
            },
        )
    }
    keyDialog?.let { (type, key) ->
        SpaceKeyDialog(
            key = key,
            onDismiss = { keyDialog = null },
            onSave = { label, nfcId ->
                keyDialog = null
                if (key == null) model.createKey(type.id, label.takeIf { it.isNotBlank() }, nfcId.takeIf { it.isNotEmpty() })
                else model.updateKey(key.id, label, nfcId)
            },
        )
    }
    deletingType?.let { type ->
        AlertDialog(
            onDismissRequest = { deletingType = null },
            title = { Text(type.name) },
            confirmButton = {
                TextButton(onClick = {
                    deletingType = null
                    model.deleteKeyType(type.id)
                }) { Text(stringResource(Res.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deletingType = null }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

/** Creates or edits a type of key: its name and the spaces it is for, with how many a lending of each can take. */
@Composable
private fun SpaceKeyTypeDialog(
    type: SpaceKeyType?,
    spaces: List<Space>,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, spaces: List<SpaceKeyTypeSpace>) -> Unit,
) {
    var name by remember { mutableStateOf(type?.name.orEmpty()) }
    var description by remember { mutableStateOf(type?.description.orEmpty()) }
    // The maximum per space, as typed; a space without an entry is not one of the type's
    var maximums by remember {
        mutableStateOf(type?.spaces.orEmpty().associate { it.space to it.maxPerLending.toString() })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.management_spaces_key_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.space_key_description)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                for (space in spaces) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(
                            checked = space.id in maximums,
                            onCheckedChange = { checked ->
                                maximums = if (checked) maximums + (space.id to "1") else maximums - space.id
                            },
                        )
                        Text(space.name, modifier = Modifier.weight(1f))
                        if (space.id in maximums) {
                            OutlinedTextField(
                                value = maximums.getValue(space.id),
                                onValueChange = { maximums = maximums + (space.id to it.filter { c -> c.isDigit() }) },
                                label = { Text(stringResource(Res.string.management_spaces_key_max)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && maximums.values.all { (it.toIntOrNull() ?: 0) > 0 },
                onClick = {
                    onSave(
                        name.trim(),
                        description.trim(),
                        maximums.map { (space, max) -> SpaceKeyTypeSpace(space, max.toInt()) },
                    )
                },
            ) { Text(stringResource(Res.string.management_spaces_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.management_spaces_cancel)) } },
    )
}

/** Creates or edits a key: its label, and the id of its NFC tag, typed in hexadecimal. */
@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun SpaceKeyDialog(key: SpaceKey?, onDismiss: () -> Unit, onSave: (label: String, nfcId: ByteArray) -> Unit) {
    var label by remember { mutableStateOf(key?.label.orEmpty()) }
    var nfc by remember { mutableStateOf(key?.nfcId?.toHexString().orEmpty()) }
    val nfcId = remember(nfc) { nfc.trim().takeIf { it.isEmpty() }?.let { ByteArray(0) } ?: runCatching { nfc.trim().hexToByteArray() }.getOrNull() }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(Res.string.space_key_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = nfc,
                    onValueChange = { nfc = it },
                    label = { Text(stringResource(Res.string.space_key_nfc)) },
                    isError = nfcId == null,
                    supportingText = if (nfcId == null) ({ Text(stringResource(Res.string.space_key_nfc_invalid)) }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = nfcId != null, onClick = { onSave(label.trim(), nfcId!!) }) {
                Text(stringResource(Res.string.management_spaces_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.management_spaces_cancel)) } },
    )
}
