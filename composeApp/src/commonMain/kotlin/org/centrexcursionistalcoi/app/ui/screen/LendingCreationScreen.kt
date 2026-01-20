package org.centrexcursionistalcoi.app.ui.screen

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import com.diamondedge.logging.logging
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.referenced
import org.centrexcursionistalcoi.app.exception.CannotAllocateEnoughItemsException
import org.centrexcursionistalcoi.app.exception.NoValidInsuranceForPeriodException
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.ui.data.FutureSelectableDates
import org.centrexcursionistalcoi.app.ui.data.RangeSelectableDates
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Add
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.AddCircle
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.ArrowBack
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Delete
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Error
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Info
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Remove
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Undo
import org.centrexcursionistalcoi.app.ui.reusable.CardWithIcon
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.form.DatePickerFormField
import org.centrexcursionistalcoi.app.ui.utils.unknown
import org.centrexcursionistalcoi.app.utils.toUuid
import org.centrexcursionistalcoi.app.viewmodel.LendingCreationViewModel
import org.jetbrains.compose.resources.stringResource

private val log = logging()

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
            log.i { "Shopping list is empty, going back..." }
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
    inventoryItemTypes: List<ReferencedInventoryItemType>?,
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
                        Icon(MaterialSymbols.ArrowBack, stringResource(Res.string.back))
                    }
                },
                actions = {
                    if (isShoppingListDirty) {
                        IconButton(
                            onClick = onResetShoppingList,
                            content = {
                                Icon(
                                    imageVector = MaterialSymbols.Undo,
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
                    Icon(MaterialSymbols.AddCircle, null)
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
                    val items = allocatedItems?.filter { it.type.id == typeId }
                    val itemError = errors
                        ?.filterIsInstance<CannotAllocateEnoughItemsException>()
                        ?.find { it.itemTypeId == typeId }
                    val itemAllocated = itemError == null && from != null && to != null && allocatedItems != null && !items.isNullOrEmpty()

                    val backgroundColor = if (itemError != null)
                        MaterialTheme.colorScheme.errorContainer
                    else if (itemAllocated)
                        Color(0xFFBBD0AE)
                    else
                        Color.Transparent
                    val contentColor = if (itemError != null)
                        MaterialTheme.colorScheme.onErrorContainer
                    else if (itemAllocated)
                        Color(0xFF2A441F)
                    else
                        Color.Unspecified

                    ListItem(
                        headlineContent = { Text("${type?.displayName ?: unknown()} ($amount)") },
                        supportingContent = if (!itemAllocated) {
                            {
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
                                        else -> stringResource(Res.string.lending_creation_items_unknown_error)
                                    }
                                )
                            }
                        } else null,
                        trailingContent = {
                            val availableAmount = inventoryItems.count { it.type.id == typeId }
                            val canAddMore = amount < availableAmount

                            Row {
                                if (itemError != null && itemError.availableItems.isNullOrEmpty()) {
                                    AssistChip(
                                        onClick = { onRemoveItemTypeFromShoppingList(typeId) },
                                        label = { Icon(MaterialSymbols.Delete, null) },
                                        modifier = Modifier.padding(end = 4.dp),
                                    )
                                } else {
                                    AssistChip(
                                        onClick = { onRemoveItemFromShoppingList(typeId) },
                                        label = { Icon(MaterialSymbols.Remove, null) },
                                        modifier = Modifier.padding(end = 4.dp),
                                    )
                                    AssistChip(
                                        enabled = canAddMore,
                                        onClick = { onAddItemToShoppingList(typeId) },
                                        label = { Icon(MaterialSymbols.Add, null) },
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = backgroundColor,
                            headlineColor = contentColor,
                            supportingColor = contentColor,
                        ),
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp).clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
            item("warning") {
                CardWithIcon(
                    icon = MaterialSymbols.Info,
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
                            icon = MaterialSymbols.Error,
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

                val hasNoValidInsuranceForPeriodException = errors.any { it is NoValidInsuranceForPeriodException }
                if (hasNoValidInsuranceForPeriodException) {
                    item(
                        key = "error-insurance"
                    ) {
                        CardWithIcon(
                            icon = MaterialSymbols.Error,
                            title = stringResource(Res.string.lending_creation_error_insurance_title),
                            message = stringResource(Res.string.lending_creation_error_insurance),
                            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 12.dp).padding(horizontal = 12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        )
                    }
                }

                itemsIndexed(
                    errors.filterNot { it is CannotAllocateEnoughItemsException || it is NoValidInsuranceForPeriodException },
                    { i, _ -> "error-$i" }
                ) { _, error ->
                    CardWithIcon(
                        icon = MaterialSymbols.Error,
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
            item("spacer") { Spacer(Modifier.height(64.dp)) }
        }
    }
}

private val previewType = InventoryItemType(
    id = "3b69c54e-3465-4a1d-a194-29dc64058e4a".toUuid(),
    displayName = "Test Item",
    description = null,
    categories = null,
    department = null,
    image = null,
).referenced(emptyList())

private val previewItem = InventoryItem(
    id = "8c9ccff9-ec17-4918-9327-08f7de3576d3".toUuid(),
    variation = null,
    type = previewType.id,
    nfcId = null,
    manufacturerTraceabilityCode = null,
).referenced(previewType)

@Preview
@Composable
fun LendingCreationScreen_Allocated_Preview() {
    LendingCreationScreen(
        shoppingList = mapOf(previewType.id to 1),
        isShoppingListDirty = false,
        onAddItemToShoppingList = {},
        onRemoveItemFromShoppingList = {},
        onRemoveItemTypeFromShoppingList = {},
        onResetShoppingList = {},
        inventoryItems = listOf(previewItem),
        inventoryItemTypes = listOf(previewType),
        from = LocalDate(2025, 10, 11),
        onFromChange = {},
        to = LocalDate(2025, 10, 15),
        onToChange = {},
        errors = null,
        allocatedItems = listOf(previewItem),
        onCreateLendingRequest = {},
        onBackRequested = {},
    )
}
