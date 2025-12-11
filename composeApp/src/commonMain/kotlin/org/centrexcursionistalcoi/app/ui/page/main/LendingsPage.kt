package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.*
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.animation.sharedBounds
import org.centrexcursionistalcoi.app.ui.icons.material.CalendarEndOutline
import org.centrexcursionistalcoi.app.ui.icons.material.CalendarStartOutline
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.*
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveTabRow
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.TabData
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@Composable
fun LendingsPage(
    windowSizeClass: WindowSizeClass,

    profile: ProfileResponse,
    onAddInsuranceRequested: () -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,
    onItemTypeDetailsRequested: (ReferencedInventoryItemType) -> Unit,

    lendings: List<ReferencedLending>?,
    onLendingSignUpRequested: () -> Unit,

    shoppingList: Map<Uuid, Int>,
    onAddItemToShoppingListRequest: (ReferencedInventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (ReferencedInventoryItemType) -> Unit,
) {
    val departments = remember(inventoryItems) { inventoryItems?.mapNotNull { it.type.department }?.toSet().orEmpty().toList() }
    val itemsWithoutDepartmentExist = remember(inventoryItems) { inventoryItems?.any { it.type.department == null } == true }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { departments.size + (if (itemsWithoutDepartmentExist) 1 else 0) }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        AdaptiveTabRow(
            selectedTabIndex = pagerState.currentPage,
            tabs = departments.map { TabData(it.displayName) } +
                    if (itemsWithoutDepartmentExist)
                        listOf(TabData(stringResource(Res.string.lending_category_without_department)))
                    else
                        emptyList(),
            onTabSelected = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { page ->
            // if null, show items without department
            val department: Department? = departments.getOrNull(page)

            LendingsPage_Content(
                windowSizeClass = windowSizeClass,
                profile = profile,
                onAddInsuranceRequested = onAddInsuranceRequested,
                inventoryItems = inventoryItems?.filter { it.type.department == department },
                onItemTypeDetailsRequested = onItemTypeDetailsRequested,
                lendings = lendings,
                onLendingSignUpRequested = onLendingSignUpRequested,
                shoppingList = shoppingList,
                onAddItemToShoppingListRequest = onAddItemToShoppingListRequest,
                onRemoveItemFromShoppingListRequest = onRemoveItemFromShoppingListRequest,
            )
        }
    }
}

@Composable
private fun LendingsPage_Content(
    windowSizeClass: WindowSizeClass,

    profile: ProfileResponse,
    onAddInsuranceRequested: () -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,
    onItemTypeDetailsRequested: (ReferencedInventoryItemType) -> Unit,

    lendings: List<ReferencedLending>?,
    onLendingSignUpRequested: () -> Unit,

    shoppingList: Map<Uuid, Int>,
    onAddItemToShoppingListRequest: (ReferencedInventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (ReferencedInventoryItemType) -> Unit,
) {
    val scrollState = rememberLazyGridState()
    val isRegisteredForLendings = profile.lendingUser != null
    val hasAnInsurance = profile.activeInsurances().isNotEmpty()

    var selectedCategories by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(lendings) {
        if (!lendings.isNullOrEmpty()) {
            // Scroll to top when lendings change
            scrollState.animateScrollToItem(0)
        }
    }

    AdaptiveVerticalGrid(
        windowSizeClass,
        state = scrollState,
        gridMinSize = 200.dp,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        item(key = "top_spacer", contentType = "spacer") { Modifier.height(16.dp) }

        if (!isRegisteredForLendings) {
            item("lending_not_signed_up", span = { GridItemSpan(maxLineSpan) }) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).padding(horizontal = 16.dp)) {
                        Icon(MaterialSymbols.Badge, null, modifier = Modifier.padding(end = 16.dp))
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
        } else if (!hasAnInsurance) {
            item("lending_no_insurance", span = { GridItemSpan(maxLineSpan) }) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).padding(horizontal = 16.dp)) {
                        Icon(MaterialSymbols.HealthAndSafety, null, modifier = Modifier.padding(end = 16.dp))
                        Text(
                            text = stringResource(Res.string.lending_no_active_insurances_title),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = stringResource(Res.string.lending_no_active_insurances_message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                    OutlinedButton(
                        onClick = onAddInsuranceRequested,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp).padding(horizontal = 16.dp)
                    ) { Text(stringResource(Res.string.lending_no_active_insurances_action)) }
                }
            }
        } else {
            val groupedItems = inventoryItems
                ?.groupBy { it.type }
                .orEmpty()
                .toList()
                .sortedBy { (type) -> type.displayName }
                .filter { (type) ->
                    if (selectedCategories.isNotEmpty())
                        type.categories.orEmpty().any { it in selectedCategories }
                    else
                        true
                }
            val categories = inventoryItems?.flatMap { it.type.categories.orEmpty() }?.toSet().orEmpty().toList()

            stickyHeader("lending_header") {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.home_lending),
                        style = MaterialTheme.typography.titleLarge,
                    )
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
            }

            if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium) {
                items(
                    items = groupedItems,
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
                        onClick = { onItemTypeDetailsRequested(type) },
                    )
                }
            } else {
                items(
                    items = groupedItems,
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
                        onClick = { onItemTypeDetailsRequested(type) },
                    )
                }
            }
        }

        item(key = "bottom_spacer", contentType = "spacer") { Modifier.height(16.dp) }
    }
}

@Composable
fun LendingItem_Small(
    type: ReferencedInventoryItemType,
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
                    modifier = Modifier.fillMaxSize().sharedBounds("type-${type.id}-image"),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier.weight(3f)
            ) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.sharedBounds("type-${type.id}-display-name").weight(1f),
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Text(text = items.size.toString())
                    }
                }
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
                                imageVector = MaterialSymbols.Remove,
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
                            imageVector = MaterialSymbols.Add,
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
    type: ReferencedInventoryItemType,
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
                            imageVector = MaterialSymbols.Remove,
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
                        imageVector = MaterialSymbols.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            val imageFile by type.rememberImageFile()
            AsyncByteImage(
                bytes = imageFile,
                contentDescription = type.displayName,
                modifier = Modifier.fillMaxSize().sharedBounds("type-${type.id}-image"),
                contentScale = ContentScale.Crop,
            )
        }
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.sharedBounds("type-${type.id}-display-name").weight(1f),
            )
            Badge(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(text = items.size.toString())
            }
        }
    }
}

@Composable
fun OldLendingItem(lending: ReferencedLending, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
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

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun LendingItem(
    lending: ReferencedLending,
    isActive: Boolean = false,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified,
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified,
        ),
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        ) {
            Icon(MaterialSymbols.CalendarStartOutline, null)
            Text(
                text = lending.from.toString(),
                modifier = Modifier.weight(1f).padding(start = 4.dp, end = 12.dp)
            )
            Icon(MaterialSymbols.CalendarEndOutline, null)
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
            Lending.Status.REQUESTED -> Pair(Res.string.lending_not_confirmed, MaterialSymbols.Pending)
            Lending.Status.CONFIRMED -> Pair(Res.string.lending_pending_pickup, MaterialSymbols.Inventory2)
            Lending.Status.TAKEN -> {
                val returnStarted = lending.receivedItems.isNotEmpty()
                if (returnStarted) {
                    Pair(Res.string.lending_pending_return_partial, MaterialSymbols.AssignmentReturn)
                } else {
                    Pair(Res.string.lending_pending_return, MaterialSymbols.AssignmentReturn)
                }
            }

            Lending.Status.RETURNED -> Pair(Res.string.lending_pending_memory, MaterialSymbols.NoteAdd)
            else -> null
        }
        badge?.let { (labelRes, icon) ->
            AssistChip(
                onClick = onClick,
                label = { Text(stringResource(labelRes)) },
                leadingIcon = { Icon(icon, stringResource(labelRes)) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}
