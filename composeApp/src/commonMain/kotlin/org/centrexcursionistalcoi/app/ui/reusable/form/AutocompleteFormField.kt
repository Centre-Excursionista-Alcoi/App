package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
@ExperimentalMaterial3Api
fun AutocompleteFormField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: Set<String>,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onSuggestionClicked: (String) -> Unit = { onValueChange(it) },
) {
    AutocompleteFormField(
        value = value,
        onValueChange = onValueChange,
        suggestions = suggestions,
        label = label,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        toString = { it },
        onSuggestionClicked = onSuggestionClicked
    )
}

@Composable
@ExperimentalMaterial3Api
fun <T> AutocompleteFormField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: Set<T>,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    toString: (T) -> String = { it.toString() },
    onSuggestionClicked: (T) -> Unit = { onValueChange(toString(it)) },
) {
    val anySuggestion = suggestions.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = anySuggestion && expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            singleLine = true,
            enabled = enabled,
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            readOnly = readOnly,
            trailingIcon = { if (anySuggestion) ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )

        ExposedDropdownMenu(
            expanded = anySuggestion && expanded,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(toString(suggestion)) },
                    onClick = {
                        onSuggestionClicked(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}
