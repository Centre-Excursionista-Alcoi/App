package ui.pages.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import backend.database.InventoryItem
import ui.reusable.list.InventoryItemCard

@Composable
fun RentalPage(items: List<InventoryItem>?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (items != null) {
            for (item in items) {
                InventoryItemCard(item)
            }
        } else {
            InventoryItemCard(null)
            InventoryItemCard(null)
            InventoryItemCard(null)
        }
    }
}
