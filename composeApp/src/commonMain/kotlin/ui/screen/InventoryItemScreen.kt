package ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import backend.data.database.InventoryItem

class InventoryItemScreen(
    private val item: InventoryItem
): BaseScreen(
    title = { item.displayName },
    canGoBack = true
) {
    @Composable
    override fun ScreenContent() {
        Text(item.displayName)
    }
}
