package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Remove

@Composable
@ExperimentalMaterial3Api
fun AutocompleteMultipleFormField(
    entries: List<String>,
    onEntryAdded: (String) -> Unit,
    onEntryRemoved: (String) -> Unit,
    suggestions: Set<String>,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
) {
    Column {
        val anySuggestion = suggestions.isNotEmpty()
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = anySuggestion && expanded,
            onExpandedChange = { expanded = it }
        ) {
            var value by remember { mutableStateOf("") }

            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = label,
                singleLine = true,
                enabled = enabled,
                modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                readOnly = readOnly,
                trailingIcon = { if (anySuggestion) ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                keyboardActions = KeyboardActions {
                    if (value.isNotBlank()) {
                        onEntryAdded(value)
                        value = ""
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                )
            )

            ExposedDropdownMenu(
                expanded = anySuggestion && expanded,
                onDismissRequest = { expanded = false }
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onEntryAdded(suggestion)
                            expanded = false
                        }
                    )
                }
            }
        }

        LazyRow {
            items(entries) { entry ->
                AssistChip(
                    onClick = { onEntryRemoved(entry) },
                    label = { Text(entry) },
                    trailingIcon = {
                        Icon(
                            MaterialSymbols.Remove,
                            null,
                        )
                    }
                )
            }
        }
    }
}
