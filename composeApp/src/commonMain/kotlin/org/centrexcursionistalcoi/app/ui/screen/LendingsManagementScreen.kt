package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.viewmodel.LendingsManagementViewModel

@Composable
fun LendingsManagementScreen(
    model: LendingsManagementViewModel = viewModel { LendingsManagementViewModel() },
    onBack: () -> Unit
) {
    val inventoryItemTypes by model.inventoryItemTypes.collectAsState()
    val inventoryItems by model.inventoryItems.collectAsState()
    val lendings by model.lendings.collectAsState()
    val users by model.users.collectAsState()

    LendingsManagementScreen(
        inventoryItemTypes = inventoryItemTypes,
        inventoryItems = inventoryItems,
        lendings = lendings,
        users = users,
        onBack = onBack
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingsManagementScreen(
    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItems: List<InventoryItem>?,
    lendings: List<Lending>?,
    users: List<UserData>?,
    onBack: () -> Unit
) {
    val unconfirmedLendings = remember(lendings) { lendings?.filterNot { it.confirmed }.orEmpty() }

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
            items(
                items = unconfirmedLendings,
                key = { it.id },
                contentType = { "unconfirmed-lending" }
            ) { lending ->
                val items = lending.items

                Column {
                    Text("Lending ID: ${lending.id}, User: ${users?.find { it.id == lending.userSub }?.username ?: "Unknown"}")
                    items.forEach { itemId ->
                        val item = inventoryItems?.find { it.id == itemId }
                        val itemType = inventoryItemTypes?.find { it.id == item?.type }
                        Text(" - Item: ${itemType?.displayName ?: "Unknown"}")
                    }
                }
            }
        }
    }
}
