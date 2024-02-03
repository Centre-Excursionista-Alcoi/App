package ui.pages.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import backend.database.InventoryItem
import ui.reusable.list.InventoryItemCard

@Composable
fun RentalPage(items: List<InventoryItem>?) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        if (items != null) {
            for (item in items) {
                InventoryItemCard(item, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
            }
        } else {
            InventoryItemCard(null, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
            InventoryItemCard(null, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
            InventoryItemCard(null, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
        }
    }
}
