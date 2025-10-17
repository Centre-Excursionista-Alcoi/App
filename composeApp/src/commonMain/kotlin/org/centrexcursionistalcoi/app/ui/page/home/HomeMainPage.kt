package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.dialog.InventoryItemTypeDetailsDialog
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage

@Composable
fun HomeMainPage(
    windowSizeClass: WindowSizeClass,
    profile: ProfileResponse,
    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItems: List<InventoryItem>?,
    shoppingList: Map<Uuid, Int>,
    onAddItemToShoppingListRequest: (InventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (InventoryItemType) -> Unit,
) {
    AdaptiveVerticalGrid(
        windowSizeClass,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
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
        // TODO: Check whether the user has signed up for lending
        stickyHeader {
            Text(
                text = "Material Lending"
            )
        }
        item("lending_items", span = { GridItemSpan(maxLineSpan) }) {
            var showingDetails by remember { mutableStateOf<InventoryItemType?>(null) }
            showingDetails?.let { type ->
                InventoryItemTypeDetailsDialog(type) { showingDetails = null }
            }

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

                    OutlinedCard(
                        enabled = type != null,
                        onClick = { showingDetails = type },
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
            }
        }
    }
}
