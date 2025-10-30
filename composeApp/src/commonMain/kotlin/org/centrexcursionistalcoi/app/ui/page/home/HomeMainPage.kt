package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.permission.HelperHolder
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.dialog.InventoryItemTypeDetailsDialog
import org.centrexcursionistalcoi.app.ui.dialog.LendingDetailsDialog
import org.centrexcursionistalcoi.app.ui.icons.material.CalendarEndOutline
import org.centrexcursionistalcoi.app.ui.icons.material.CalendarStartOutline
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.CardWithIcon
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeMainPage(
    windowSizeClass: WindowSizeClass,
    snackbarHostState: SnackbarHostState,
    notificationPermissionResult: NotificationPermissionResult?,
    onNotificationPermissionRequest: () -> Unit,
    onNotificationPermissionDenyRequest: () -> Unit,
    profile: ProfileResponse,
    inventoryItems: List<ReferencedInventoryItem>?,
    lendings: List<ReferencedLending>?,
    onLendingSignUpRequested: () -> Unit,
    memoryUploadProgress: Pair<Long, Long>?,
    onMemorySubmitted: (ReferencedLending, PlatformFile) -> Job,
    onMemoryEditorRequested: (ReferencedLending) -> Unit,
    shoppingList: Map<Uuid, Int>,
    onAddItemToShoppingListRequest: (InventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (InventoryItemType) -> Unit,
    onCancelLendingRequest: (ReferencedLending) -> Job,
) {
    val scrollState = rememberLazyStaggeredGridState()
    val permissionHelper = HelperHolder.getPermissionHelperInstance()
    val isRegisteredForLendings = profile.lendingUser != null

    var selectedCategories by remember { mutableStateOf<List<String>>(emptyList()) }

    var showingItemTypeDetails by remember { mutableStateOf<InventoryItemType?>(null) }
    showingItemTypeDetails?.let { type ->
        InventoryItemTypeDetailsDialog(type) { showingItemTypeDetails = null }
    }

    LaunchedEffect(lendings) {
        if (!lendings.isNullOrEmpty()) {
            // Scroll to top when lendings change
            scrollState.animateScrollToItem(0)
        }
    }
    LaunchedEffect(notificationPermissionResult) {
        if (notificationPermissionResult != NotificationPermissionResult.Granted) {
            // Scroll to top when permission is required
            scrollState.animateScrollToItem(0)
        }
    }

    AdaptiveVerticalGrid(
        windowSizeClass,
        state = scrollState,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium) {
            item("welcome_message", span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = stringResource(Res.string.welcome, profile.username),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp, bottom = 24.dp)
                )
            }
        }

        // The notification permission is only used for lendings, so don't ask for it if the user is not registered for lendings
        if (isRegisteredForLendings && notificationPermissionResult in listOf(NotificationPermissionResult.Denied, NotificationPermissionResult.NotAllowed)) {
            item("notification_permission", contentType = "permission") {
                CardWithIcon(
                    title = stringResource(Res.string.permission_notification_title),
                    message = stringResource(Res.string.permission_notification_message),
                    icon = Icons.Default.Notifications,
                    contentDescription = stringResource(Res.string.permission_notification_title),
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        onClick = onNotificationPermissionDenyRequest,
                    ) {
                        Icon(Icons.Default.Close, stringResource(Res.string.permission_deny))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.permission_deny))
                    }
                    if (notificationPermissionResult == NotificationPermissionResult.NotAllowed) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            onClick = { permissionHelper.openSettings() },
                        ) {
                            Icon(Icons.Default.Settings, stringResource(Res.string.permission_settings))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(Res.string.permission_settings))
                        }
                    } else {
                        OutlinedButton(
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            onClick = onNotificationPermissionRequest,
                        ) {
                            Icon(Icons.Default.Security, stringResource(Res.string.permission_grant))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(Res.string.permission_grant))
                        }
                    }
                }
            }
        }

        val activeLendings = lendings?.filter { it.status() !in listOf(Lending.Status.MEMORY_SUBMITTED, Lending.Status.COMPLETE) }
        if (!activeLendings.isNullOrEmpty()) {
            item("active_lendings_header") {
                Text(
                    text = stringResource(Res.string.home_lendings),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxWidth().padding(horizontal = 8.dp),
                )
            }
            items(activeLendings, key = { it.id }, contentType = { "active-lending" }) { lending ->
                LendingItem(
                    lending,
                    snackbarHostState,
                    memoryUploadProgress,
                    onMemorySubmitted = { onMemorySubmitted(lending, it) },
                    onMemoryEditorRequested = { onMemoryEditorRequested(lending) },
                    onCancelLendingRequest = { onCancelLendingRequest(lending) }
                )
            }
        }

        item("lending_header") {
            Text(
                text = stringResource(Res.string.home_lending),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxWidth().padding(horizontal = 8.dp),
            )
        }

        if (profile.lendingUser == null) {
            item("lending_not_signed_up", span = StaggeredGridItemSpan.FullLine) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).padding(horizontal = 16.dp)) {
                        Icon(Icons.Default.Badge, null, modifier = Modifier.padding(end = 16.dp))
                        Text(
                            text = stringResource(Res.string.lending_signup_required),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedButton(
                        onClick = onLendingSignUpRequested,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp).padding(horizontal = 16.dp)
                    ) { Text(stringResource(Res.string.lending_signup_action)) }
                }
            }
        } else if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium) {
            item("lending_items", span = StaggeredGridItemSpan.FullLine) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val groupedItems = inventoryItems?.groupBy { it.type }

                    items(
                        items = groupedItems?.toList().orEmpty(),
                        key = { (type) -> type.id },
                        contentType = { "lending-item-large" },
                    ) { (type, items) ->
                        val selectedAmount = shoppingList[type.id] ?: 0

                        LendingItem_Large(
                            type = type,
                            items = items,
                            selectedAmount = selectedAmount,
                            onAddItemToShoppingListRequest = { onAddItemToShoppingListRequest(type) },
                            onRemoveItemFromShoppingListRequest = { onRemoveItemFromShoppingListRequest(type) },
                            onClick = { showingItemTypeDetails = type }
                        )
                    }
                }
            }
        } else {
            val groupedItems = inventoryItems?.groupBy { it.type }?.toList()
            val categories = inventoryItems?.mapNotNull { it.type.category }?.toSet().orEmpty().toList()

            item("categories_chips") {
                LazyRow(modifier = Modifier.fillMaxWidth()) {
                    items(
                        items = categories,
                        key = { it },
                        contentType = { "category-chip" },
                    ) { category ->
                        val isSelected = selectedCategories.contains(category)
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategories = if (isSelected) {
                                    selectedCategories - category
                                } else {
                                    selectedCategories + category
                                }
                            },
                            label = { Text(category) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
            }

            items(
                items = groupedItems?.toList().orEmpty()
                    .filter { (type) ->
                        if (selectedCategories.isNotEmpty())
                            type.category in selectedCategories
                        else
                            true
                    },
                key = { (type) -> type.id },
                contentType = { "lending-item-small" },
            ) { (type, items) ->
                val selectedAmount = shoppingList[type.id] ?: 0

                LendingItem_Small(
                    type = type,
                    items = items,
                    selectedAmount = selectedAmount,
                    onAddItemToShoppingListRequest = { onAddItemToShoppingListRequest(type) },
                    onRemoveItemFromShoppingListRequest = { onRemoveItemFromShoppingListRequest(type) },
                    onClick = { showingItemTypeDetails = type }
                )
            }
        }

        val oldLendings = lendings?.filter { it.status() in listOf(Lending.Status.MEMORY_SUBMITTED, Lending.Status.COMPLETE) }.orEmpty()
        if (oldLendings.isNotEmpty()) {
            item("past_lendings_header", span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = stringResource(Res.string.home_past_lendings),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxWidth().padding(horizontal = 8.dp),
                )
            }
            items(items = oldLendings, key = { it.id }, contentType = { "old-lending" }) { lending ->
                OldLendingItem(lending)
            }
        }
    }
}

@Composable
fun LendingItem_Small(
    type: InventoryItemType,
    items: List<ReferencedInventoryItem>,
    selectedAmount: Int,
    onAddItemToShoppingListRequest: () -> Unit,
    onRemoveItemFromShoppingListRequest: () -> Unit,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.width(300.dp).padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val imageFile by type.rememberImageFile()
                AsyncByteImage(
                    bytes = imageFile,
                    contentDescription = type.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier.weight(3f)
            ) {
                Text(
                    text = "${type.displayName} (${items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedVisibility(
                        visible = selectedAmount > 0
                    ) {
                        Surface(
                            modifier = Modifier.padding(4.dp).zIndex(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Text(
                                text = selectedAmount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = selectedAmount > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        ElevatedButton(
                            contentPadding = PaddingValues(0.dp),
                            onClick = { onRemoveItemFromShoppingListRequest() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    ElevatedButton(
                        contentPadding = PaddingValues(0.dp),
                        enabled = selectedAmount < items.size,
                        onClick = { onAddItemToShoppingListRequest() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LendingItem_Large(
    type: InventoryItemType,
    items: List<ReferencedInventoryItem>,
    selectedAmount: Int,
    onAddItemToShoppingListRequest: () -> Unit,
    onRemoveItemFromShoppingListRequest: () -> Unit,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.width(300.dp).padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (selectedAmount > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp).zIndex(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = selectedAmount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).zIndex(1f),
            ) {
                AnimatedVisibility(
                    visible = selectedAmount > 0,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    ElevatedButton(
                        contentPadding = PaddingValues(0.dp),
                        onClick = { onRemoveItemFromShoppingListRequest() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                ElevatedButton(
                    contentPadding = PaddingValues(0.dp),
                    enabled = selectedAmount < items.size,
                    onClick = { onAddItemToShoppingListRequest() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            val imageFile by type.rememberImageFile()
            AsyncByteImage(
                bytes = imageFile,
                contentDescription = type.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            text = "${type.displayName} (${items.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun OldLendingItem(lending: ReferencedLending) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        LendingDetailsDialog(
            lending = lending,
            onCancelRequest = {},
            memoryUploadProgress = null,
            onMemorySubmitted = null,
            onMemoryEditorRequested = null,
            onDismissRequest = { showingDialog = false },
        )
    }

    OutlinedCard(
        onClick = { showingDialog = true },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        Text(
            text = "${lending.from} → ${lending.to}",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            textAlign = TextAlign.Center,
        )
        val groupedItems = lending.items.groupBy { it.type }
        Text(
            text = pluralStringResource(
                Res.plurals.lending_details_item_row,
                lending.items.size,
                lending.items.size,
                groupedItems.map { (type, items) -> "${type.displayName} (${items.size})" }.joinToString()
            ),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )
        Text(
            text = lending.id.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LendingItem(
    lending: ReferencedLending,
    snackbarHostState: SnackbarHostState,
    memoryUploadProgress: Pair<Long, Long>?,
    onMemorySubmitted: (PlatformFile) -> Job,
    onMemoryEditorRequested: () -> Unit,
    onCancelLendingRequest: () -> Job,
) {
    val scope = rememberCoroutineScope()

    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        LendingDetailsDialog(
            lending = lending,
            onCancelRequest = { onCancelLendingRequest().invokeOnCompletion { showingDialog = false } },
            memoryUploadProgress = memoryUploadProgress,
            onMemorySubmitted = onMemorySubmitted,
            onMemoryEditorRequested = {
                showingDialog = false
                onMemoryEditorRequested()
            },
            onDismissRequest = { showingDialog = false },
        )
    }

    OutlinedCard(
        onClick = { showingDialog = true },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        ) {
            Icon(Icons.Default.CalendarStartOutline, null)
            Text(
                text = lending.from.toString(),
                modifier = Modifier.weight(1f).padding(start = 4.dp, end = 12.dp)
            )
            Icon(Icons.Default.CalendarEndOutline, null)
            Text(
                text = lending.to.toString(),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }
        val groupedItems = lending.items.groupBy { it.type }
        Text(
            text = pluralStringResource(
                Res.plurals.home_lending_items,
                lending.items.size,
                lending.items.size,
                "\n" + groupedItems.map { (type, items) -> "- ${type.displayName} x${items.size}" }.joinToString("\n")
            ),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )
        val badge = when (lending.status()) {
            Lending.Status.REQUESTED -> Triple(Res.string.lending_not_confirmed, Icons.Default.Pending, Res.string.lending_not_confirmed_message)
            Lending.Status.CONFIRMED -> Triple(Res.string.lending_pending_pickup, Icons.Default.Inventory2, Res.string.lending_pending_pickup_message)
            Lending.Status.TAKEN -> Triple(Res.string.lending_pending_return, Icons.AutoMirrored.Default.AssignmentReturn, Res.string.lending_pending_return_message)
            Lending.Status.RETURNED -> Triple(Res.string.lending_pending_memory, Icons.AutoMirrored.Default.NoteAdd, Res.string.lending_pending_memory_message)
            else -> null
        }
        badge?.let { (labelRes, icon, message) ->
            AssistChip(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(getString(message))
                    }
                },
                label = { Text(stringResource(labelRes)) },
                leadingIcon = { Icon(icon, stringResource(labelRes)) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}
