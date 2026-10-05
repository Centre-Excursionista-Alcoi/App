package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.PriceUnit
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.screen.spaces.formatPrice
import org.centrexcursionistalcoi.app.ui.screen.spaces.label
import org.centrexcursionistalcoi.app.viewmodel.management.SpacesManagementViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

/**
 * Create, edit, close and delete spaces, and manage their keys. For the people who manage spaces.
 */
@Composable
fun SpacesListView(model: SpacesManagementViewModel = koinViewModel()) {
    val spaces by model.spaces.collectAsState()

    var editing by remember { mutableStateOf<Space?>(null) }
    var creating by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf<Space?>(null) }
    var deleting by remember { mutableStateOf<Space?>(null) }

    val spacesValue = spaces
    if (spacesValue == null) {
        LoadingBox()
        return
    }

    LazyColumnWidthWrapper(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item("add") {
            Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text(stringResource(Res.string.management_spaces_add))
            }
        }
        if (spacesValue.isEmpty()) {
            item("empty") { Text(stringResource(Res.string.spaces_empty)) }
        }
        item("inventory") { SpaceKeysInventory(model, spacesValue) }
        items(spacesValue, key = { it.id.toString() }) { space ->
            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(space.name, style = MaterialTheme.typography.titleMedium)
                    Text(space.description, maxLines = 3)
                    if (space.isClosed) {
                        Text(
                            text = space.closedReason?.let { stringResource(Res.string.spaces_closed_reason, it) }
                                ?: stringResource(Res.string.spaces_closed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    space.prices.forEach { price ->
                        Text(
                            stringResource(
                                Res.string.spaces_price_line,
                                price.category.label(),
                                formatPrice(price.price),
                                price.unit.label(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editing = space }) { Text(stringResource(Res.string.edit)) }
                        if (space.isClosed) {
                            OutlinedButton(onClick = { model.openSpace(space.id) }) {
                                Text(stringResource(Res.string.management_spaces_open))
                            }
                        } else {
                            OutlinedButton(onClick = { closing = space }) {
                                Text(stringResource(Res.string.management_spaces_close))
                            }
                        }
                        OutlinedButton(onClick = { deleting = space }) { Text(stringResource(Res.string.delete)) }
                    }
                }
            }
        }
    }

    if (creating) {
        SpaceEditorDialog(
            space = null,
            onDismiss = { creating = false },
            onSave = { name, description, conditions, requiresKeys, prices ->
                creating = false
                model.createSpace(name, description, conditions.takeIf { it.isNotBlank() }, requiresKeys, prices)
            },
        )
    }
    editing?.let { space ->
        SpaceEditorDialog(
            space = space,
            onDismiss = { editing = null },
            onSave = { name, description, conditions, requiresKeys, prices ->
                editing = null
                model.updateSpace(space.id, name, description, conditions, requiresKeys, prices)
            },
        )
    }
    closing?.let { space ->
        CloseSpaceDialog(
            onDismiss = { closing = null },
            onClose = { reason ->
                closing = null
                model.closeSpace(space.id, Clock.System.now(), until = null, reason = reason.takeIf { it.isNotBlank() })
            },
        )
    }
    deleting?.let { space ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text(stringResource(Res.string.management_spaces_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    model.deleteSpace(space.id)
                }) { Text(stringResource(Res.string.management_spaces_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(Res.string.management_spaces_cancel)) }
            },
        )
    }
}

@Composable
private fun SpaceEditorDialog(
    space: Space?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, conditions: String, requiresKeys: Boolean, prices: List<CategoryPrice>) -> Unit,
) {
    var name by remember { mutableStateOf(space?.name.orEmpty()) }
    var description by remember { mutableStateOf(space?.description.orEmpty()) }
    var conditions by remember { mutableStateOf(space?.conditionsOfUse.orEmpty()) }
    var requiresKeys by remember { mutableStateOf(space?.requiresKeys ?: false) }
    // Price of each category as typed (empty: no price), and what it is charged for
    var priceTexts by remember {
        mutableStateOf(Category.entries.associateWith { category ->
            space?.prices?.find { it.category == category }?.price?.toString().orEmpty()
        })
    }
    var units by remember {
        mutableStateOf(Category.entries.associateWith { category ->
            space?.prices?.find { it.category == category }?.unit ?: PriceUnit.PER_NIGHT
        })
    }

    val prices = Category.entries.mapNotNull { category ->
        priceTexts.getValue(category).replace(',', '.').toDoubleOrNull()?.let { CategoryPrice(category, it, units.getValue(category)) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (space == null) Res.string.management_spaces_add else Res.string.management_spaces_edit))
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.management_spaces_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.management_spaces_description)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = conditions,
                    onValueChange = { conditions = it },
                    label = { Text(stringResource(Res.string.management_spaces_conditions)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(checked = requiresKeys, onCheckedChange = { requiresKeys = it })
                    Text(stringResource(Res.string.management_spaces_requires_keys))
                }
                Text(
                    stringResource(Res.string.spaces_prices),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Category.entries.forEach { category ->
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedTextField(
                            value = priceTexts.getValue(category),
                            onValueChange = { priceTexts = priceTexts + (category to it) },
                            label = { Text("${category.label()} (€)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PriceUnit.entries.forEach { unit ->
                                FilterChip(
                                    selected = units.getValue(category) == unit,
                                    onClick = { units = units + (category to unit) },
                                    label = { Text(unit.label()) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && description.isNotBlank(),
                onClick = { onSave(name.trim(), description.trim(), conditions.trim(), requiresKeys, prices) },
            ) { Text(stringResource(Res.string.management_spaces_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.management_spaces_cancel)) }
        },
    )
}

@Composable
private fun CloseSpaceDialog(onDismiss: () -> Unit, onClose: (reason: String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.management_spaces_close)) },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(stringResource(Res.string.management_spaces_close_reason)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onClose(reason) }) { Text(stringResource(Res.string.management_spaces_close)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.management_spaces_cancel)) }
        },
    )
}
