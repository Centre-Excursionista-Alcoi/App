package ui.reusable.list

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import backend.database.InventoryItem
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.fade
import com.eygraber.compose.placeholder.material3.placeholder
import kotlin.random.Random

/**
 * Draws a new inventory item in a card.
 * @param item The item to display, or null if only a placeholder.
 */
@Composable
fun InventoryItemCard(item: InventoryItem?, modifier: Modifier = Modifier) {
    OutlinedCard(modifier) {
        Text(
            text = item?.displayName ?: "X".repeat(Random.Default.nextInt(5, 10)),
            modifier = Modifier
                .placeholder(visible = item == null, highlight = PlaceholderHighlight.fade())
                .padding(8.dp)
        )
        Text(
            text = "#${item?.id ?: 10} - ${item?.let { it.category ?: "null" } ?: 10}",
            modifier = Modifier
                .placeholder(visible = item == null, highlight = PlaceholderHighlight.fade())
                .padding(horizontal = 8.dp).padding(bottom = 8.dp)
        )
    }
}
