package ui.reusable.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@ExperimentalMaterial3Api
@Composable
fun <Type> FormSelect(
    value: Type?,
    onValueChanged: (Type) -> Unit,
    label: String,
    options: List<Type>,
    modifier: Modifier = Modifier,
    dismissOnSelect: Boolean = true,
    toStringConverter: @Composable (Type) -> String = { it.toString() }
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value?.let { toStringConverter(it) } ?: "",
            onValueChange = {},
            modifier = Modifier.menuAnchor().then(modifier),
            label = { Text(label) },
            readOnly = true,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (option in options) {
                ListItem(
                    headlineContent = { Text(toStringConverter(option)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onValueChanged(option)
                            if (dismissOnSelect) expanded = false
                        }
                )
            }
        }
    }
}
