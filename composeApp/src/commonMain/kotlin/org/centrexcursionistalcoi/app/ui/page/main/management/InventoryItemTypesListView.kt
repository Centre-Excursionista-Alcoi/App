package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.inventory_item_create
import cea_app.composeapp.generated.resources.inventory_item_nfc_id
import cea_app.composeapp.generated.resources.inventory_item_variation
import cea_app.composeapp.generated.resources.management_inventory_item_type_categories
import cea_app.composeapp.generated.resources.management_inventory_item_type_create
import cea_app.composeapp.generated.resources.management_inventory_item_type_department
import cea_app.composeapp.generated.resources.management_inventory_item_type_description
import cea_app.composeapp.generated.resources.management_inventory_item_type_display_name
import cea_app.composeapp.generated.resources.management_inventory_item_type_identifiers
import cea_app.composeapp.generated.resources.management_no_item_types
import cea_app.composeapp.generated.resources.none
import cea_app.composeapp.generated.resources.scanner_open
import cea_app.composeapp.generated.resources.submit
import com.diamondedge.logging.logging
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.permission.launchWithCameraPermission
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.ui.dialog.CreateInventoryItemDialog
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.dialog.InventoryItemInformationDialog
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Add
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.QrCodeScanner
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.DropdownField
import org.centrexcursionistalcoi.app.ui.reusable.Scanner
import org.centrexcursionistalcoi.app.ui.reusable.buttons.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.reusable.form.AutocompleteMultipleFormField
import org.centrexcursionistalcoi.app.ui.reusable.form.FormImagePicker
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

private val log = logging()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemTypesListView(
    windowSizeClass: WindowSizeClass,
    selectedItemId: Uuid?,
    types: List<ReferencedInventoryItemType>?,
    allCategories: Set<String>,
    departments: List<Department>?,
    items: List<ReferencedInventoryItem>?,
    onCreate: (displayName: String, description: String, categories: List<String>, weight: String, department: Department?, image: PlatformFile?) -> Job,
    onUpdate: (id: Uuid, displayName: String, description: String, categories: List<String>, weight: String, department: Department?, image: PlatformFile?) -> Job,
    onDelete: (ReferencedInventoryItemType) -> Job,
    onCreateInventoryItem: (variation: String, ReferencedInventoryItemType, amount: Int) -> Job,
    onDeleteInventoryItem: (ReferencedInventoryItem) -> Job,
    onUpdateInventoryItemManufacturerData: (ReferencedInventoryItem, String) -> Job,
) {
    val scope = rememberCoroutineScope()

    var selectedItemTypeId by remember { mutableStateOf(selectedItemId) }

    var highlightItemId by remember { mutableStateOf<Uuid?>(null) }
    var highlightItemNfcId by remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(highlightItemId, highlightItemNfcId) {
        if (highlightItemId != null || highlightItemNfcId != null) {
            delay(3000) // Highlight for 3 seconds
            highlightItemId = null
            highlightItemNfcId = null
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val payload = PlatformNFC.readNFC() ?: continue
            log.d { "NFC tag read: $payload" }
            payload.uuid()?.let { highlightItemId = it }
            payload.id?.let { highlightItemNfcId = it }
        }
    }

    var creatingInventoryItem by remember { mutableStateOf<ReferencedInventoryItemType?>(null) }
    creatingInventoryItem?.let { type ->
        CreateInventoryItemDialog(
            type = type,
            onCreate = onCreateInventoryItem,
            onDismissRequested = { creatingInventoryItem = null }
        )
    }

    var showingScanner by remember { mutableStateOf(false) }
    if (showingScanner) {
        Scanner(
            onScan = { barcode ->
                val data = barcode.data
                // scope.launch { snackbarHostState.showSnackbar(getString(Res.string.scanner_read, data)) }
                log.i { "Barcode: ${barcode.data}, format: ${barcode.format}" }
                items?.find { it.id == data.toUuidOrNull() }?.let {
                    selectedItemTypeId = it.type.id
                    highlightItemId = it.id
                } ?: log.d { "Could not find item id=$data" }
                items?.find { it.nfcId.contentEquals(data.encodeToByteArray()) }?.let {
                    selectedItemTypeId = it.type.id
                    highlightItemId = it.id
                } ?: log.d { "Could not find item with nfcId=$data" }
                items?.find { it.manufacturerTraceabilityCode == data }?.let {
                    selectedItemTypeId = it.type.id
                    highlightItemId = it.id
                } ?: log.d { "Could not find item with manufacturerTraceabilityCode=$data" }
            },
            onDismissRequest = {
                showingScanner = false
            }
        )
    }

    val groupedItems = remember(items, types) {
        items.orEmpty().groupBy { it.type }.toList()
    }
    val typesWithoutItems = remember(items, types) {
        types.orEmpty().filter { type ->
            items?.none { it.type.id == type.id } ?: true
        }.map { type -> type to emptyList<ReferencedInventoryItem>() }
    }
    ListView(
        windowSizeClass = windowSizeClass,
        selectedItemId = selectedItemTypeId,
        items = groupedItems + typesWithoutItems,
        itemIdProvider = { (type) -> type.id },
        itemDisplayName = { (type) -> type.displayName },
        itemLeadingContent = { (type) ->
            type.image ?: return@ListView
            val image by type.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = type.displayName,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
            )
        },
        itemTrailingContent = { (_, items) ->
            Badge { Text(items.size.toString(), style = MaterialTheme.typography.labelLarge) }
        },
        itemToolbarActions = { (type) ->
            TooltipIconButton(
                imageVector = MaterialSymbols.Add,
                tooltip = stringResource(Res.string.inventory_item_create),
                positioning = TooltipAnchorPosition.Left,
                onClick = { creatingInventoryItem = type },
            )
        },
        emptyItemsText = stringResource(Res.string.management_no_item_types),
        isCreatingSupported = true,
        createTitle = stringResource(Res.string.management_inventory_item_type_create),
        onDeleteRequest = { (type) -> onDelete(type) },
        searchBarActions = {
            TooltipIconButton(
                imageVector = MaterialSymbols.QrCodeScanner,
                tooltip = stringResource(Res.string.scanner_open),
                positioning = TooltipAnchorPosition.Left,
                onClick = { scope.launchWithCameraPermission { showingScanner = true } },
            )
        },
        editItemContent = { typeAndItems ->
            val type = typeAndItems?.first

            var isLoading by remember { mutableStateOf(false) }
            var image by remember { mutableStateOf<PlatformFile?>(null) }
            var categories by remember { mutableStateOf(type?.categories ?: emptyList()) }
            var displayName by remember { mutableStateOf(type?.displayName ?: "") }
            var description by remember { mutableStateOf(type?.description ?: "") }
            var weight by remember { mutableStateOf(type?.weight?.toString() ?: "") }
            var department by remember { mutableStateOf<Department?>(type?.department) }

            val isDirty = if (type == null) true else
                displayName != type.displayName ||
                        description != type.description ||
                        categories != type.categories ||
                        weight != type.weight?.toString() ||
                        department != type.department ||
                        image != null

            FormImagePicker(
                image = image,
                container = type,
                onImagePicked = { image = it },
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp)),
                isLoading = isLoading,
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(Res.string.management_inventory_item_type_display_name)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true,
                enabled = !isLoading,
            )

            AutocompleteMultipleFormField(
                entries = categories,
                onEntryAdded = { categories += it },
                onEntryRemoved = { categories -= it },
                suggestions = allCategories,
                label = { Text(stringResource(Res.string.management_inventory_item_type_categories)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                enabled = !isLoading,
            )

            DropdownField(
                value = department,
                onValueChange = { department = it },
                label = stringResource(Res.string.management_inventory_item_type_department),
                options = departments.orEmpty(),
                itemToString = { it?.displayName ?: stringResource(Res.string.none) },
                itemLeadingContent = {
                    it?.let { department ->
                        val image by department.rememberImageFile()
                        AsyncByteImage(
                            bytes = image,
                            contentDescription = department.displayName,
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                enabled = !isLoading && !departments.isNullOrEmpty(),
                allowNull = true,
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(Res.string.management_inventory_item_type_description)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(Modifier.height(64.dp))

            OutlinedButton(
                enabled = !isLoading && isDirty,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                onClick = {
                    isLoading = true
                    val job = if (type == null) {
                        onCreate(displayName, description, categories, weight, department, image)
                    } else {
                        onUpdate(type.id, displayName, description, categories, weight, department, image)
                    }
                    job.invokeOnCompletion {
                        isLoading = false
                        finishEdit()
                    }
                }
            ) {
                Text(stringResource(Res.string.submit))
            }
        },
    ) { (type, items) ->
        if (type.image != null) {
            val image by type.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = type.displayName,
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp)),
                canBeMaximized = true,
            )
        }

        type.department?.let { department ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val image by department.rememberImageFile()
                AsyncByteImage(
                    bytes = image,
                    contentDescription = department.displayName,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = department.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
            }
        }

        Text(
            text = stringResource(Res.string.management_inventory_item_type_categories),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(type.categories.orEmpty()) { category ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(category) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        val description = type.description
        if (description != null) {
            Text(
                text = stringResource(Res.string.management_inventory_item_type_description),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = stringResource(Res.string.management_inventory_item_type_identifiers),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        var deletingItem by remember { mutableStateOf<ReferencedInventoryItem?>(null) }

        var showingItemDialog by remember { mutableStateOf<ReferencedInventoryItem?>(null) }
        LaunchedEffect(items) {
            // Update the dialog contents if the items are updated
            if (showingItemDialog != null) {
                showingItemDialog = items.find { it.id == showingItemDialog?.id }
            }
        }
        showingItemDialog?.let { item ->
            InventoryItemInformationDialog(
                item = item,
                onReadNfc = { payload ->
                    payload.uuid()?.let { highlightItemId = it }
                    payload.id?.let { highlightItemNfcId = it }
                },
                onReadManufacturerData = { onUpdateInventoryItemManufacturerData(item, it) },
                onDeleteRequest = { deletingItem = item },
                onDismissRequest = { showingItemDialog = null }
            )
        }

        deletingItem?.let { item ->
            DeleteDialog(
                item = item,
                displayName = { it.id.toString() },
                onDelete = {
                    onDeleteInventoryItem(item).also { job ->
                        job.invokeOnCompletion { showingItemDialog = null }
                    }
                },
                onDismissRequested = { deletingItem = null }
            )
        }

        for (item in items) {
            val isHighlighted = item.id == highlightItemId || (item.nfcId != null && item.nfcId contentEquals highlightItemNfcId)
            val backgroundColor by animateColorAsState(if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            ListItem(
                headlineContent = {
                    Text(
                        text = item.id.toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                },
                supportingContent = item.variation
                    ?.takeIf(String::isNotEmpty)
                    ?.let { { Text(stringResource(Res.string.inventory_item_variation, it)) } },
                overlineContent = item.nfcId
                    ?.let { { Text(stringResource(Res.string.inventory_item_nfc_id, it.toHexString())) } },
                colors = ListItemDefaults.colors(containerColor = backgroundColor),
                modifier = Modifier.clickable { showingItemDialog = item },
            )
        }
    }
}