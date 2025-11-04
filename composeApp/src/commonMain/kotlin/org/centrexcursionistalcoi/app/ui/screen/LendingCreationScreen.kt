package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.aakira.napier.Napier
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.exception.CannotAllocateEnoughItemsException
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.ui.data.FutureSelectableDates
import org.centrexcursionistalcoi.app.ui.data.RangeSelectableDates
import org.centrexcursionistalcoi.app.ui.reusable.CardWithIcon
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.form.DatePickerFormField
import org.centrexcursionistalcoi.app.ui.utils.unknown
import org.centrexcursionistalcoi.app.viewmodel.LendingCreationViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingCreationScreen(
    originalShoppingList: ShoppingList,
    model: LendingCreationViewModel = viewModel { LendingCreationViewModel(originalShoppingList) },
    onLendingCreated: () -> Unit,
    onBackRequested: () -> Unit,
) {
    val allocatedItems by model.allocatedItems.collectAsState()
    val inventoryItems by model.inventoryItems.collectAsState()
    val inventoryItemTypes by model.inventoryItemTypes.collectAsState()
    val from by model.from.collectAsState()
    val to by model.to.collectAsState()
    val shoppingList by model.shoppingList.collectAsState()
    val errors by model.errors.collectAsState()

    val isDirty = originalShoppingList != shoppingList

    LaunchedEffect(shoppingList) {
        if (shoppingList.isEmpty()) {
            Napier.i { "Shopping list is empty, going back..." }
            onBackRequested()
        }
    }

    LendingCreationScreen(
        shoppingList = shoppingList,
        isShoppingListDirty = isDirty,
        onAddItemToShoppingList = model::addItemToShoppingList,
        onRemoveItemFromShoppingList = model::removeItemFromShoppingList,
        onRemoveItemTypeFromShoppingList = model::removeItemTypeFromShoppingList,
        onResetShoppingList = model::resetShoppingList,
        inventoryItems = inventoryItems,
        inventoryItemTypes = inventoryItemTypes,
        allocatedItems = allocatedItems,
        from = from,
        onFromChange = model::setFrom,
        to = to,
        onToChange = model::setTo,
        errors = errors,
        onCreateLendingRequest = {
            model.createLending(onLendingCreated)
        },
        onBackRequested = onBackRequested,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingCreationScreen(
    shoppingList: ShoppingList,
    isShoppingListDirty: Boolean,
    /**
     * One item of this type will be added to the shopping list.
     */
    onAddItemToShoppingList: (Uuid) -> Unit,
    /**
     * One item of this type will be removed from the shopping list.
     */
    onRemoveItemFromShoppingList: (Uuid) -> Unit,
    /**
     * All items of this type will be removed from the shopping list.
     */
    onRemoveItemTypeFromShoppingList: (Uuid) -> Unit,
    onResetShoppingList: () -> Unit,
    inventoryItems: List<ReferencedInventoryItem>?,
    inventoryItemTypes: List<InventoryItemType>?,
    from: LocalDate?,
    onFromChange: (LocalDate) -> Unit,
    to: LocalDate?,
    onToChange: (LocalDate) -> Unit,
    errors: List<Throwable>?,
    allocatedItems: List<ReferencedInventoryItem>?,
    onCreateLendingRequest: () -> Unit,
    onBackRequested: () -> Unit,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.lending_creation_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackRequested
                    ) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, stringResource(Res.string.back))
                    }
                },
                actions = {
                    if (isShoppingListDirty) {
                        IconButton(
                            onClick = onResetShoppingList,
                            content = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.Undo,
                                    contentDescription = stringResource(Res.string.lending_creation_reset)
                                )
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!allocatedItems.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onCreateLendingRequest,
                ) {
                    Icon(Icons.Default.AddCircleOutline, null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.lending_creation_action))
                }
            }
        }
    ) { paddingValues ->
        LazyColumnWidthWrapper(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item("dates") {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    DatePickerFormField(
                        value = from,
                        onValueChange = onFromChange,
                        label = stringResource(Res.string.lending_creation_from),
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        selectableDates = RangeSelectableDates(from = today, to = to),
                        onRangeSelected = { range ->
                            onFromChange(range.start)
                            onToChange(range.endInclusive)
                        },
                    )
                    DatePickerFormField(
                        value = to,
                        onValueChange = onToChange,
                        label = stringResource(Res.string.lending_creation_until),
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        selectableDates = FutureSelectableDates(from ?: today),
                        onRangeSelected = { range ->
                            onFromChange(range.start)
                            onToChange(range.endInclusive)
                        },
                    )
                }
            }
            if (inventoryItemTypes == null || inventoryItems == null) {
                item("loading") {
                    Text(
                        text = stringResource(Res.string.status_loading),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            } else {
                items(shoppingList.toList()) { (typeId, amount) ->
                    val type = inventoryItemTypes.find { it.id == typeId }
                    val itemError = errors
                        ?.filterIsInstance<CannotAllocateEnoughItemsException>()
                        ?.find { it.itemTypeId == typeId }

                    ListItem(
                        headlineContent = { Text("${type?.displayName ?: unknown()} ($amount)") },
                        supportingContent = {
                            val items = allocatedItems?.filter { it.type.id == typeId }
                            Column {
                                Text(
                                    text = when {
                                        itemError != null -> {
                                            val availableAmount = itemError.availableItems?.size ?: 0
                                            if (availableAmount > 0) {
                                                stringResource(Res.string.lending_creation_error_allocation_insufficient, availableAmount)
                                            } else {
                                                stringResource(Res.string.lending_creation_error_allocation_none)
                                            }
                                        }

                                        from == null || to == null -> stringResource(Res.string.lending_creation_select_dates)
                                        allocatedItems == null -> stringResource(Res.string.lending_creation_allocating)
                                        items.isNullOrEmpty() -> stringResource(Res.string.lending_creation_no_items_allocated)
                                        else -> stringResource(Res.string.lending_creation_items_allocated) + "\n" +
                                                items.joinToString("\n") { "- ${it.id}" }
                                    }
                                )
                                if (itemError != null && from != null && to != null && allocatedItems != null && !items.isNullOrEmpty()) {
                                    for (item in items) {
                                        Text(
                                            text = "- ${item.id}",
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            val availableAmount = inventoryItems.count { it.type.id == typeId }
                            val canAddMore = amount < availableAmount

                            Row {
                                if (itemError != null && itemError.availableItems.isNullOrEmpty()) {
                                    AssistChip(
                                        onClick = { onRemoveItemTypeFromShoppingList(typeId) },
                                        label = { Icon(Icons.Default.Delete, null) },
                                        modifier = Modifier.padding(end = 4.dp),
                                    )
                                } else {
                                    AssistChip(
                                        onClick = { onRemoveItemFromShoppingList(typeId) },
                                        label = { Icon(Icons.Default.Remove, null) },
                                        modifier = Modifier.padding(end = 4.dp),
                                    )
                                    AssistChip(
                                        enabled = canAddMore,
                                        onClick = { onAddItemToShoppingList(typeId) },
                                        label = { Icon(Icons.Default.Add, null) },
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (itemError != null) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                            headlineColor = if (itemError != null) MaterialTheme.colorScheme.onErrorContainer else Color.Unspecified,
                            supportingColor = if (itemError != null) MaterialTheme.colorScheme.onErrorContainer else Color.Unspecified,
                        ),
                        modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
            item("warning") {
                CardWithIcon(
                    icon = Icons.Default.Info,
                    title = stringResource(Res.string.lending_creation_warning_title),
                    message = stringResource(Res.string.lending_creation_warning_message),
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 12.dp).padding(horizontal = 12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
            }
            if (!errors.isNullOrEmpty()) {
                val allocationErrors = errors.filterIsInstance<CannotAllocateEnoughItemsException>()
                if (allocationErrors.isNotEmpty()) {
                    item(
                        key = "error-allocation"
                    ) {
                        CardWithIcon(
                            icon = Icons.Default.ErrorOutline,
                            title = stringResource(Res.string.lending_creation_error_allocation_title),
                            message = stringResource(Res.string.lending_creation_error_allocation),
                            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 12.dp).padding(horizontal = 12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        )
                    }
                }

                itemsIndexed(
                    errors.filterNot { it is CannotAllocateEnoughItemsException },
                    { i, _ -> "error-$i" }
                ) { _, error ->
                    CardWithIcon(
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(Res.string.lending_creation_error_title),
                        message = error.message?.let { message ->
                            stringResource(Res.string.lending_creation_error_message, message)
                        } ?: stringResource(Res.string.lending_creation_error_message_unknown),
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 12.dp).padding(horizontal = 12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    )
                }
            }
            item("spacer") { Spacer(Modifier.height(48.dp)) }
        }
    }
}
