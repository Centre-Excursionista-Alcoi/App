package ui.reusable.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import backend.data.database.InventoryItem
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
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .placeholder(visible = item == null, highlight = PlaceholderHighlight.fade()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Category, null)
            }
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp).padding(end = 8.dp)
            ) {
                Text(
                    text = item?.displayName ?: "X".repeat(Random.Default.nextInt(5, 10)),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .placeholder(visible = item == null, highlight = PlaceholderHighlight.fade()),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "#${item?.id ?: 10} - ${item?.let { it.category ?: "null" } ?: 10}",
                    modifier = Modifier
                        .padding(horizontal = 8.dp).padding(bottom = 8.dp)
                        .placeholder(visible = item == null, highlight = PlaceholderHighlight.fade()),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
