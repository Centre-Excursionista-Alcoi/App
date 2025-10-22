package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.ui.data.FutureSelectableDates
import org.centrexcursionistalcoi.app.ui.reusable.form.DatePickerFormField
import org.centrexcursionistalcoi.app.viewmodel.LendingCreationViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingCreationScreen(
    shoppingList: ShoppingList,
    model: LendingCreationViewModel = viewModel { LendingCreationViewModel(shoppingList) },
    onLendingCreated: () -> Unit,
    onBackRequested: () -> Unit,
) {
    val allocatedItems by model.allocatedItems.collectAsState()
    val inventoryItems by model.inventoryItems.collectAsState()
    val inventoryItemTypes by model.inventoryItemTypes.collectAsState()
    val from by model.from.collectAsState()
    val to by model.to.collectAsState()
    val error by model.error.collectAsState()

    LendingCreationScreen(
        shoppingList = shoppingList,
        inventoryItems = inventoryItems,
        inventoryItemTypes = inventoryItemTypes,
        allocatedItems = allocatedItems,
        from = from,
        onFromChange = model::setFrom,
        to = to,
        onToChange = model::setTo,
        error = error,
        onCreateLendingRequest = {
            model.createLending(onLendingCreated)
        },
        onBackRequested = onBackRequested,
    )
}

// TODO: Localize
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingCreationScreen(
    shoppingList: ShoppingList,
    inventoryItems: List<ReferencedInventoryItem>?,
    inventoryItemTypes: List<InventoryItemType>?,
    from: LocalDate?,
    onFromChange: (LocalDate) -> Unit,
    to: LocalDate?,
    onToChange: (LocalDate) -> Unit,
    error: Throwable?,
    allocatedItems: List<ReferencedInventoryItem>?,
    onCreateLendingRequest: () -> Unit,
    onBackRequested: () -> Unit,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lending Confirmation") },
                navigationIcon = {
                    IconButton(
                        onClick = onBackRequested
                    ) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, stringResource(Res.string.back))
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
                    Text("Create Lending")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item("dates") {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    DatePickerFormField(
                        value = from,
                        onValueChange = onFromChange,
                        label = "Start Date",
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        selectableDates = FutureSelectableDates(today),
                    )
                    DatePickerFormField(
                        value = to,
                        onValueChange = onToChange,
                        label = "End Date",
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        selectableDates = FutureSelectableDates(from ?: today),
                    )
                }
            }
            if (inventoryItemTypes == null || inventoryItems == null) {
                item("loading") {
                    Text(
                        text = "Loading...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            } else {
                items(shoppingList.toList()) { (typeId, amount) ->
                    val type = inventoryItemTypes.find { it.id == typeId }

                    ListItem(
                        headlineContent = { Text("${type?.displayName ?: "N/A"} ($amount)") },
                        supportingContent = {
                            val items = allocatedItems?.filter { it.type == typeId }
                            Text(
                                text = when {
                                    from == null || to == null -> "Select dates to allocate items"
                                    allocatedItems == null -> "Allocating..."
                                    items.isNullOrEmpty() -> "No items allocated"
                                    else -> "Allocated items:\n- ${items.joinToString("\n- ") { it.id.toString() }}"
                                }
                            )
                        }
                    )
                }
            }
            item("warning") {
                OutlinedCard(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 12.dp).padding(horizontal = 12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Icon(Icons.Default.Info, null)
                        Text("Attention", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        text = "You will reserve the allocated items for the selected dates, but the request has to be approved by an administrator before the lending is complete.",
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = "Once an admin confirms the lending, you will receive a notification, and will be able to schedule the pickup of the items.",
                        modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
                    )
                }
            }
            if (error != null) item("error") {
                OutlinedCard(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Icon(Icons.Default.Error, null)
                        Text("Error", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        text = error.message ?: "An unknown error occurred while allocating items.",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            item("spacer") { Spacer(Modifier.height(48.dp)) }
        }
    }
}
