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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.dialog.InventoryItemTypeDetailsDialog
import org.centrexcursionistalcoi.app.ui.dialog.LendingDetailsDialog
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage

@Composable
fun HomeMainPage(
    windowSizeClass: WindowSizeClass,
    profile: ProfileResponse,
    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItems: List<InventoryItem>?,
    lendings: List<Lending>?,
    shoppingList: Map<Uuid, Int>,
    onAddItemToShoppingListRequest: (InventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (InventoryItemType) -> Unit,
    onCancelLendingRequest: (Lending) -> Job,
) {
    var showingItemTypeDetails by remember { mutableStateOf<InventoryItemType?>(null) }
    showingItemTypeDetails?.let { type ->
        InventoryItemTypeDetailsDialog(type) { showingItemTypeDetails = null }
    }

    var showingLendingDetails by remember { mutableStateOf<Lending?>(null) }
    showingLendingDetails?.let { lending ->
        LendingDetailsDialog(
            lending,
            inventoryItemTypes.orEmpty(),
            onCancelRequest = {
                onCancelLendingRequest(lending).invokeOnCompletion {
                    showingLendingDetails = null
                }
            }
        ) { showingItemTypeDetails = null }
    }

    AdaptiveVerticalGrid(
        windowSizeClass,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium) {
            item("welcome_message", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Welcome back ${profile.username}!",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 24.dp)
                )
            }
        }

        val nonReturnedLendings = lendings?.filter { !it.isReturned() }
        if (!nonReturnedLendings.isNullOrEmpty()) {
            stickyHeader {
                Text(
                    text = "Your Lendings"
                )
            }
            items(nonReturnedLendings) { lending ->
                OutlinedCard(
                    onClick = { showingLendingDetails = lending },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("From: ${lending.from}", style = MaterialTheme.typography.titleMedium)
                    Text("To: ${lending.to}", style = MaterialTheme.typography.titleMedium)
                    Text("Items:", style = MaterialTheme.typography.titleMedium)
                    val items = lending.items.groupBy { it.type }
                    for ((typeId, items) in items) {
                        val type = inventoryItemTypes?.find { it.id == typeId }
                        Text(
                            text = "- ${type?.displayName ?: "N/A"} x${items.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!lending.confirmed) {
                        Text("Not confirmed")
                    } else if (!lending.taken) {
                        Text("Confirmed, not taken yet")
                    } else if (!lending.returned) {
                        Text("Not returned")
                    } else {
                        Text("Returned")
                    }
                }
            }
        }

        // TODO: Check whether the user has signed up for lending
        stickyHeader {
            Text(
                text = "Material Lending",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
        }

        if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium) {
            item("lending_items", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val groupedItems = inventoryItems?.groupBy { it.type }

                    items(
                        items = groupedItems?.toList().orEmpty(),
                        key = { (typeId) -> typeId }
                    ) { (typeId, items) ->
                        val type = inventoryItemTypes?.find { type -> type.id == typeId }
                        val selectedAmount = shoppingList[typeId] ?: 0

                        LendingItem_Large(
                            type = type,
                            items = items,
                            selectedAmount = selectedAmount,
                            onAddItemToShoppingListRequest = onAddItemToShoppingListRequest,
                            onRemoveItemFromShoppingListRequest = onRemoveItemFromShoppingListRequest,
                            onClick = { showingItemTypeDetails = type }
                        )
                    }
                }
            }
        } else {
            val groupedItems = inventoryItems?.groupBy { it.type }?.toList()

            items(
                items = groupedItems?.toList().orEmpty(),
                key = { (typeId) -> typeId }
            ) { (typeId, items) ->
                val type = inventoryItemTypes?.find { type -> type.id == typeId }
                val selectedAmount = shoppingList[typeId] ?: 0

                LendingItem_Small(
                    type = type,
                    items = items,
                    selectedAmount = selectedAmount,
                    onAddItemToShoppingListRequest = onAddItemToShoppingListRequest,
                    onRemoveItemFromShoppingListRequest = onRemoveItemFromShoppingListRequest,
                    onClick = { showingItemTypeDetails = type }
                )
            }
        }
    }
}

@Composable
fun LendingItem_Small(
    type: InventoryItemType?,
    items: List<InventoryItem>,
    selectedAmount: Int,
    onAddItemToShoppingListRequest: (InventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (InventoryItemType) -> Unit,
    onClick: () -> Unit
) {
    OutlinedCard(
        enabled = type != null,
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
                if (type == null) CircularProgressIndicator()
                else {
                    val imageFile by type.rememberImageFile()
                    AsyncByteImage(
                        bytes = imageFile,
                        contentDescription = type.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(3f)
            ) {
                Text(
                    text = type?.displayName?.let { "$it (${items.size})" } ?: "Loading...",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        visible = selectedAmount > 0 && type != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        ElevatedButton(
                            contentPadding = PaddingValues(0.dp),
                            enabled = type != null,
                            onClick = { onRemoveItemFromShoppingListRequest(type!!) },
                            modifier = Modifier.fillMaxWidth(),
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
                        enabled = type != null && selectedAmount < items.size,
                        onClick = { onAddItemToShoppingListRequest(type!!) },
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
    type: InventoryItemType?,
    items: List<InventoryItem>,
    selectedAmount: Int,
    onAddItemToShoppingListRequest: (InventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (InventoryItemType) -> Unit,
    onClick: () -> Unit
) {
    OutlinedCard(
        enabled = type != null,
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
                    visible = selectedAmount > 0 && type != null,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    ElevatedButton(
                        contentPadding = PaddingValues(0.dp),
                        enabled = type != null,
                        onClick = { onRemoveItemFromShoppingListRequest(type!!) }
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
                    enabled = type != null && selectedAmount < items.size,
                    onClick = { onAddItemToShoppingListRequest(type!!) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (type == null) CircularProgressIndicator()
            else {
                val imageFile by type.rememberImageFile()
                AsyncByteImage(
                    bytes = imageFile,
                    contentDescription = type.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Text(
            text = type?.displayName?.let { "$it (${items.size})" } ?: "Loading...",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )
    }
}
