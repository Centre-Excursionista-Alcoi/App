package ui.reusable.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import backend.data.database.Category
import backend.data.database.InventoryItem
import backend.int.IconProvider
import backend.int.imageVector
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.fade
import com.eygraber.compose.placeholder.material3.placeholder
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import ui.dialog.GridCoroutineDialog
import ui.dialog.ListCoroutineDialog
import ui.dialog.TextInputDialog
import kotlin.random.Random

/**
 * The number used for placeholders (will be hidden).
 */
private const val PlaceholderNumber = "10"

/**
 * Draws a new inventory item in a card.
 * @param item The item to display, or null if only a placeholder.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InventoryItemCard(
    item: InventoryItem?,
    categories: List<Category>?,
    isManager: Boolean = false,
    modifier: Modifier = Modifier,
    onIconUpdateRequested: ((newIcon: String?) -> Job)? = null,
    onDisplayNameUpdateRequested: ((displayName: String) -> Job)? = null,
    onCategoryUpdateRequested: ((category: Category?) -> Job)? = null,
    onClick: () -> Unit = {}
) {
    var showingIconsDialog: Boolean by remember { mutableStateOf(false) }
    if (showingIconsDialog) {
        var iconSelection by remember { mutableStateOf(item?.icon ?: item?.category?.icon) }

        GridCoroutineDialog(
            title = stringResource(Res.string.lending_icon_selection_dialog_title),
            onDismissRequest = { showingIconsDialog = false },
            onSubmit = onIconUpdateRequested?.let { { it(iconSelection) } },
            items = IconProvider.icons.toList()
        ) { isLoading, (key, icon) ->
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
    var editingCategory by remember { mutableStateOf(false) }
    if (editingCategory) {
        var categorySelection by remember { mutableStateOf(item?.category) }

        ListCoroutineDialog(
            title = stringResource(Res.string.lending_category_selection_dialog_title),
            onDismissRequest = { editingCategory = false },
            onSubmit = onCategoryUpdateRequested?.let { { it(categorySelection) } },
            items = categories?.let { listOf(null, *it.toTypedArray()) }
        ) { isLoading, category ->
            SelectableListItem(
                selected = category == categorySelection,
                text = category?.displayName ?: stringResource(Res.string.lending_category_none),
                enabled = !isLoading
            ) { categorySelection = category }
        }
    }

    OutlinedCard(
        modifier = Modifier
            .clickable(enabled = item != null, onClick = onClick)
            .then(modifier)
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
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    state = rememberTooltipState(),
                    tooltip = {
                        PlainTooltip {
                            Text(
                                text = if (isManager) {
                                    stringResource(Res.string.lending_category_edit)
                                } else {
                                    stringResource(Res.string.lending_category)
                                }
                            )
                        }
                    }
                ) {
                    Text(
                        text = buildAnnotatedString {
                            if (item != null) {
                                val displayName = item.category?.displayName
                                    ?: stringResource(Res.string.lending_category_none)
                                append(displayName)
                            } else {
                                append(PlaceholderNumber)
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp).padding(bottom = 8.dp)
                            .placeholder(
                                visible = item == null,
                                highlight = PlaceholderHighlight.fade()
                            )
                            .clickable(enabled = item != null && isManager) {
                                editingCategory = true
                            },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
