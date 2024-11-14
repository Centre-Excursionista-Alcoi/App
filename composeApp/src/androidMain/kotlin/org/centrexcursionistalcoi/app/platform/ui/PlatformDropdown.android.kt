package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun <Type : Any> PlatformDropdown(
    value: Type?,
    onValueChange: (Type) -> Unit,
    options: List<Type>,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    toString: (Type?) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = toString(value),
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    text = { Text(toString(option)) }
                )
            }
        }
    }
}