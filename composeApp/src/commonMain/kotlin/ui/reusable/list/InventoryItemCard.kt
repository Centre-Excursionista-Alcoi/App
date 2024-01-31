package ui.reusable.list

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    OutlinedCard(
        modifier = Modifier.width(200.dp).height(150.dp).then(modifier)
    ) {
        Text(
            text = item?.displayName ?: "X".repeat(Random.Default.nextInt(5, 10)),
            modifier = Modifier
                .padding(8.dp)
                .placeholder(visible = item == null, highlight = PlaceholderHighlight.fade())
        )
        Text(
            text = "#${item?.id ?: 10} - ${item?.let { it.category ?: "null" } ?: 10}",
            modifier = Modifier
                .padding(horizontal = 8.dp).padding(bottom = 8.dp)
                .placeholder(visible = item == null, highlight = PlaceholderHighlight.fade())
        )
    }
}
