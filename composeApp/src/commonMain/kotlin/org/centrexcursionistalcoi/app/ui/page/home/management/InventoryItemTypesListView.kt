package org.centrexcursionistalcoi.app.ui.page.home.management

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.dialog.QRCodeDialog
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.form.AutocompleteMultipleFormField
import org.centrexcursionistalcoi.app.ui.reusable.form.FormImagePicker
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemTypesListView(
    windowSizeClass: WindowSizeClass,
    selectedItemId: Uuid?,
    types: List<InventoryItemType>?,
    allCategories: Set<String>,
    items: List<ReferencedInventoryItem>?,
    onCreate: (displayName: String, description: String, categories: List<String>, image: PlatformFile?) -> Job,
    onUpdate: (id: Uuid, displayName: String, description: String, categories: List<String>, image: PlatformFile?) -> Job,
    onDelete: (InventoryItemType) -> Job,
    onDeleteInventoryItem: (ReferencedInventoryItem) -> Job,
) {
    val selectedItemIndex = remember(selectedItemId, types) {
        types?.indexOfFirst { it.id == selectedItemId }
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
        selectedItemIndex = selectedItemIndex,
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
        emptyItemsText = stringResource(Res.string.management_no_item_types),
        isCreatingSupported = true,
        onDeleteRequest = { (type) -> onDelete(type) },
        editItemContent = { typeAndItems ->
            val type = typeAndItems?.first

            var isLoading by remember { mutableStateOf(false) }
            var image by remember { mutableStateOf<PlatformFile?>(null) }
            var categories by remember { mutableStateOf(type?.categories ?: emptyList()) }
            var displayName by remember { mutableStateOf(type?.displayName ?: "") }
            var description by remember { mutableStateOf(type?.description ?: "") }

            val isDirty = if (type == null) true else
                displayName != type.displayName ||
                        description != type.description ||
                        categories != type.categories ||
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
                entries = type?.categories.orEmpty(),
                onEntryAdded = { categories += it },
                onEntryRemoved = { categories -= it },
                suggestions = allCategories,
                label = { Text(stringResource(Res.string.management_inventory_item_type_categories)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                enabled = !isLoading,
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
                        onCreate(displayName, description, categories, image)
                    } else {
                        onUpdate(type.id, displayName, description, categories, image)
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
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp))
            )
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

        var highlightItemId by remember { mutableStateOf<Uuid?>(null) }
        var highlightItemNfcId by remember { mutableStateOf<ByteArray?>(null) }
        LaunchedEffect(highlightItemId, highlightItemNfcId) {
            if (highlightItemId != null || highlightItemNfcId != null) {
                delay(3000) // Highlight for 3 seconds
                highlightItemId = null
                highlightItemNfcId = null
            }
        }

        var showingItemDialog by remember { mutableStateOf<ReferencedInventoryItem?>(null) }
        showingItemDialog?.let { item ->
            QRCodeDialog(
                value = item.id.toString(),
                onReadNfc = { payload ->
                    payload.uuid()?.let { highlightItemId = it }
                    payload.id?.let { highlightItemNfcId = it }
                },
                onDismissRequest = { showingItemDialog = null }
            )
        }

        var deletingItem by remember { mutableStateOf<ReferencedInventoryItem?>(null) }
        deletingItem?.let { item ->
            DeleteDialog(
                item = item,
                displayName = { it.id.toString() },
                onDelete = { onDeleteInventoryItem(item) },
                onDismissRequested = { deletingItem = null }
            )
        }

        for (item in items) {
            val isHighlighted = item.id == highlightItemId || (item.nfcId != null && item.nfcId contentEquals highlightItemNfcId)
            val backgroundColor by animateColorAsState(if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            ListItem(
                headlineContent = { Text(item.id.toString(), fontFamily = FontFamily.Monospace) },
                supportingContent = item.variation?.let { { Text(stringResource(Res.string.inventory_item_variation, it)) } },
                overlineContent = item.nfcId?.let { { Text(stringResource(Res.string.inventory_item_nfc_id, it.toHexString())) } },
                colors = ListItemDefaults.colors(containerColor = backgroundColor),
                modifier = Modifier.clickable { showingItemDialog = item },
            )
        }
    }
}