package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.nav.LocalTransitionContext
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.ui.dialog.CreateInventoryItemDialog
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.dialog.EditInventoryItemTypeDialog
import org.centrexcursionistalcoi.app.ui.dialog.QRCodeDialog
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.utils.currentOrThrow
import org.centrexcursionistalcoi.app.viewmodel.InventoryItemModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InventoryItemsScreen(
    typeId: Uuid,
    model: InventoryItemModel = viewModel { InventoryItemModel(typeId) },
    onBack: () -> Unit
) {
    val type by model.type.collectAsState()
    val items by model.items.collectAsState()

    InventoryItemsScreen(
        type = type,
        items = items.orEmpty(),
        onCreate = model::createInventoryItem,
        onUpdate = model::updateInventoryItemType,
        onDelete = model::delete,
        onBack = onBack
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun InventoryItemsScreen(
    type: InventoryItemType?,
    items: List<ReferencedInventoryItem>,
    onCreate: (variation: String, type: InventoryItemType, amount: Int) -> Job,
    onUpdate: (id: Uuid, displayName: String?, description: String?, image: PlatformFile?) -> Job,
    onDelete: () -> Job,
    onBack: () -> Unit
) {
    val (sharedTransitionScope, animatedContentScope) = LocalTransitionContext.currentOrThrow

    var showingItemDetails by remember { mutableStateOf<ReferencedInventoryItem?>(null) }
    showingItemDetails?.let { item ->
        QRCodeDialog(value = item.id.toString()) { showingItemDetails = null }
    }

    // Creating item
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateInventoryItemDialog(
            type = type,
            onCreate = onCreate,
            onDismissRequested = { creating = false }
        )
    }

    var editing by remember { mutableStateOf(false) }
    if (editing && type != null) {
        EditInventoryItemTypeDialog(type, onUpdate) { editing = false }
    }

    var deleting by remember { mutableStateOf(false) }
    if (deleting && type != null) {
        DeleteDialog(type, { it.displayName }, onDelete) { deleting = false }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    with(sharedTransitionScope) {
                        Text(
                            text = type?.displayName ?: stringResource(Res.string.status_loading),
                            modifier = Modifier.sharedBounds(
                                sharedContentState = sharedTransitionScope.rememberSharedContentState("inventory_item_type"),
                                animatedVisibilityScope = animatedContentScope
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                    ) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(Res.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { deleting = true }
                    ) {
                        Icon(Icons.Default.Delete, stringResource(Res.string.delete))
                    }
                    IconButton(
                        onClick = { editing = true }
                    ) {
                        Icon(Icons.Default.Edit, stringResource(Res.string.edit))
                    }
                    IconButton(
                        onClick = { creating = true }
                    ) {
                        Icon(Icons.Default.Add, stringResource(Res.string.create))
                    }
                },
            )
        }
    ) { paddingValues ->
        if (type == null) {
            LoadingBox(paddingValues)
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Badge(
                modifier = Modifier.padding(8.dp).zIndex(2f).align(Alignment.TopEnd),
            ) {
                Text(
                    text = pluralStringResource(Res.plurals.management_items_amount, items.size, items.size),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val description = type.description
                if (!description.isNullOrBlank()) item("description") {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (type.image != null) item("image") {
                    val imageFile by type.rememberImageFile()
                    AsyncByteImage(
                        bytes = imageFile,
                        contentDescription = type.displayName,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(bottom = 8.dp)
                    )
                }
                items(items) { item ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = item.id.toString().uppercase(),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                            )
                        },
                        supportingContent = { Text(item.variation ?: "(No variation)") },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = { showingItemDetails = item }
                                ) {
                                    Icon(Icons.Default.QrCode, stringResource(Res.string.qrcode))
                                }
                                /*IconButton(
                                    onClick = { onDelete(item) }
                                ) {
                                    Icon(Icons.Default.Delete, stringResource(Res.string.delete))
                                }*/
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
        }
    }
}
