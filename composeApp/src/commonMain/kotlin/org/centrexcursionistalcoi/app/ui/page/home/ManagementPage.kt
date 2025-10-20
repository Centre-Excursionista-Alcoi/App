package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.ui.dialog.CreateInventoryItemTypeDialog
import org.centrexcursionistalcoi.app.ui.dialog.EditInventoryItemTypeDialog
import org.centrexcursionistalcoi.app.ui.dialog.QRCodeDialog
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.DropdownField
import org.centrexcursionistalcoi.app.ui.reusable.ListCard
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManagementPage(
    windowSizeClass: WindowSizeClass,

    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    users: List<UserData>?,

    inventoryItemTypes: List<InventoryItemType>?,
    onCreateInventoryItemType: (displayName: String, description: String, image: PlatformFile?) -> Job,
    onUpdateInventoryItemType: (id: Uuid, displayName: String?, description: String?, image: PlatformFile?) -> Job,
    onDeleteInventoryItemType: (InventoryItemType) -> Job,

    inventoryItems: List<InventoryItem>?,
    onCreateInventoryItem: (variation: String, type: InventoryItemType, amount: Int) -> Job,
    onDeleteInventoryItem: (InventoryItem) -> Job,

    onManageLendingsRequested: () -> Unit,
) {
    AdaptiveVerticalGrid(
        windowSizeClass,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        item(key = "lendings") {
            Button(
                onClick = onManageLendingsRequested
            ) { Text(stringResource(Res.string.management_lendings)) }
        }
        item(key = "departments") {
            DepartmentsCard(departments, onCreateDepartment, onDeleteDepartment)
        }
        item(key = "users") {
            UsersCard(users)
        }
        item(key = "item_types") {
            InventoryItemTypesCard(
                inventoryItemTypes,
                onCreateInventoryItemType,
                onUpdateInventoryItemType,
                onDeleteInventoryItemType
            )
        }
        item(key = "items") {
            InventoryItemsCard(
                inventoryItemTypes,
                inventoryItems,
                onCreateInventoryItem,
                onDeleteInventoryItem
            )
        }
    }
}

@Composable
fun DepartmentsCard(
    departments: List<Department>?,
    onCreate: (displayName: String, image: PlatformFile?) -> Job,
    onDelete: (Department) -> Job,
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateDepartmentDialog(onCreate) { creating = false }
    }

    ListCard(
        list = departments,
        titleResource = Res.string.management_departments,
        emptyTextResource = Res.string.management_no_departments,
        displayName = { it.displayName },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onCreate = { creating = true },
        onDelete = onDelete
    )
}

@Composable
fun InventoryItemTypesCard(
    types: List<InventoryItemType>?,
    onCreate: (displayName: String, description: String, image: PlatformFile?) -> Job,
    onUpdate: (id: Uuid, displayName: String?, description: String?, image: PlatformFile?) -> Job,
    onDelete: (InventoryItemType) -> Job,
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateInventoryItemTypeDialog(onCreate) { creating = false }
    }

    var editing by remember { mutableStateOf<InventoryItemType?>(null) }
    editing?.let { item ->
        EditInventoryItemTypeDialog(item, onUpdate) { editing = null }
    }

    ListCard(
        list = types,
        titleResource = Res.string.management_inventory_item_types,
        emptyTextResource = Res.string.management_no_item_types,
        displayName = { it.displayName },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onCreate = { creating = true },
        onEditRequested = { editing = it },
        onDelete = onDelete,
        detailsDialogContent = { type ->
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
        },
    )
}

@Composable
fun InventoryItemsCard(
    types: List<InventoryItemType>?,
    items: List<InventoryItem>?,
    onCreate: (variation: String, type: InventoryItemType, amount: Int) -> Job,
    onDelete: (InventoryItem) -> Job,
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateInventoryItemDialog(types.orEmpty(), onCreate) { creating = false }
    }

    var showingItemDetails by remember { mutableStateOf<InventoryItem?>(null) }
    showingItemDetails?.let { item ->
        QRCodeDialog(value = item.id.toString()) { showingItemDetails = null }
    }

    var highlightInventoryItemId by remember { mutableStateOf<Uuid?>(null) }
    LaunchedEffect(PlatformNFC.supportsNFC) {
        Napier.i { "Starting NFC read... Supported: ${PlatformNFC.supportsNFC}" }
        if (PlatformNFC.supportsNFC) withContext(defaultAsyncDispatcher) {
            while (true) {
                val read = PlatformNFC.readNFC() ?: continue
                try {
                    highlightInventoryItemId = Uuid.parse(read)
                    Napier.i { "Highlighting item: $highlightInventoryItemId" }
                } catch (_: IllegalArgumentException) {
                    // invalid UUID
                }
            }
        }
    }
    // dismiss highlight after 3 seconds
    LaunchedEffect(highlightInventoryItemId) {
        if (highlightInventoryItemId != null) {
            delay(3000)
            highlightInventoryItemId = null
        }
    }

    val groupedItems = remember(items, types) {
        items?.groupBy { it.type }?.toList()
    }
    ListCard(
        list = groupedItems,
        titleResource = Res.string.management_inventory_items,
        emptyTextResource = Res.string.management_no_items,
        displayName = { (typeId, items) ->
            (types?.find { it.id == typeId }?.displayName ?: "N/A") + " (${items.size})"
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onCreate = { creating = true },
        detailsDialogContent = { (typeId, items) ->
            val type = types?.find { it.id == typeId }
            if (type == null) {
                Text(stringResource(Res.string.inventory_item_type_not_found))
            } else {
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
                Text(
                    text = stringResource(Res.string.inventory_item_amount, items.size),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                for (item in items) {
                    ListItem(
                        headlineContent = { Text(item.id.toString().uppercase()) },
                        supportingContent = { Text(item.variation ?: "(No variation)") },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = { showingItemDetails = item }
                                ) {
                                    Icon(Icons.Default.QrCode, stringResource(Res.string.qrcode))
                                }
                                IconButton(
                                    onClick = { onDelete(item) }
                                ) {
                                    Icon(Icons.Default.Delete, stringResource(Res.string.delete))
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (highlightInventoryItemId == item.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            headlineColor = if (highlightInventoryItemId == item.id) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified,
                            supportingColor = if (highlightInventoryItemId == item.id) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified,
                            trailingIconColor = if (highlightInventoryItemId == item.id) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified,
                        ),
                    )
                }
            }
        },
    )
}

@Composable
fun UsersCard(users: List<UserData>?) {
    ListCard(
        list = users,
        titleResource = Res.string.management_users,
        emptyTextResource = Res.string.management_no_departments,
        displayName = { it.username },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    )
}

@Composable
fun CreateInventoryItemDialog(
    types: List<InventoryItemType>,
    onCreate: (variation: String, type: InventoryItemType, amount: Int) -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var variation by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<InventoryItemType?>(null) }
    var amount by remember { mutableStateOf("") }
    val isValid = type != null && amount.toUIntOrNull() != null
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text("Create item") },
        text = {
            Column {
                OutlinedTextField(
                    value = variation,
                    onValueChange = { variation = it },
                    label = { Text("Variation (optional)") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownField(
                    value = type,
                    onValueChange = { type = it },
                    options = types,
                    label = "Type",
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    itemToString = { it?.displayName ?: "" }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
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
                    onCreate(variation, type!!, amount.toInt()).invokeOnCompletion { onDismissRequested() }
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text("Cancel") }
        },
    )
}

@Composable
fun CreateDepartmentDialog(
    onCreate: (displayName: String, image: PlatformFile?) -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<PlatformFile?>(null) }
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.Image
    ) { file -> image = file }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text("Create department") },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !isLoading,
                    onClick = { imagePicker.launch() }
                ) {
                    image?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                    } ?: Text(
                        "Select image (optional)",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && displayName.isNotBlank(),
                onClick = {
                    isLoading = true
                    onCreate(displayName, image).invokeOnCompletion { onDismissRequested() }
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text("Cancel") }
        },
    )
}
