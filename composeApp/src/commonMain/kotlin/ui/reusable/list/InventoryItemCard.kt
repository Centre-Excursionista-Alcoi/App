package ui.reusable.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import backend.data.database.InventoryItem
import backend.int.IconProvider
import backend.int.imageVector
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.fade
import com.eygraber.compose.placeholder.material3.placeholder
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Job
import resources.MR
import kotlin.random.Random

/**
 * Draws a new inventory item in a card.
 * @param item The item to display, or null if only a placeholder.
 */
@Composable
fun InventoryItemCard(
    item: InventoryItem?,
    isManager: Boolean = false,
    modifier: Modifier = Modifier,
    onIconUpdateRequested: ((newIcon: String?) -> Job)? = null
) {
    var updatingIcon by remember { mutableStateOf(false) }
    var showingIconsDialog: Boolean by remember { mutableStateOf(false) }
    if (showingIconsDialog) {
        var iconSelection by remember { mutableStateOf(item?.category?.icon) }
        AlertDialog(
            onDismissRequest = {
                if (!updatingIcon) showingIconsDialog = false
            },
            title = { Text(stringResource(MR.strings.lending_icon_selection_dialog_title)) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.FixedSize(48.dp)
                ) {
                    items(IconProvider.icons.toList()) { (key, icon) ->
                        IconButton(
                            enabled = !updatingIcon,
                            onClick = { iconSelection = key },
                            colors = if (iconSelection == key) {
                                IconButtonDefaults.filledIconButtonColors()
                            } else {
                                IconButtonDefaults.iconButtonColors()
                            }
                        ) { Icon(icon, key) }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !updatingIcon,
                    onClick = {
                        updatingIcon = true
                        onIconUpdateRequested?.invoke(iconSelection)?.invokeOnCompletion {
                            updatingIcon = false
                            showingIconsDialog = false
                        }
                    }
                ) { Text(stringResource(MR.strings.ok)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !updatingIcon,
                    onClick = { showingIconsDialog = false }
                ) { Text(stringResource(MR.strings.cancel)) }
            }
        )
    }

    OutlinedCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .placeholder(visible = item == null, highlight = PlaceholderHighlight.fade())
                    .clickable(enabled = item != null && isManager) {
                        showingIconsDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(item?.imageVector ?: item?.category.imageVector, item?.displayName)
            }
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp).padding(end = 8.dp)
            ) {
                Text(
                    text = item?.displayName ?: "X".repeat(Random.Default.nextInt(5, 10)),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .placeholder(
                            visible = item == null,
                            highlight = PlaceholderHighlight.fade()
                        ),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "#${item?.id ?: 10} - ${item?.let { it.category ?: "null" } ?: 10}",
                    modifier = Modifier
                        .padding(horizontal = 8.dp).padding(bottom = 8.dp)
                        .placeholder(
                            visible = item == null,
                            highlight = PlaceholderHighlight.fade()
                        ),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
