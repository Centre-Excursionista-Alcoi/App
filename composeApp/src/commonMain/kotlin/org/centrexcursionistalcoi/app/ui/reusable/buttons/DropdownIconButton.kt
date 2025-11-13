package org.centrexcursionistalcoi.app.ui.reusable.buttons

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownIconButton(
    imageVector: ImageVector,
    items: List<T>,
    onItemSelected: (T) -> Unit,
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
