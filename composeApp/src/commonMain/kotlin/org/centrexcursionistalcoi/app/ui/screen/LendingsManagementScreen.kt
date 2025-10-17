package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.ListCard
import org.centrexcursionistalcoi.app.viewmodel.LendingsManagementViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingsManagementScreen(
    model: LendingsManagementViewModel = viewModel { LendingsManagementViewModel() },
    onBack: () -> Unit
) {
    val inventoryItemTypes by model.inventoryItemTypes.collectAsState()
    val lendings by model.lendings.collectAsState()
    val users by model.users.collectAsState()

    LendingsManagementScreen(
        inventoryItemTypes = inventoryItemTypes,
        lendings = lendings,
        users = users,
        onConfirmRequest = model::confirm,
        onPickupRequest = model::pickup,
        onReturnRequest = model::`return`,
        onBack = onBack
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingsManagementScreen(
    inventoryItemTypes: List<InventoryItemType>?,
    lendings: List<Lending>?,
    users: List<UserData>?,
    onConfirmRequest: (Lending) -> Unit,
    onPickupRequest: (Lending) -> Unit,
    onReturnRequest: (Lending) -> Unit,
    onBack: () -> Unit
) {
    val unconfirmedLendings = remember(lendings) { lendings?.filterNot { it.confirmed }.orEmpty() }
    val pendingPickupLendings = remember(lendings) { lendings?.filter { it.confirmed && !it.taken }.orEmpty() }
    val pendingReturnLendings = remember(lendings) { lendings?.filter { it.confirmed && it.taken && !it.returned }.orEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                title = { Text("Lendings") }
            )
        }
    ) { paddingValues ->
        val windowSizeClass = calculateWindowSizeClass()
        AdaptiveVerticalGrid(
            windowSizeClass = windowSizeClass,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (unconfirmedLendings.isNotEmpty()) item(key = "unconfirmed_lendings") {
                UnconfirmedLendingsCard(inventoryItemTypes, users, unconfirmedLendings, onConfirmRequest)
            }
            if (pendingPickupLendings.isNotEmpty()) item(key = "pending_pickup_lendings") {
                PendingPickupLendingsCard(inventoryItemTypes, users, pendingPickupLendings, onPickupRequest)
            }
            if (pendingReturnLendings.isNotEmpty()) item(key = "pending_return_lendings") {
                PendingReturnLendingsCard(inventoryItemTypes, users, pendingReturnLendings, onReturnRequest)
            }

            if (unconfirmedLendings.isEmpty() && pendingPickupLendings.isEmpty() && pendingReturnLendings.isEmpty()) {
                item(key = "no_lendings") {
                    Text(
                        text = stringResource(Res.string.management_no_lendings),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UnconfirmedLendingsCard(
    types: List<InventoryItemType>?,
    users: List<UserData>?,
    lendings: List<Lending>,
    onConfirmRequest: (Lending) -> Unit,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_unconfirmed_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            val user = users?.find { it.id == lending.userSub }
            Text("User: ${user?.username ?: "Unknown"}")
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        detailsDialogContent = { lending ->
            val user = users?.find { it.id == lending.userSub }
            val items = lending.items

            Text("User: ${user?.username ?: "Unknown"}")
            Text("Items:")
            for ((typeId, items) in items.groupBy { it.type }) {
                val type = types?.find { it.id == typeId }
                Text("- ${type?.displayName ?: "Unknown Type"}: ${items.size} unit(s)")
            }

            HorizontalDivider()

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onConfirmRequest(lending)
                    dismiss()
                }
            ) { Text("Confirm") }
        }
    )
}

@Composable
fun PendingPickupLendingsCard(
    types: List<InventoryItemType>?,
    users: List<UserData>?,
    lendings: List<Lending>,
    onPickupRequest: (Lending) -> Unit,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_pending_pickup_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            val user = users?.find { it.id == lending.userSub }
            Text("User: ${user?.username ?: "Unknown"}")
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        detailsDialogContent = { lending ->
            val user = users?.find { it.id == lending.userSub }
            val items = lending.items

            Text("User: ${user?.username ?: "Unknown"}")
            Text("Items:")
            for ((typeId, items) in items.groupBy { it.type }) {
                val type = types?.find { it.id == typeId }
                Text("- ${type?.displayName ?: "Unknown Type"}: ${items.size} unit(s)")
            }

            HorizontalDivider()

            Text("When pressing the button above, you are confirming that the user has picked up the items, and making yourself responsible of having handed them over correctly.")
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onPickupRequest(lending)
                    dismiss()
                }
            ) { Text("Pickup") }
        }
    )
}


@Composable
fun PendingReturnLendingsCard(
    types: List<InventoryItemType>?,
    users: List<UserData>?,
    lendings: List<Lending>,
    onReturnRequest: (Lending) -> Unit,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_pending_return_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            val user = users?.find { it.id == lending.userSub }
            Text("User: ${user?.username ?: "Unknown"}")
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        detailsDialogContent = { lending ->
            val user = users?.find { it.id == lending.userSub }
            val items = lending.items

            Text("User: ${user?.username ?: "Unknown"}")
            Text("Items:")
            for ((typeId, items) in items.groupBy { it.type }) {
                val type = types?.find { it.id == typeId }
                Text("- ${type?.displayName ?: "Unknown Type"}: ${items.size} unit(s)")
            }

            HorizontalDivider()

            val givenByUser = users?.find { it.id == lending.givenBy }
            val givenAt = lending.givenAt?.toLocalDateTime(TimeZone.currentSystemDefault())
            Text("Given by: ${givenByUser?.username ?: "Unknown"} at ${givenAt ?: "Unknown time"}")

            HorizontalDivider()

            Text("When pressing the button above, you are confirming that the user has returned the items in good condition.")
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onReturnRequest(lending)
                    dismiss()
                }
            ) { Text("Return") }
        }
    )
}
