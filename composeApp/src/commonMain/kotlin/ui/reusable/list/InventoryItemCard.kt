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
import app.composeapp.generated.resources.Res
import backend.data.database.InventoryItem
import backend.int.IconProvider
import backend.int.imageVector
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.fade
import com.eygraber.compose.placeholder.material3.placeholder
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import ui.dialog.CoroutineDialog
import ui.dialog.TextInputDialog
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
    onIconUpdateRequested: ((newIcon: String?) -> Job)? = null,
    onDisplayNameUpdateRequested: ((displayName: String) -> Job)? = null
) {
    var showingIconsDialog: Boolean by remember { mutableStateOf(false) }
    if (showingIconsDialog) {
        var iconSelection by remember { mutableStateOf(item?.category?.icon) }

        CoroutineDialog(
            title = stringResource(Res.string.lending_icon_selection_dialog_title),
            onDismissRequest = { showingIconsDialog = false },
            onSubmit = onIconUpdateRequested?.let { { onIconUpdateRequested(iconSelection) } }
        ) { isLoading ->
            LazyVerticalGrid(
                columns = GridCells.FixedSize(48.dp)
            ) {
                items(IconProvider.icons.toList()) { (key, icon) ->
                    IconButton(
                        enabled = !isLoading,
                        onClick = { iconSelection = key },
                        colors = if (iconSelection == key) {
                            IconButtonDefaults.filledIconButtonColors()
                        } else {
                            IconButtonDefaults.iconButtonColors()
                        }
                    ) { Icon(icon, key) }
                }
            }
        }
    }
    var editingDisplayName by remember { mutableStateOf(false) }
    if (editingDisplayName) {
        TextInputDialog(
            title = stringResource(Res.string.lending_display_name_edit),
            label = stringResource(Res.string.lending_display_name),
            initialValue = item?.displayName ?: "",
            onValueChange = onDisplayNameUpdateRequested,
            onDismissRequest = { editingDisplayName = false }
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
                        )
                        .clickable(enabled = item != null && isManager) {
                            editingDisplayName = true
                        },
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
