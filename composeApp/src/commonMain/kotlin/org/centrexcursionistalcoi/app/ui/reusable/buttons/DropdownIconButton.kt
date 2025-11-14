package org.centrexcursionistalcoi.app.ui.reusable.buttons

import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.centrexcursionistalcoi.app.utils.or

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownIconButton(
    imageVector: ImageVector,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    selection: T? = null,
    toString: @Composable (T) -> String = { it.toString() },
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            expanded = false
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        IconButton(
            enabled = enabled,
            onClick = { expanded = true },
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        ) {
            Icon(imageVector, contentDescription)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            matchAnchorWidth = false,
            onDismissRequest = { expanded = false },
        ) {
            for (item in items) {
                val isSelected = item == selection
                DropdownMenuItem(
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer or Color.Unspecified chooseBy isSelected
                    ),
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.primaryContainer or Color.Unspecified chooseBy isSelected
                    ),
                    text = { Text(toString(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownIconButton(
    imageVector: ImageVector,
    items: Iterable<T>,
    selectedItems: List<T>,
    onItemClicked: (T) -> Unit,
    modifier: Modifier = Modifier,
    toString: @Composable (T) -> String = { it.toString() },
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            expanded = false
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        IconButton(
            enabled = enabled,
            onClick = { expanded = true },
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        ) {
            Icon(imageVector, contentDescription)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            matchAnchorWidth = false,
            onDismissRequest = { expanded = false },
        ) {
            for (item in items) {
                DropdownMenuItem(
                    colors = MenuDefaults.itemColors(
                        textColor = if (selectedItems.contains(item))
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            Color.Unspecified,
                    ),
                    modifier = Modifier.background(
                        color = if (selectedItems.contains(item))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            Color.Unspecified
                    ),
                    text = { Text(toString(item)) },
                    onClick = {
                        onItemClicked(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
