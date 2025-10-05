package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

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
                            imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = if (isSelected) "Selected" else "Not selected"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
