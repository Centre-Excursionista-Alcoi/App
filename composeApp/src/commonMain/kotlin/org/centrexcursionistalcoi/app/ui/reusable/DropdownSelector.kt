package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Checkbox
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.CheckboxUnchecked
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun <T> DropdownSelector(
    selection: List<T>,
    options: List<T>,
    onSelectionChange: (List<T>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemToString: @Composable (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { expanded = it },
    ) {
        @Suppress("SimplifiableCallChain") // cannot be used because itemToString is Composable
        OutlinedTextField(
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            value = selection.map { itemToString(it) }.joinToString(", "),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                val isSelected = selection.contains(option)
                DropdownMenuItem(
                    enabled = enabled,
                    text = { Text(itemToString(option)) },
                    onClick = {
                        val newSelection = if (isSelected) {
                            selection - option
                        } else {
                            selection + option
                        }
                        onSelectionChange(newSelection)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSelected) MaterialSymbols.Checkbox else MaterialSymbols.CheckboxUnchecked,
                            contentDescription = if (isSelected) "Selected" else "Not selected"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
